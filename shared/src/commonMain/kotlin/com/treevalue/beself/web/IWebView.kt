package com.treevalue.beself.web

import compose_webview_multiplatform.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi

expect class WebViewBundle()

expect class NativeWebView

/**
 * WebView状态信息接口，避免环形依赖
 */
interface WebViewStateProvider {
    fun getCurrentUrl(): String?
    fun getCurrentTitle(): String?
    fun getProgress(): Int
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
    fun isLoading(): Boolean
}

interface IWebView : WebViewStateProvider {

    val webView: NativeWebView

    val scope: CoroutineScope

    fun loadUrl(
        url: String,
        additionalHttpHeaders: Map<String, String> = emptyMap(),
    )

    fun loadHtml(
        html: String? = null,
        baseUrl: String? = null,
        mimeType: String? = "text/html",
        encoding: String? = "utf-8",
        historyUrl: String? = null,
    )

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadRawHtmlFile(fileName: String) {
        val html = Res.readBytes(fileName).decodeToString().trimIndent()
        loadHtml(html, encoding = "utf-8")
    }

    suspend fun loadHtmlFile(fileName: String)

    fun postUrl(
        url: String,
        postData: ByteArray,
    )

    fun goBack()

    fun goForward()

    fun reload()

    fun stopLoading()

    fun stopVideoRemoval() {
    }

    fun evaluateJavaScript(
        script: String,
        callback: ((String) -> Unit)? = null,
    )

    fun saveState(): WebViewBundle?

    fun scrollOffset(): Pair<Int, Int>

    // 权限管理
    suspend fun requestPermission(permission: WebViewPermission): Boolean
    fun grantPermission(permission: WebViewPermission)
    fun denyPermission(permission: WebViewPermission)

    // 媒体控制
    fun pauseAllMedia()
    fun resumeAllMedia()
    fun setMediaVolume(volume: Float)

    // 下载管理
    fun setDownloadListener(listener: (url: String, filename: String) -> Unit)

    // 文件选择
    fun setFileChooserListener(listener: suspend () -> List<String>?)

    // 用户代理
    fun setUserAgent(userAgent: String)
    fun getUserAgent(): String

    // Cookie和存储
    fun clearCache()
    fun clearCookies()
    fun clearLocalStorage()

    // 网页内容交互
    fun getPageSource(callback: (String) -> Unit)
    fun takeScreenshot(callback: (ByteArray) -> Unit)
}

// 新增权限枚举
enum class WebViewPermission {
    CAMERA,
    MICROPHONE,
    LOCATION,
    NOTIFICATIONS,
    STORAGE,
    MEDIA_AUTOPLAY
}
