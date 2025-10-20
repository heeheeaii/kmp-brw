package com.treevalue.beself.web

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.treevalue.beself.bus.DownloadEvent
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.js.getVideoRemovalScript
import com.treevalue.beself.net.FileUrlDetector
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import com.treevalue.beself.util.dw
import com.treevalue.beself.values.VIDEO_BLOCK_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

actual typealias NativeWebView = WebView

class AndroidWebView(
    override val webView: WebView,
    override val scope: CoroutineScope,
    val webViewClient: SimpleWebViewClient,
    val webviewChromeClient: SimpleChromeClient,
    private val serviceProvider: ServiceProvider? = null,
) : IWebView, WebViewStateProvider {
    private var downloadListener: ((String, String) -> Unit)? = null
    private var fileChooserListener: (suspend () -> List<String>?)? = null


    init {
        setupWebViewClient()
        setupEnhancedSettings()
    }

    override fun isLoading(): Boolean {
        return try {
            // 通过多个指标判断是否正在加载
            val progress = webView.progress
            val url = webView.url
            progress < 100 && !url.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override fun getCurrentUrl(): String? {
        return try {
            webView.url
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentTitle(): String? {
        return try {
            webView.title
        } catch (e: Exception) {
            null
        }
    }

    override fun getProgress(): Int {
        return try {
            webView.progress
        } catch (e: Exception) {
            100
        }
    }

    override fun stopVideoRemoval() {
        webView.evaluateJavascript(
            """
            // 移除CSS样式
            const style = document.getElementById('${VIDEO_BLOCK_ID}');
            if (style) {
                style.remove();
            }
            // 停止所有定时器和观察器
            window.videoRemovalActive = false;
            
            """.trimIndent()
        ) {
            KLogger.dd { "视频移除功能已停止" }
        }
    }

    private fun setupEnhancedSettings() {
        webView.settings.apply {
            // 启用所有必要的功能
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)

            // 媒体播放设置
            mediaPlaybackRequiresUserGesture = false

            // 存储和缓存
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT

            // 文件访问
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            allowContentAccess = true

            // 网络和安全
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // 图片和资源加载
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false

            // 地理位置
            setGeolocationEnabled(true)

            // 字体和显示
            defaultTextEncodingName = "utf-8"

            // 其他设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }

        // Cookie设置
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun setupWebViewClient() {
        webView.webViewClient = this@AndroidWebView.webViewClient
        webView.webChromeClient = this@AndroidWebView.webviewChromeClient

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            KLogger.dd { "Download requested: $url -> $filename" }

            try {
                if (FileUrlDetector.isBlockedFileType(url)) {
                    KLogger.dw { "Blocked download attempt for prohibited file type: $filename" }
                    Toast.makeText(webView.context, "不支持下载此类型文件", Toast.LENGTH_SHORT).show()
                    return@setDownloadListener
                }
                // 区分两种场景（与桌面版一致）
                if (FileUrlDetector.isDownloadableUrl(url)) {
                    // 场景1：URL本身是下载文件 - 提供选择下载
                    EventBus.publish(DownloadEvent.DownloadAvailable(url, filename))
                } else {
                    // 场景2：网页内下载 - 自动开始
                    EventBus.publish(DownloadEvent.AutoStartDownload(url, filename))
                }

                startDownload(webView.context, url, filename, mimetype)
                downloadListener?.invoke(url, filename)
            } catch (e: Exception) {
                KLogger.de { "Download failed: ${e.message}" }
                Toast.makeText(webView.context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDownload(context: Context, url: String, filename: String, mimeType: String) {
        if (FileUrlDetector.isBlockedFileType(url)) {
            return
        }
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("正在下载...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)

            // 添加用户代理
            addRequestHeader("User-Agent", webView.settings.userAgentString)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "开始下载: $filename", Toast.LENGTH_SHORT).show()
    }

    override suspend fun requestPermission(permission: WebViewPermission): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val context = webView.context
            val androidPermission = when (permission) {
                WebViewPermission.CAMERA -> Manifest.permission.CAMERA
                WebViewPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
                WebViewPermission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
                else -> {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }
            }

            val hasPermission =
                ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
            continuation.resume(hasPermission)
        }
    }

    override fun grantPermission(permission: WebViewPermission) {
        KLogger.dd { "Permission granted: $permission" }
    }

    override fun denyPermission(permission: WebViewPermission) {
        KLogger.dd { "Permission denied: $permission" }
    }

    override fun pauseAllMedia() {
        webView.evaluateJavascript(
            """
            document.querySelectorAll('audio, video').forEach(function(media) {
                if (!media.paused) {
                    media.pause();
                    media.setAttribute('data-was-playing', 'true');
                }
            });
            """.trimIndent()
        ) { }
    }

    override fun resumeAllMedia() {
        webView.evaluateJavascript(
            """
            document.querySelectorAll('audio, video').forEach(function(media) {
                if (media.getAttribute('data-was-playing') === 'true') {
                    media.play().catch(function(error) {
                        console.log('Resume play failed:', error);
                    });
                    media.removeAttribute('data-was-playing');
                }
            });
            """.trimIndent()
        ) { }
    }

    override fun setMediaVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        webView.evaluateJavascript(
            """
            document.querySelectorAll('audio, video').forEach(function(media) {
                media.volume = $clampedVolume;
            });
            """.trimIndent()
        ) { }
    }

    override fun setDownloadListener(listener: (url: String, filename: String) -> Unit) {
        this.downloadListener = listener
    }

    override fun setFileChooserListener(listener: suspend () -> List<String>?) {
        this.fileChooserListener = listener
    }

    override fun setUserAgent(userAgent: String) {
        webView.settings.userAgentString = userAgent
    }

    override fun getUserAgent(): String {
        return webView.settings.userAgentString
    }

    override fun clearCache() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
    }

    override fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    override fun clearLocalStorage() {
        webView.evaluateJavascript(
            """
            try {
                localStorage.clear();
                sessionStorage.clear();
                if (window.indexedDB) {
                    indexedDB.databases().then(databases => {
                        databases.forEach(db => indexedDB.deleteDatabase(db.name));
                    });
                }
            } catch(e) {
                console.log('Clear storage error:', e);
            }
            """.trimIndent()
        ) { }

        // 清除WebView存储
        WebStorage.getInstance().deleteAllData()
    }

    override fun getPageSource(callback: (String) -> Unit) {
        webView.evaluateJavascript(
            "(function(){return document.documentElement.outerHTML})();"
        ) { html ->
            val cleanHtml = html?.removeSurrounding("\"")?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: ""
            callback(cleanHtml)
        }
    }

    override fun takeScreenshot(callback: (ByteArray) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(
                webView.width, webView.height, Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            webView.draw(canvas)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            bitmap.recycle()

            callback(byteArray)
        } catch (e: Exception) {
            KLogger.de { "Screenshot failed: ${e.message}" }
            callback(ByteArray(0))
        }
    }

    override fun canGoBack() = webView.canGoBack()

    override fun canGoForward() = webView.canGoForward()

    override fun loadUrl(
        url: String,
        additionalHttpHeaders: Map<String, String>,
    ) {
        webView.loadUrl(url, additionalHttpHeaders)
    }

    override fun loadHtml(
        html: String?,
        baseUrl: String?,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {
        if (html == null) return
        webView.loadDataWithBaseURL(baseUrl, html, mimeType, encoding, historyUrl)
        webView.post {
            if (serviceProvider?.isStillVideoEnable() != true) {
                webView.evaluateJavascript("setTimeout(function() { ${getVideoRemovalScript()} }, 1000);") { }
            } else {
                stopVideoRemoval()
            }
        }
    }

    override suspend fun loadHtmlFile(fileName: String) {
        KLogger.dd { "Loading HTML file: $fileName" }
        webView.loadUrl("file:///android_asset/$fileName")
    }

    override fun postUrl(
        url: String,
        postData: ByteArray,
    ) {
        KLogger.dd { "Posting to URL: $url" }
        webView.postUrl(url, postData)
    }

    override fun goBack() {
        webView.goBack()
    }

    override fun goForward() {
        webView.goForward()
    }

    override fun reload() {
        val currentUrl = webView.url
        if (!currentUrl.isNullOrEmpty()) {
            val originalCacheMode = webView.settings.cacheMode
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.loadUrl(currentUrl)
            webView.post {
                webView.settings.cacheMode = originalCacheMode
            }
        } else {
            webView.reload()
        }
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun evaluateJavaScript(
        script: String,
        callback: ((String) -> Unit)?,
    ) {
        webView.post {
            webView.evaluateJavascript(script, callback)
        }
    }

    override fun scrollOffset(): Pair<Int, Int> {
        return Pair(webView.scrollX, webView.scrollY)
    }

    override fun saveState(): WebViewBundle? {
        val bundle = WebViewBundle()
        return if (webView.saveState(bundle) != null) {
            bundle
        } else {
            null
        }
    }
}

open class PopupSupportWebChromeClient(
    private val onShowFilePicker: (Intent) -> Unit,
    private val onCreateNewTab: (String) -> Unit,
) : SimpleChromeClient() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?,
    ): Boolean {


        val newWebView = WebView(view.context)

        // 确保新 WebView 支持多窗口
        newWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }

        newWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.isNotEmpty() && url != "about:blank") {
                    onCreateNewTab(url)
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                if (url.isNotEmpty() && url != "about:blank") {
                    onCreateNewTab(url)
                }
            }
        }

        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()


        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        this.filePathCallback = filePathCallback
        val filePickerIntent = fileChooserParams?.createIntent()

        if (filePickerIntent == null) {
            cancelFileChooser()
        } else {
            onShowFilePicker(filePickerIntent)
        }
        return true
    }

    fun onReceiveFiles(uris: Array<Uri>) {
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    fun cancelFileChooser() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
    }
}
