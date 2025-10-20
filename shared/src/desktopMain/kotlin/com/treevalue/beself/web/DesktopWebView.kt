package com.treevalue.beself.web

import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.EventId
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.setting.WebSettings
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import dev.datlag.kcef.KCEFBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import org.cef.network.CefPostData
import org.cef.network.CefPostDataElement
import org.cef.network.CefRequest
import kotlin.coroutines.resume

actual typealias NativeWebView = KCEFBrowser

object DesktopWebViewManager {
    // 跟踪 tabId 和 browser 实例的映射关系
    private val tabBrowserMap = mutableMapOf<String, KCEFBrowser>()

    init {
        EventBus.registerHandler<TabEvent.TabClosed>(EventId.TabClosed) { event ->
            disposeBrowserForTab(event.tabId)
        }
    }

    fun getBrowser(tabId: String): KCEFBrowser? {
        return tabBrowserMap[tabId]
    }

    fun registerBrowser(tabId: String, browser: KCEFBrowser) {
        if (tabBrowserMap.containsKey(tabId)) {
            return
        }
        tabBrowserMap[tabId] = browser
        
    }

    private fun disposeBrowserForTab(tabId: String) {
        tabBrowserMap[tabId]?.let { browser ->
            try {
                
                // 确保在主线程中执行浏览器清理
                browser.close(true)
                tabBrowserMap.remove(tabId)
                
            } catch (e: Exception) {
                
            }
        }
    }

    fun cleanup() {
        // 清理所有残留的浏览器实例
        tabBrowserMap.values.forEach { browser ->
            try {
                browser.close(true)
            } catch (e: Exception) {
                
            }
        }
        tabBrowserMap.clear()
    }
}

