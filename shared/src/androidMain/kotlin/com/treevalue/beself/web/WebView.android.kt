package com.treevalue.beself.web

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.treevalue.beself.js.getForceDarkModeScript
import com.treevalue.beself.js.getVideoRemovalScript
import com.treevalue.beself.net.FileUrlDetector
import com.treevalue.beself.values.VIDEO_BLOCK_ID
import com.treevalue.beself.bus.DownloadEvent
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.js.getNewTabInterceptionScript
import com.treevalue.beself.js.getBilibiliFixScript
import com.treevalue.beself.net.*
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import com.treevalue.beself.util.dw

actual typealias WebViewBundle = android.os.Bundle

internal object FileHandlerUtil {
    fun handleFileUrl(view: WebView?, url: String): Boolean {
        if (view == null) return false

        val context = view.context
        val extension = getFileExtension(url)

        KLogger.dd { "处理文件URL: $url, 扩展名: $extension" }

        return when {
            displayableFileTypes.contains(extension) -> {
                KLogger.dd { "直接在WebView中显示文件: $url" }
                false // 让WebView正常加载
            }

            previewableFileTypes.contains(extension) -> {
                showFilePreviewDialog(context, url, extension)
                true
            }

            else -> {
                showDownloadDialog(context, url)
                true
            }
        }
    }

    private fun showFilePreviewDialog(context: Context, fileUrl: String, extension: String) {
        try {
            AlertDialog.Builder(context).apply {
                setTitle("文件预览")
                setMessage("检测到 ${extension.uppercase()} 文件，请选择操作：")
                setPositiveButton("预览") { _, _ ->
                    getPreviewUrl(fileUrl, extension)?.let { previewUrl ->
                        (context as? Activity)?.runOnUiThread {
                            // 需要通过回调获取WebView实例
                        }
                    } ?: downloadFile(context, fileUrl)
                }
                setNeutralButton(Pages.GrabSitePage.Download.getLang()) { _, _ -> downloadFile(context, fileUrl) }
                setNegativeButton(Pages.AddSitePage.Cancel.getLang()) { dialog, _ -> dialog.dismiss() }
                show()
            }
        } catch (e: Exception) {
            KLogger.de { "显示预览对话框失败: ${e.message}" }
            downloadFile(context, fileUrl)
        }
    }

    private fun showDownloadDialog(context: Context, fileUrl: String) {
        try {
            val fileName = Uri.parse(fileUrl).lastPathSegment ?: "unknown_file"
            AlertDialog.Builder(context).apply {
                setTitle("下载文件")
                setMessage("是否下载文件：$fileName ?")
                setPositiveButton(Pages.GrabSitePage.Download.getLang()) { _, _ -> downloadFile(context, fileUrl) }
                setNegativeButton(Pages.AddSitePage.Cancel.getLang()) { dialog, _ -> dialog.dismiss() }
                show()
            }
        } catch (e: Exception) {
            KLogger.de { "显示下载对话框失败: ${e.message}" }
            downloadFile(context, fileUrl)
        }
    }

