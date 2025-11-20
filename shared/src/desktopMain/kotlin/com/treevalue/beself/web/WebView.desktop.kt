package com.treevalue.beself.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.bus.DownloadEvent
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.config.BrowserConfig
import com.treevalue.beself.js.getHideScrollbarScript
import com.treevalue.beself.net.FileUrlDetector
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefRendering
import org.cef.browser.CefRequestContext
import org.cef.callback.CefBeforeDownloadCallback
import org.cef.callback.CefDownloadItem
import org.cef.callback.CefMediaAccessCallback
import org.cef.handler.CefDisplayHandler
import org.cef.handler.CefDownloadHandlerAdapter
import org.cef.handler.CefKeyboardHandler
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefPermissionHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.Toolkit
import java.net.URI
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.ln

actual class WebViewBundle

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
    val tabId = remember {
        serviceProvider?.let { provider ->
            if (provider is InterceptRequestBackend) {
                provider.getActiveTabInfo()?.id
            } else null
        }
    }
    if (tabId == null) {
        KLogger.de { "tabId is null" }
        return
    }

    val currentOnDispose by rememberUpdatedState(onDispose)
    val scope = rememberCoroutineScope()

    val client = remember {
        SharedKCEFClientManager.getOrCreateSharedClient()?.apply {
            if (state.webSettings.desktopWebSettings.disablePopupWindows) {
                addLifeSpanHandler(DisablePopupWindowsLifeSpanHandler())
            } else {
                if (getLifeSpanHandler() is DisablePopupWindowsLifeSpanHandler) {
                    removeLifeSpanHandler()
                }
            }
        }
    }

    val browser = remember(client, state.webSettings) {
        client?.let {
            var browserInstance = DesktopWebViewManager.getBrowser(tabId)
            if (browserInstance == null) {
                browserInstance = factory(WebViewBuildParam(state, it))
            }
            DesktopWebViewManager.registerBrowser(tabId, browserInstance)
            browserInstance
        }
    }

    val desktopWebView = remember(browser) {
        browser?.let {
            DesktopWebView(
                webView = it,
                scope = scope,
                serviceProvider = serviceProvider
            )
        }
    }

    LaunchedEffect(browser, state.content) {
        if (browser != null && desktopWebView != null && !DesktopWebViewManager.isBrowserLoaded(tabId)) {
            delay(200)

            when (val content = state.content) {
                is WebContent.Url -> {
                    if (content.url.isNotEmpty()) {
                        desktopWebView.loadUrl(content.url, content.additionalHttpHeaders)
                        DesktopWebViewManager.markBrowserLoaded(tabId)
                    }
                }
                is WebContent.HtmlPage -> {
                    desktopWebView.loadHtml(
                        content.html,
                        content.baseUrl,
                        content.mimeType,
                        content.encoding,
                        content.historyUrl
                    )
                    DesktopWebViewManager.markBrowserLoaded(tabId)
                }
                else -> {
                    DesktopWebViewManager.markBrowserLoaded(tabId)
                }
            }
        }
    }

    // 渲染WebView
    browser?.let { browserInstance ->
        SwingPanel(
            factory = {
                setupWebView(
                    kbrowser = browserInstance,
                    state = state,
                    navigator = navigator,
                    desktopWebView = desktopWebView,
                    scope = scope,
                    onCreated = onCreated
                )
                browserInstance.uiComponent
            },
            modifier = modifier,
        )
    }

    // 清理资源
    DisposableEffect(client, browser) {
        onDispose {
            try {
                browser?.let { currentOnDispose(it) }
                // 不要dispose共享的client，只有在应用退出时才dispose
            } catch (e: Exception) {
                // 忽略清理时的异常
            }
        }
    }
}

actual class WebViewBuildParam(
    val state: WebViewState,
    val client: KCEFClient,
) {
    val webSettings get() = state.webSettings
    val rendering: CefRendering
        get() = if (webSettings.desktopWebSettings.offScreenRendering) {
            CefRendering.OFFSCREEN
        } else {
            CefRendering.DEFAULT
        }
    val transparent: Boolean get() = webSettings.desktopWebSettings.transparent
    val requestContext: CefRequestContext get() = CefRequestContext.getGlobalContext()
}

actual class PlatformWebViewParams

actual fun buildDefaultWebview(param: WebViewBuildParam): NativeWebView {
    return when (val content = param.state.content) {
        is WebContent.Url -> param.client.createBrowser(
            content.url,
            param.rendering,
            param.transparent,
            param.requestContext,
        )

        is WebContent.HtmlPage -> param.client.createBrowserWithHtml(
            content.html,
            content.baseUrl ?: KCEFBrowser.BLANK_URI,
            param.rendering,
            param.transparent,
            param.requestContext
        )

        else -> param.client.createBrowser(
            KCEFBrowser.BLANK_URI,
            param.rendering,
            param.transparent,
            param.requestContext,
        )
    }
}

object SharedKCEFClientManager {
    @Volatile
    private var sharedClient: KCEFClient? = null

