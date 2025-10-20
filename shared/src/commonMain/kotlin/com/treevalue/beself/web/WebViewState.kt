package com.treevalue.beself.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.treevalue.beself.setting.WebSettings
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.dw

sealed class WebContent {

    data class Url(
        val url: String,
        val additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) : WebContent()


    data class HtmlPage(
        val html: String,
        val baseUrl: String? = null,
        val encoding: String = "utf-8",
        val mimeType: String? = null,
        val historyUrl: String? = null,
    ) : WebContent()
}

sealed class LoadingState {
    data object Initializing : LoadingState()

    data class Loading(val progress: Float) : LoadingState()

    data object Finished : LoadingState()
}

@Immutable
data class WebViewError(
    val code: Int,
    val description: String,
    val isFromMainFrame: Boolean,
)

class WebViewState(webContent: WebContent) {
    var webViewCallback: WebViewCallback? = null
    val defaultChromeUserAgent =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"

    // 内部缓存的状态值
    private var _lastLoadedUrl: String? by mutableStateOf(null)
    private var _loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)
    private var _pageTitle: String? by mutableStateOf(null)

    // 自动刷新开关
    var autoRefresh: Boolean by mutableStateOf(true)

    var content: WebContent by mutableStateOf(webContent)

    /**
     * 动态获取最后加载的URL，自动从WebView实例中刷新
     */
    var lastLoadedUrl: String?
        get() {
            if (autoRefresh && webView != null) {
                return try {
                    val currentUrl = webView!!.getCurrentUrl()
                    if (currentUrl != _lastLoadedUrl && !currentUrl.isNullOrEmpty()) {
                        _lastLoadedUrl = currentUrl
                        KLogger.dd { "URL自动刷新: $_lastLoadedUrl" }
                    }
                    _lastLoadedUrl
                } catch (e: Exception) {
                    KLogger.dw { "获取URL失败: ${e.message}" }
                    _lastLoadedUrl
                }
            }
            return _lastLoadedUrl
        }
        internal set(value) {
            _lastLoadedUrl = value
        }

    /**
     * 动态获取页面标题，自动从WebView实例中刷新
     */
    var pageTitle: String?
        get() {
            if (autoRefresh && webView != null) {
                return try {
                    val currentTitle = webView!!.getCurrentTitle()
                    if (currentTitle != _pageTitle && !currentTitle.isNullOrEmpty()) {
                        _pageTitle = currentTitle
                        KLogger.dd { "标题自动刷新: $_pageTitle" }
                    }
                    _pageTitle
                } catch (e: Exception) {
                    KLogger.dw { "获取标题失败: ${e.message}" }
                    _pageTitle
                }
            }
            return _pageTitle
        }
        internal set(value) {
            _pageTitle = value
        }

    /**
     * 加载状态，支持自动刷新和手动设置
     */
    var loadingState: LoadingState
        get() {
            if (autoRefresh && webView != null) {
                return try {
                    val currentProgress = webView!!.getProgress()
                    val isLoading = webView!!.isLoading()

                    val newState = when {
                        isLoading && currentProgress < 100 -> LoadingState.Loading(currentProgress / 100.0f)
                        currentProgress == 100 && !isLoading -> LoadingState.Finished
                        currentProgress > 0 -> LoadingState.Loading(currentProgress / 100.0f)
                        else -> LoadingState.Loading(0.0f)
                    }

                    if (newState != _loadingState) {
                        _loadingState = newState
                        KLogger.dd { "加载状态自动刷新: $_loadingState" }
                    }
                    _loadingState
                } catch (e: Exception) {
                    KLogger.dw { "获取加载状态失败: ${e.message}" }
                    _loadingState
                }
            }
            return _loadingState
        }
        internal set(value) {
            _loadingState = value
        }

    /**
     * 当前页面进度（0.0 - 1.0）
     */
    val progress: Float
        get() {
            return when (val state = loadingState) {
                is LoadingState.Loading -> state.progress
                is LoadingState.Finished -> 1.0f
                else -> 0.0f
            }
        }

    val errorsForCurrentRequest: SnapshotStateList<WebViewError> = mutableStateListOf()

    val webSettings: WebSettings by mutableStateOf(WebSettings())

    var webView by mutableStateOf<IWebView?>(null)

    var viewState: WebViewBundle? = null
        internal set
}

@Composable
fun rememberWebViewState(
    url: String,
    additionalHttpHeaders: Map<String, String> = emptyMap(),
): WebViewState =
    remember {
        WebViewState(
            WebContent.Url(
                url = url,
                additionalHttpHeaders = additionalHttpHeaders,
            ),
        )
    }.apply {
        this.content = WebContent.Url(
            url = url,
            additionalHttpHeaders = additionalHttpHeaders,
        )
    }

@Composable
fun rememberWebViewStateWithHTMLData(
    data: String,
    baseUrl: String? = null,
    encoding: String = "utf-8",
    mimeType: String? = null,
    historyUrl: String? = null,
    autoRefresh: Boolean = true,
): WebViewState = remember {
    WebViewState(WebContent.HtmlPage(data, baseUrl, encoding, mimeType, historyUrl)).apply {
        this.autoRefresh = autoRefresh
    }
}.apply {
    this.content = WebContent.HtmlPage(
        data, baseUrl, encoding, mimeType, historyUrl,
    )
}

interface WebViewCallback {
    fun onPermissionRequest(permission: WebViewPermission, callback: (Boolean) -> Unit) {}
    fun onDownloadStart(url: String, filename: String, mimeType: String) {}
    fun onFileChooser(callback: (List<String>?) -> Unit) {}
    fun onGeolocationRequest(origin: String, callback: (Boolean) -> Unit) {}
    fun onJsAlert(message: String, callback: () -> Unit) {}
    fun onJsConfirm(message: String, callback: (Boolean) -> Unit) {}
    fun onConsoleMessage(level: String, message: String, sourceId: String, line: Int) {}
}