    private fun getPreviewUrl(fileUrl: String, extension: String): String? {
        return when (extension) {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> "https://docs.google.com/viewer?url=${
                Uri.encode(
                    fileUrl
                )
            }&embedded=true"

            "txt", "csv" -> fileUrl
            else -> null
        }
    }

    private fun downloadFile(context: Context, url: String) {
        try {
            val fileName = URLUtil.guessFileName(url, null, null)
            startDownload(context, url, fileName, "*/*")

            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            KLogger.de { "下载文件失败: ${e.message}" }
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

internal object UrlValidator {
    fun shouldAllowUrl(url: String, isMainFrame: Boolean, serviceProvider: ServiceProvider?): Boolean {
        serviceProvider?.let {
            return it.isUrlAllowed(url)
        }
        return true
    }

    fun isAllowedRedirect(fromUrl: String, toUrl: String): Boolean {
        return try {
            val fromHost = getHostnameFromUrl(fromUrl)
            val toHost = getHostnameFromUrl(toUrl)
            getRootDomain(fromHost) == getRootDomain(toHost)
        } catch (e: Exception) {
            false
        }
    }

    private fun getRootDomain(hostname: String): String {
        val parts = hostname.split(".")
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            hostname
        }
    }
}

internal object NewTabHandler {
    fun handleNewTabRequest(url: String) {
        try {
            if (url.startsWith("newtab://")) {
                val withoutScheme = url.substring(9)
                val questionIndex = withoutScheme.indexOf('?')

                val encodedUrl = if (questionIndex != -1) {
                    withoutScheme.substring(0, questionIndex)
                } else {
                    withoutScheme
                }

                val targetUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
                var title = "新标签页"

                if (questionIndex != -1) {
                    val queryString = withoutScheme.substring(questionIndex + 1)
                    queryString.split("&").forEach { param ->
                        val keyValue = param.split("=", limit = 2)
                        if (keyValue.size == 2 && keyValue[0] == "title") {
                            title = java.net.URLDecoder.decode(keyValue[1], "UTF-8")
                        }
                    }
                }

                EventBus.publish(TabEvent.RequestNewTab(targetUrl, title))
                KLogger.dd { "发布新标签页事件: $targetUrl, 标题: $title" }
            }
        } catch (e: Exception) {
            KLogger.de { "解析新标签页请求失败: $url, 错误: ${e.message}" }
        }
    }
}

abstract class BaseWebViewClient(
    protected val serviceProvider: ServiceProvider? = null,
) : WebViewClient() {

    abstract var state: WebViewState
    abstract var navigator: WebViewController
    protected var lastUrl = ""

    protected fun handleCommonPageStarted(view: WebView, url: String?, favicon: Bitmap?) {

    }

    protected fun handleCommonUrlWithSysDownload(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request == null) return false

        val url = request.url.toString()
        // 处理文件URL
        if (isFileUrl(url)) {
            if (FileUrlDetector.isBlockedFileType(url)) {
                KLogger.dw { "阻止访问被禁止的文件URL: $url" }
                return true
            }
            return FileHandlerUtil.handleFileUrl(view, url)
        }

        // 处理特殊协议
        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view?.context?.startActivity(intent)
                return true
            } catch (e: Exception) {
                KLogger.de { "Failed to handle special URL: $url" }
            }
        }

        // 处理应用跳转
        if (url.startsWith("bilibili://")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view?.context?.startActivity(intent)
                return true
            } catch (e: Exception) {
                val webUrl = url.replace("bilibili://", "https://m.bilibili.com/")
                view?.loadUrl(webUrl)
                return true
            }
        }

        // 新标签页处理
        if (url.startsWith("newtab://")) {
            NewTabHandler.handleNewTabRequest(url)
            return true
        }

        // 避免无限重定向
        if (url == lastUrl) {
            return false
        }

        // URL验证
        if (!UrlValidator.shouldAllowUrl(url, request.isForMainFrame, serviceProvider)) {
            val currentUrl = view?.url
            if (currentUrl != null && UrlValidator.isAllowedRedirect(currentUrl, url)) {
                return false // 允许跳转
            }
            return true // 拦截
        }

        // 请求拦截器处理
        navigator.requestInterceptor?.let { interceptor ->
            val isRedirectRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.isRedirect
            } else {
                false
            }

            val webRequest = WebRequest(
                url,
                request.requestHeaders?.toMutableMap() ?: mutableMapOf(),
                request.isForMainFrame,
                isRedirectRequest,
                request.method ?: "GET"
            )

            return when (val result = interceptor.onInterceptUrlRequest(webRequest, navigator)) {
                is WebRequestInterceptResult.Allow -> false
                is WebRequestInterceptResult.Reject -> true
                is WebRequestInterceptResult.Modify -> {
                    navigator.stopLoading()
                    navigator.loadUrl(result.request.url, result.request.headers)
                    true
                }
            }
        }

        return false
    }

    protected fun handleCommonError(view: WebView, request: WebResourceRequest?, error: WebResourceError?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val errorCode = error?.errorCode ?: -1
        val description = error?.description?.toString() ?: "Unknown error"
        val url = request?.url?.toString() ?: "Unknown URL"

        if (request?.isForMainFrame == true) {
            KLogger.de { "Main frame error: $description for $url" }
            state.errorsForCurrentRequest.add(
                WebViewError(errorCode, description, true)
            )
        } else {
            KLogger.dw { "Resource error: $description for $url" }
        }
    }

    protected fun handleCommonHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        val statusCode = errorResponse?.statusCode ?: -1
        val url = request?.url?.toString() ?: "Unknown URL"

        if (request?.isForMainFrame == true) {
            KLogger.de { "HTTP Error: $statusCode for main frame: $url" }
        } else {
            KLogger.dw { "HTTP Error: $statusCode for resource: $url" }
        }
    }

    protected fun handleCommonSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        KLogger.dw { "SSL Error: ${error?.toString()}" }

        val url = error?.url
        if (url?.contains("bilibili.com") == true || url?.contains("hdslb.com") == true) {
            handler?.proceed()
        } else {
            handler?.cancel()
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        navigator.canGoBack = view.canGoBack()
        navigator.canGoForward = view.canGoForward()
    }
}

