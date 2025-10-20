package com.treevalue.beself.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.platform.g_desktop
import com.treevalue.beself.platform.getPlatformName

@Composable
fun webView(
    state: WebViewState,
    navigator: WebViewController,
    modifier: Modifier = Modifier,
    webViewCallback: WebViewCallback? = null,
    platformWebViewParams: PlatformWebViewParams? = null,
    serviceProvider: ServiceProvider? = null,
) {
    state.webViewCallback = webViewCallback

    // Chrome兼容模式配置
    state.webSettings.apply {
        userAgent = state.defaultChromeUserAgent
        allowThirdPartyCookies = true
        enableWebRTC = true
        enableWebGL = true
        enableWebAssembly = true
        allowGoogleAccountLogin = true
    }

    setupPlatformWebView(
        state = state,
        modifier = modifier,
        navigator = navigator,
        platformWebViewParams = platformWebViewParams,
        factory = ::buildDefaultWebview,
        serviceProvider = serviceProvider,
    )

    // 处理导航事件
    state.webView?.let { wv ->
        LaunchedEffect(wv, navigator) {
            state.lastLoadedUrl?.let {
                if (serviceProvider is InterceptRequestBackend) {
                    serviceProvider.checkUrlForDownload(it)
                }
            }
            with(navigator) {
                wv.handleNavigationEvents()
            }
        }

        // 处理内容加载（非桌面平台）
        if (getPlatformName() != g_desktop) {
            LaunchedEffect(wv, state) {
                snapshotFlow { state.content }.collect { content ->
                    when (content) {
                        is WebContent.Url -> {
                            state.lastLoadedUrl = content.url
                            wv.loadUrl(content.url, content.additionalHttpHeaders)
                        }

                        is WebContent.HtmlPage -> {
                            wv.loadHtml(
                                content.html,
                                content.baseUrl,
                                content.mimeType,
                                content.encoding,
                                content.historyUrl
                            )
                        }
                    }
                }
            }
        }
    }
}

expect class WebViewBuildParam

expect class PlatformWebViewParams

expect fun buildDefaultWebview(param: WebViewBuildParam): NativeWebView

@Composable
expect fun setupPlatformWebView(
    state: WebViewState,
    navigator: WebViewController,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    onCreated: (NativeWebView) -> Unit = {},
    onDispose: (NativeWebView) -> Unit = {},
    platformWebViewParams: PlatformWebViewParams? = null,
    factory: (WebViewBuildParam) -> NativeWebView = ::buildDefaultWebview,
    serviceProvider: ServiceProvider? = null,
)