class DesktopWebView(
    override val webView: KCEFBrowser,
    override val scope: CoroutineScope,
    private val webSettings: WebSettings? = null,
    serviceProvider: ServiceProvider? = null,
) : IWebView, WebViewStateProvider {
    private var downloadListener: ((String, String) -> Unit)? = null
    private var fileChooserListener: (suspend () -> List<String>?)? = null
    private var userAgent: String = webSettings?.userAgent ?: ""

    // 用于跟踪加载状态的变量
    private var isCurrentlyLoading: Boolean = false
    private var currentProgress: Int = 100

    // 实现WebViewStateProvider接口
    override fun getCurrentUrl(): String? {
        return try {
            webView.url
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentTitle(): String? {
        return try {
            var title: String? = null
            webView.evaluateJavaScript("document.title") { result ->
                title = result?.takeIf { it != "null" && it.isNotEmpty() }
            }
            title
        } catch (e: Exception) {
            null
        }
    }


    override fun getProgress(): Int {
        return try {
            // CEF没有直接的进度概念，我们通过加载状态来模拟
            if (isCurrentlyLoading) {
                currentProgress
            } else {
                100
            }
        } catch (e: Exception) {
            100
        }
    }

    override fun isLoading(): Boolean {
        return isCurrentlyLoading
    }

    init {
        setupNewTabHandling()
    }

    private fun setupNewTabHandling() {
        // 注入新标签页拦截脚本
        evaluateJavaScript(getNewTabInterceptionScript())
    }

    /**
     * 桌面特定生成JavaScript拦截脚本
     */
    private fun getNewTabInterceptionScript(): String {
        return """
            (function() {
                if (window.newTabInterceptionInjected) {
                    console.log('桌面端新标签页拦截脚本已存在，跳过注入');
                    return;
                }
                
                console.log('桌面端开始注入新标签页拦截脚本...');
                
                // 用于与桌面端通信的函数
                function notifyNewTab(url, title) {
                    // 使用cefQuery与桌面端通信
                    if (window.cefQuery) {
                        window.cefQuery({
                            request: JSON.stringify({
                                id: Date.now(),
                                method: 'newTab',
                                params: JSON.stringify({
                                    url: url,
                                    title: title || '新标签页'
                                })
                            }),
                            onSuccess: function(response) {
                                console.log('新标签页请求已发送:', url);
                            },
                            onFailure: function(error_code, error_message) {
                                console.error('新标签页请求失败:', error_message);
                            }
                        });
                    } else {
                        console.warn('cefQuery不可用，无法发送新标签页请求');
                    }
                }
                
                // 1. 拦截window.open()
                var originalOpen = window.open;
                window.open = function(url, name, specs) {
                    console.log('桌面端拦截到window.open调用:', url, name, specs);
                    
                    if (url) {
                        notifyNewTab(url, name || '新标签页');
                        return null; // 阻止默认行为
                    }
                    
                    return originalOpen.call(this, url, name, specs);
                };
                
                // 2. 拦截target="_blank"链接
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    
                    // 查找最近的<a>标签
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    
                    if (target && target.tagName === 'A') {
                        var href = target.href;
                        var targetAttr = target.getAttribute('target');
                        
                        if (targetAttr === '_blank' && href) {
                            e.preventDefault();
                            e.stopPropagation();
                            
                            var linkText = target.textContent || target.title || target.alt || '新标签页';
                            notifyNewTab(href, linkText);
                            
                            console.log('桌面端已拦截target="_blank"链接:', href);
                            return false;
                        }
                    }
                });
                
                // 3. 拦截表单的target="_blank"
                document.addEventListener('submit', function(e) {
                    var form = e.target;
                    if (form.tagName === 'FORM' && form.getAttribute('target') === '_blank') {
                        e.preventDefault();
                        
                        var formData = new FormData(form);
                        var url = form.action || window.location.href;
                        
                        if (form.method.toLowerCase() === 'get') {
                            var params = new URLSearchParams(formData);
                            url += (url.includes('?') ? '&' : '?') + params.toString();
                        }
                        
                        console.log('桌面端拦截表单提交到新窗口:', url);
                        notifyNewTab(url, '表单结果');
                        return false;
                    }
                });
                
                // 标记已注入
                window.newTabInterceptionInjected = true;
                console.log('桌面端新标签页拦截脚本注入完成');
            })();
        """.trimIndent()
    }

    override suspend fun requestPermission(permission: WebViewPermission): Boolean {
        return suspendCancellableCoroutine { continuation ->
            when (permission) {
                WebViewPermission.CAMERA,
                WebViewPermission.MICROPHONE,
                WebViewPermission.NOTIFICATIONS,
                    -> {
                    // 桌面端通常直接授权，实际权限由系统控制
                    continuation.resume(true)
                }

                WebViewPermission.LOCATION -> {
                    // 地理位置权限需要特殊处理
                    continuation.resume(true)
                }

                else -> continuation.resume(false)
            }
        }
    }

    override fun grantPermission(permission: WebViewPermission) {
        // CEF权限在handler中处理
        when (permission) {
            WebViewPermission.CAMERA -> {
                // 相机权限已在PermissionHandler中授权
            }

            WebViewPermission.MICROPHONE -> {
                // 麦克风权限已在PermissionHandler中授权
            }

            else -> {
                // 其他权限处理
            }
        }
    }

    override fun denyPermission(permission: WebViewPermission) {
        // 权限拒绝逻辑
    }

    override fun pauseAllMedia() {
        evaluateJavaScript(
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
        evaluateJavaScript(
            """
            document.querySelectorAll('audio, video').forEach(function(media) {
                if (media.getAttribute('data-was-playing') === 'true') {
                    media.play();
                    media.removeAttribute('data-was-playing');
                }
            });
            """.trimIndent()
        ) { }
    }

    override fun setMediaVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        evaluateJavaScript(
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
        // CEF文件选择需要通过JavaScript处理
        evaluateJavaScript(
            """
            window.desktopFileChooser = function() {
                return new Promise(function(resolve) {
                    window.cefQuery({
                        request: JSON.stringify({
                            id: Date.now(),
                            method: 'fileChooser',
                            params: '{}'
                        }),
                        onSuccess: function(response) {
                            resolve(JSON.parse(response));
                        },
                        onFailure: function(error_code, error_message) {
                            resolve(null);
                        }
                    });
                });
            };
            """.trimIndent()
        ) { }
    }

    override fun setUserAgent(userAgent: String) {
        this.userAgent = userAgent
        // CEF需要在请求时设置User-Agent
        evaluateJavaScript(
            """
            Object.defineProperty(navigator, 'userAgent', {
                value: '$userAgent',
                writable: false
            });
            """.trimIndent()
        ) { }
    }

    override fun getUserAgent(): String {
        if (userAgent.isEmpty()) {
            // 返回默认的Chrome User-Agent
            userAgent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        return userAgent
    }

    override fun clearCache() {
    }

    override fun clearCookies() {
        // CEF清除Cookies
        evaluateJavaScript(
            """
            document.cookie.split(";").forEach(function(c) { 
                document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"); 
            });
            """.trimIndent()
        ) { }
    }

    override fun clearLocalStorage() {
        evaluateJavaScript(
            """
            try {
                localStorage.clear();
                sessionStorage.clear();
                
                // 清除IndexedDB
                if (window.indexedDB) {
                    indexedDB.databases().then(databases => {
                        databases.forEach(db => {
                            indexedDB.deleteDatabase(db.name);
                        });
                    });
                }
                
                // 清除WebSQL (如果支持)
                if (window.openDatabase) {
                    // WebSQL已被废弃，现代浏览器不支持
                }
            } catch(e) {
                console.log('Error clearing storage:', e);
            }
            """.trimIndent()
        ) { }
    }

    override fun getPageSource(callback: (String) -> Unit) {
        evaluateJavaScript(
            "(function(){return document.documentElement.outerHTML})();"
        ) { html ->
            callback(html)
        }
    }

    override fun takeScreenshot(callback: (ByteArray) -> Unit) {
        // CEF截图功能
        webView.createScreenshot(true)
    }

    override fun canGoBack() = webView.canGoBack()

    override fun canGoForward() = webView.canGoForward()

    override fun loadUrl(
        url: String,
        additionalHttpHeaders: Map<String, String>,
    ) {
        if (additionalHttpHeaders.isNotEmpty()) {
            val request =
                CefRequest.create().apply {
                    this.url = url
                    this.setHeaderMap(additionalHttpHeaders)
                }
            webView.loadRequest(request)
        } else {
            KLogger.dd {
                "DesktopWebView loadUrl $url"
            }
            webView.loadURL(url)
        }

        // 页面加载后注入新标签页拦截脚本
        evaluateJavaScript("setTimeout(function() { ${getNewTabInterceptionScript()} }, 100);") { }
//        evaluateJavaScript("setTimeout(function() { ${getVideoRemovalScript()} }, 1000);") { }
    }

    override fun loadHtml(
        html: String?,
        baseUrl: String?,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {
        KLogger.dd {
            "DesktopWebView loadHtml"
        }
        if (html != null) {
            webView.loadHtml(html, baseUrl ?: KCEFBrowser.BLANK_URI)
            // HTML加载后注入新标签页拦截脚本
            evaluateJavaScript("setTimeout(function() { ${getNewTabInterceptionScript()} }, 100);") { }
//            evaluateJavaScript("setTimeout(function() { ${getVideoRemovalScript()} }, 1000);") { }
        }
    }

    override fun stopVideoRemoval() {
        evaluateJavaScript(
            """
        // 移除CSS样式
        const style = document.getElementById('video-blocker-style');
        if (style) {
            style.remove();
        }
        
        // 停止所有定时器和观察器
        window.videoRemovalActive = false;
        
        console.log('视频移除功能已停止');
    """.trimIndent()
        ) {
            KLogger.dd { "视频移除功能已停止" }
        }
    }

    override suspend fun loadHtmlFile(fileName: String) {
        super.loadRawHtmlFile(fileName)
    }

    override fun postUrl(
        url: String,
        postData: ByteArray,
    ) {
        val request =
            CefRequest.create().apply {
                this.url = url
                this.postData =
                    CefPostData.create().apply {
                        this.addElement(
                            CefPostDataElement.create().apply {
                                this.setToBytes(postData.size, postData)
                            },
                        )
                    }
            }
        webView.loadRequest(request)
    }

    override fun goBack() = webView.goBack()

    override fun goForward() = webView.goForward()

    override fun reload() {
        val currentUrl = webView.url
        if (!currentUrl.isNullOrEmpty()) {
            webView.reloadIgnoreCache()
        } else {
            webView.reload()
        }
    }

    override fun stopLoading() = webView.stopLoad()

    override fun evaluateJavaScript(
        script: String,
        callback: ((String) -> Unit)?,
    ) {
        webView.evaluateJavaScript(script) {
            if (it != null) {
                callback?.invoke(it)
            }
        }
    }

    override fun saveState(): WebViewBundle? {
        return null
    }

    override fun scrollOffset(): Pair<Int, Int> {
        return Pair(0, 0)
    }
}