abstract class BaseWebChromeClient : WebChromeClient() {
    abstract var state: WebViewState
    abstract var context: Context

    protected fun handleCommonCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
        onCreateNewTab: ((String) -> Unit)?,
    ): Boolean {
        KLogger.dd { "onCreateWindow called - isDialog:$isDialog, isUserGesture:$isUserGesture" }

        if (!isUserGesture) {
            KLogger.dd { "Blocked non-user-gesture window creation" }
            return false
        }

        val newWebView = WebView(view.context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportMultipleWindows(true)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    onCreateNewTab?.invoke(url)
                    return true
                }
            }
        }

        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()

        return true
    }

    protected fun handleCommonProgressChanged(view: WebView, newProgress: Int) {
        state.loadingState = if (newProgress == 100) {
            LoadingState.Finished
        } else {
            LoadingState.Loading(newProgress / 100.0f)
        }
    }

    protected fun handleCommonReceivedTitle(view: WebView, title: String?) {
        state.pageTitle = title
    }

    protected fun handleCommonPermissionRequest(request: PermissionRequest) {
        val grantedPermissions = mutableListOf<String>()

        request.resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    grantedPermissions.add(resource)
                }

                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        grantedPermissions.add(resource)
                    }
                }

                PermissionRequest.RESOURCE_MIDI_SYSEX,
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
                    -> {
                    grantedPermissions.add(resource)
                }
            }
        }

        if (grantedPermissions.isNotEmpty()) {
            request.grant(grantedPermissions.toTypedArray())
        } else {
            request.deny()
        }
    }

    protected fun handleCommonConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            val level = when (it.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                ConsoleMessage.MessageLevel.WARNING -> "WARNING"
                ConsoleMessage.MessageLevel.LOG -> "LOG"
                ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                ConsoleMessage.MessageLevel.TIP -> "TIP"
                else -> "UNKNOWN"
            }

            val message = it.message()
            if (!message.contains("reportClientInfo")) {
                KLogger.dd { "Console [$level]: $message" }
            }
        }
        return true
    }
}

open class SimpleWebViewClient(
    private val service: ServiceProvider? = null,
) : BaseWebViewClient(service) {

    override lateinit var state: WebViewState
    override lateinit var navigator: WebViewController

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        KLogger.dd { "Page started: $url" }
        super.onPageStarted(view, url, favicon)
        state.loadingState = LoadingState.Loading(0.0f)
        state.errorsForCurrentRequest.clear()
        state.pageTitle = null
        state.lastLoadedUrl = url
        lastUrl = url ?: ""

        url?.let { currentUrl ->
            if (FileUrlDetector.isDownloadableUrl(currentUrl)) {
                val filename = FileUrlDetector.extractFilename(currentUrl)
                EventBus.publish(DownloadEvent.DownloadAvailable(currentUrl, filename))
            }
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        state.loadingState = LoadingState.Finished
        state.lastLoadedUrl = url

        url?.takeIf { it.contains("bilibili", true) }?.let {
            view.evaluateJavascript(getBilibiliFixScript()) {
                KLogger.dd { "bilibili video fix inject finish" }
            }
        }

        view.evaluateJavascript(getNewTabInterceptionScript()) { result ->
            KLogger.dd { "新标签页拦截脚本注入完成" }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return handleCommonUrlWithSysDownload(view, request)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        handleCommonError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        handleCommonHttpError(view, request, errorResponse)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handleCommonSslError(view, handler, error)
    }
}

open class SimpleChromeClient : BaseWebChromeClient() {
    override lateinit var state: WebViewState
    override lateinit var context: Context

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        handleCommonReceivedTitle(view, title)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        handleCommonProgressChanged(view, newProgress)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        handleCommonPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, true, false)
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        KLogger.dd { "JS Alert: $message" }
        result?.confirm()
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        KLogger.dd { "JS Confirm: $message" }
        result?.confirm()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return handleCommonConsoleMessage(consoleMessage)
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return if (state.webSettings.androidWebSettings.hideDefaultVideoPoster) {
            Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        } else {
            super.getDefaultVideoPoster()
        }
    }
}