    @Synchronized
    fun getOrCreateSharedClient(): KCEFClient? {
        if (sharedClient == null) {
            sharedClient = KCEF.newClientOrNullBlocking()
        }
        return sharedClient
    }

    @Synchronized
    fun disposeSharedClient() {
        sharedClient?.dispose()
        sharedClient = null
    }
}

private fun setupWebView(
    kbrowser: KCEFBrowser,
    state: WebViewState,
    navigator: WebViewController,
    desktopWebView: DesktopWebView?,
    scope: CoroutineScope,
    onCreated: (NativeWebView) -> Unit,
) {
    // 通知外部WebView已创建
    onCreated(kbrowser)

    // 设置状态
    state.webView = desktopWebView

    // 添加处理器
    kbrowser.apply {
        this.client.addKeyboardHandler(object : CefKeyboardHandler {
            /** The onPreKeyEvent method should return true if the event was
             * handled by the host application and should not be sent to the renderer.
             */
            override fun onPreKeyEvent(
                browser: CefBrowser?,
                event: CefKeyboardHandler.CefKeyEvent?,
                eventHandle: BoolRef?,
            ): Boolean {
                return false
            }

            // after render process deal
            override fun onKeyEvent(
                browser: CefBrowser?,
                event: CefKeyboardHandler.CefKeyEvent?,
            ): Boolean {
                // F12 的键码是 123
                if (BrowserConfig.useDebugModel && event?.windows_key_code == 123 && event.type == CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) {
                    val screenSize = Toolkit.getDefaultToolkit().screenSize
                    SwingUtilities.invokeLater {
                        val frame = JFrame("DevTools")
                        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                        frame.add(devTools.uiComponent)
                        frame.setSize(screenSize.width, screenSize.height)
                        frame.isVisible = true
                    }
                    return true  // 消费该事件
                }
                return false
            }
        })

        this.client.addDownloadHandler(object : CefDownloadHandlerAdapter() {
            override fun onBeforeDownload(
                browser: CefBrowser?,
                downloadItem: CefDownloadItem?,
                suggestedName: String?,
                callback: CefBeforeDownloadCallback?,
            ) {
                downloadItem?.let { item ->
                    val filename = suggestedName ?: FileUrlDetector.extractFilename(item.url)


                    // 区分两种场景
                    if (FileUrlDetector.isDownloadableUrl(item.url)) {
                        // 场景1：URL本身是下载文件 - 提供选择下载
                        EventBus.publish(DownloadEvent.DownloadAvailable(item.url, filename))
                    } else if (FileUrlDetector.isBlockedFileType(item.url)) {

                    } else {
                        // 场景2：网页内下载 - 自动开始
                        EventBus.publish(DownloadEvent.AutoStartDownload(item.url, filename))
                    }
                }
                callback?.Continue("", false)
            }
        })

        this.client.addPermissionHandler(object : CefPermissionHandler {
            override fun onRequestMediaAccessPermission(
                browser: CefBrowser?,
                frame: CefFrame?,
                requesting_origin: String?,
                requested_permissions: Int,
                callback: CefMediaAccessCallback?,
            ): Boolean {
                KLogger.dd { "CEF权限请求: origin=$requesting_origin, permissions=$requested_permissions" }

                // 根据权限类型决定是否授权
                val shouldGrant = when {
                    (requested_permissions and 1) != 0 -> true  // 麦克风
                    (requested_permissions and 2) != 0 -> true  // 摄像头
                    (requested_permissions and 4) != 0 -> true  // 桌面捕获
                    else -> false
                }

                callback?.Continue(if (shouldGrant) 1 else 0)
                return true
            }
        })

        this.client.addDisplayHandler(
            object : CefDisplayHandler {
                override fun onAddressChange(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    url: String?,
                ) {
                    KLogger.dd { "onAddressChange: $url" }
                    state.lastLoadedUrl = url
                }

                override fun onTitleChange(
                    browser: CefBrowser?,
                    title: String?,
                ) {
                    val givenZoomLevel = state.webSettings.zoomLevel
                    val realZoomLevel =
                        if (givenZoomLevel >= 0.0) {
                            ln(abs(givenZoomLevel)) / ln(1.2)
                        } else {
                            -ln(abs(givenZoomLevel)) / ln(1.2)
                        }
                    KLogger.dd { "titleProperty: $title" }
                    zoomLevel = realZoomLevel
                    state.pageTitle = title
                }

                override fun onFullscreenModeChange(
                    p0: CefBrowser?,
                    p1: Boolean,
                ) {
                    // Not supported
                }

                override fun onTooltip(
                    browser: CefBrowser?,
                    text: String?,
                ): Boolean {
                    return false
                }

                override fun onStatusMessage(
                    browser: CefBrowser?,
                    value: String?,
                ) {
                }

                override fun onConsoleMessage(
                    browser: CefBrowser?,
                    level: CefSettings.LogSeverity?,
                    message: String?,
                    source: String?,
                    line: Int,
                ): Boolean {
                    return false
                }

                override fun onCursorChange(
                    browser: CefBrowser?,
                    cursorType: Int,
                ): Boolean {
                    return false
                }
            },
        )

        this.client.addLoadHandler(
            object : CefLoadHandler {
                private var lastLoadedUrl = "null"

                override fun onLoadingStateChange(
                    browser: CefBrowser?,
                    isLoading: Boolean,
                    canGoBack: Boolean,
                    canGoForward: Boolean,
                ) {
                    if (isLoading) {
                        state.loadingState = LoadingState.Initializing
                    } else {
                        state.loadingState = LoadingState.Finished
                        if (url != null && url != lastLoadedUrl) {
                            lastLoadedUrl = url
                        }
                    }
                    navigator.canGoBack = canGoBack
                    navigator.canGoForward = canGoForward
                }

                override fun onLoadStart(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?,
                ) {
                    lastLoadedUrl = "null" // clean last loaded url for reload to work
                    state.loadingState = LoadingState.Loading(0F)
                    state.errorsForCurrentRequest.clear()
                    if (frame?.isMain == true) {
                        evaluateJavaScript(
                            """
                    // 预防性阻止视频播放
                    document.addEventListener('DOMContentLoaded', function() {
                        const style = document.createElement('style');
                        style.textContent = 'video { display: none !important; visibility: hidden !important; }';
                        document.head.appendChild(style);
                    });
                """.trimIndent()
                        ) { }
                    }

                }

                override fun onLoadEnd(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    httpStatusCode: Int,
                ) {
                    state.loadingState = LoadingState.Finished
                    navigator.canGoBack = canGoBack()
                    navigator.canGoBack = canGoForward()
                    state.lastLoadedUrl = url
                    if (frame?.isMain == true) {
                        evaluateJavaScript(getHideScrollbarScript()) { }
                    }
                }

                override fun onLoadError(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    errorCode: CefLoadHandler.ErrorCode?,
                    errorText: String?,
                    failedUrl: String?,
                ) {
                    state.loadingState = LoadingState.Finished
                    state.errorsForCurrentRequest.add(
                        WebViewError(
                            code = errorCode?.code ?: 404,
                            description = "Failed to load url: ${failedUrl}\n$errorText",
                            isFromMainFrame = frame?.isMain ?: false,
                        ),
                    )
                }
            },
        )

        client.addRequestHandler(
            object : CefRequestHandlerAdapter() {
                override fun onBeforeBrowse(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    request: CefRequest?,
                    userGesture: Boolean,
                    isRedirect: Boolean,
                ): Boolean {
                    val url = request?.url
                    if (!url.isNullOrEmpty() && frame?.isMain == true && FileUrlDetector.isDownloadableUrl(url)) {
                        val filename = FileUrlDetector.extractFilename(url)
                        KLogger.dd { "检测到下载文件URL: $url, filename: $filename" }

                        // 发布拦截事件，由后端处理
                        EventBus.publish(DownloadEvent.DownloadAvailable(url, filename))
                    }
                    navigator.requestInterceptor?.apply {
                        val map = mutableMapOf<String, String>()
                        request?.getHeaderMap(map)
                        KLogger.dd { "onBeforeBrowse ${request?.url} $map" }
                        val webRequest =
                            WebRequest(
                                request?.url.toString(),
                                map,
                                isForMainFrame = frame?.isMain ?: false,
                                isRedirect = isRedirect,
                                request?.method ?: "GET",
                            )
                        val interceptResult =
                            this.onInterceptUrlRequest(
                                webRequest,
                                navigator,
                            )
                        return when (interceptResult) {
                            is WebRequestInterceptResult.Allow -> {
                                super.onBeforeBrowse(browser, frame, request, userGesture, isRedirect)
                            }

                            is WebRequestInterceptResult.Reject -> {
                                true
                            }

                            is WebRequestInterceptResult.Modify -> {
                                interceptResult.request.apply {
                                    navigator.loadUrl(this.url, this.headers)
                                }
                                true
                            }
                        }
                    }
                    return super.onBeforeBrowse(browser, frame, request, userGesture, isRedirect)
                }
            },
        )

        this.client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser?,
                frame: CefFrame?,
                target_url: String?,
                target_frame_name: String?,
            ): Boolean {
                KLogger.dd { "桌面端检测到弹窗请求: $target_url" }

                if (!target_url.isNullOrEmpty()) {
                    // 发布新标签页事件
                    val title = target_frame_name?.takeIf { it.isNotEmpty() }
                        ?: extractTitleFromUrl(target_url)
                        ?: "newTab"

                    EventBus.publish(TabEvent.RequestNewTab(target_url, title))
                    KLogger.dd { "桌面端发布新标签页事件: $target_url, 标题: $title" }
                }

                // 返回true阻止默认的弹窗行为
                return true
            }
        })

    }
}

fun extractTitleFromUrl(url: String): String? {
    return try {
        val uri = URI(url)
        uri.host
    } catch (e: Exception) {
        null
    }
}

// 在应用退出时调用
fun cleanupSharedResources() {
    SharedKCEFClientManager.disposeSharedClient()
}