class OptimizedWebViewClient(
    private val service: ServiceProvider? = null,
) : SimpleWebViewClient(service) {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (serviceProvider?.isStillVideoEnable() != true) {
            view.evaluateJavascript(
                """
                document.addEventListener('DOMContentLoaded', function() {
                    const style = document.createElement('style');
                    style.id = '${VIDEO_BLOCK_ID}';
                    style.textContent = 'video { display: none !important; visibility: hidden !important; }';
                    document.head.appendChild(style);
                });
                """.trimIndent()
            ) { }
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)

        // 注入视频移除脚本（与桌面版完全一致）
        if (serviceProvider?.isStillVideoEnable() != true) {
            view.evaluateJavascript("setTimeout(function() { ${getVideoRemovalScript()} }, 500);") {}
        } else {
            serviceProvider?.setVideoLimiterSpeed(true)
            view.evaluateJavascript(
                """
                    (function(){
                    var s = document.getElementById('${VIDEO_BLOCK_ID}');
                    if (s) s.remove();
                    if (window.videoRemovalActive) { window.videoRemovalActive = false; }
                    })();
                    """.trimIndent()
            ) { }

        }

        applyDarkModeIfNeeded(view)
    }

    private fun applyDarkModeIfNeeded(view: WebView?) {
        val shouldApplyDarkMode = serviceProvider?.isDarkMode() ?: false

        if (shouldApplyDarkMode) {
            view?.evaluateJavascript(
                """
        setTimeout(function() {
            ${getForceDarkModeScript(true)}
        }, 400);
    """.trimIndent()
            ) { }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // 处理下载URL
        val url = request?.url?.toString() ?: return false
        if (serviceProvider?.isUrlAllowed(url) != true) return true
        val isSysDownload = handleCommonUrlWithSysDownload(view, request)
        if (isSysDownload) {
            return true
        }
        if (FileUrlDetector.isDownloadableUrl(url)) {
            val filename = FileUrlDetector.extractFilename(url)
            KLogger.dd { "检测到下载文件URL: $url, filename: $filename" }

            // 发布下载事件（与桌面版一致）
            EventBus.publish(DownloadEvent.DownloadAvailable(url, filename))
            return true
        }
        return false
    }

    fun setupDownloadListener(webView: WebView, downloadListener: ((String, String) -> Unit)?) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            KLogger.dd { "Download requested: $url -> $filename" }

            try {
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
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("正在下载...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        KLogger.dd { "Download started: $filename" }
    }
}

class OptimizedChromeClient(
    private val onCreateNewTab: ((String) -> Unit)? = null,
    private val onShowFilePicker: ((Intent) -> Unit)? = null,
) : SimpleChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {
        return handleCommonCreateWindow(view, isDialog, isUserGesture, resultMsg, onCreateNewTab)
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
            onShowFilePicker?.invoke(filePickerIntent) ?: cancelFileChooser()
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

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        // 自动授权地理位置（实际应用中应该询问用户）
        callback?.invoke(origin, true, false)
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        KLogger.dd { "JS Alert: $message" }
        result?.confirm()
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        KLogger.dd { "JS Confirm: $message" }
        result?.confirm()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            KLogger.dd { "Console [${it.messageLevel()}]: ${it.message()}" }
        }
        return super.onConsoleMessage(consoleMessage)
    }
}

// ===== 保持原有的Composable和其他API =====
@Composable
actual fun setupPlatformWebView(
    state: WebViewState,
    navigator: WebViewController,
    modifier: Modifier,
    captureBackPresses: Boolean,
    onCreated: (NativeWebView) -> Unit,
    onDispose: (NativeWebView) -> Unit,
    platformWebViewParams: PlatformWebViewParams?,
    factory: (WebViewBuildParam) -> NativeWebView,
    serviceProvider: ServiceProvider?,
) {
    BoxWithConstraints(modifier) {
        val width = if (constraints.hasFixedWidth) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val height = if (constraints.hasFixedHeight) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        val layoutParams = FrameLayout.LayoutParams(width, height)

        androidWebViewWrapper(
            state = state,
            layoutParams = layoutParams,
            modifier = Modifier,
            captureBackPresses = captureBackPresses,
            navigator = navigator,
            onCreated = onCreated,
            onDispose = onDispose,
            webviewClient = platformWebViewParams?.webviewClient ?: remember {
                OptimizedWebViewClient(serviceProvider)
            },
            chromeClient = platformWebViewParams?.chromeClient ?: remember {
                OptimizedChromeClient(
                    onCreateNewTab = platformWebViewParams?.onCreateNewTab ?: { url ->
                        KLogger.dd { "Default new tab handler: $url" }
                        navigator.loadUrl(url)
                    }, onShowFilePicker = platformWebViewParams?.onShowFilePicker
                )
            },
            factory = { factory(WebViewBuildParam(it)) },
            serviceProvider = serviceProvider
        )
    }
}

@Composable
fun androidWebViewWrapper(
    state: WebViewState,
    layoutParams: FrameLayout.LayoutParams,
    navigator: WebViewController,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
    webviewClient: SimpleWebViewClient = remember { OptimizedWebViewClient() },
    chromeClient: SimpleChromeClient = remember { OptimizedChromeClient() },
    factory: ((Context) -> WebView)? = null,
    serviceProvider: ServiceProvider? = null,
) {
    val webView = state.webView
    val scope = rememberCoroutineScope()

    BackHandler(captureBackPresses && navigator.canGoBack) {
        webView?.goBack()
    }

    webviewClient.state = state
    webviewClient.navigator = navigator
    chromeClient.state = state

    AndroidView(
        factory = { context ->
            (factory?.invoke(context) ?: WebView(context)).apply {
                onCreated(this)
                this.layoutParams = layoutParams

                state.viewState?.let {
                    this.restoreState(it)
                }

                chromeClient.context = context
                webChromeClient = chromeClient
                webViewClient = webviewClient

                this.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)

                    userAgentString =
                        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    allowContentAccess = true

                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = false
                    loadWithOverviewMode = false

                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mediaPlaybackRequiresUserGesture = false

                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    blockNetworkLoads = false

                    defaultTextEncodingName = "utf-8"
                    defaultFontSize = 16
                    minimumFontSize = 8
                    minimumLogicalFontSize = 8
                    textZoom = 100

                    setGeolocationEnabled(true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = true
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        isAlgorithmicDarkeningAllowed = true
                    }

                    saveFormData = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        offscreenPreRaster = false
                    }

                    setBackgroundColor(state.webSettings.backgroundColor.toArgb())
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                cookieManager.flush()

                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        WebSettingsCompat.setForceDark(this.settings, WebSettingsCompat.FORCE_DARK_ON)
                    } else {
                        WebSettingsCompat.setForceDark(this.settings, WebSettingsCompat.FORCE_DARK_OFF)
                    }

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                        WebSettingsCompat.setForceDarkStrategy(
                            this.settings,
                            WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY,
                        )
                    }
                }

                setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                    KLogger.dd { "Download started: $url" }
                    try {
                        val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                        EventBus.publish(DownloadEvent.DownloadAvailable(url, filename))
                    } catch (e: Exception) {
                        KLogger.de { "Download failed: ${e.message}" }
                    }
                }
            }.also {
                val androidWebView = AndroidWebView(
                    webView = it,
                    scope = scope,
                    webViewClient = webviewClient,
                    webviewChromeClient = chromeClient,
                    serviceProvider = serviceProvider,
                )
                state.webView = androidWebView
            }
        },
        modifier = modifier,
        onReset = {},
        onRelease = {
            onDispose(it)
        },
    )
}

// 下载处理函数
private fun startDownload(context: Context, url: String, filename: String, mimeType: String) {
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("正在下载...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            setMimeType(mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        downloadManager.enqueue(request)
        KLogger.dd { "Download started: $filename" }
    } catch (e: Exception) {
        KLogger.de { "Download failed: ${e.message}" }
    }
}

// 数据类定义
actual data class WebViewBuildParam(val context: Context)

actual fun buildDefaultWebview(param: WebViewBuildParam) = WebView(param.context)

@Immutable
actual data class PlatformWebViewParams(
    val webviewClient: SimpleWebViewClient? = null,
    val chromeClient: SimpleChromeClient? = null,
    val onCreateNewTab: ((String) -> Unit)? = null,
    val onShowFilePicker: ((Intent) -> Unit)? = null,
)
