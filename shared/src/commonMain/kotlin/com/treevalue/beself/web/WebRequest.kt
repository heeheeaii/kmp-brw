package com.treevalue.beself.web


sealed interface WebRequestInterceptResult {
    data object Allow : WebRequestInterceptResult

    data object Reject : WebRequestInterceptResult

    class Modify(val request: WebRequest) : WebRequestInterceptResult
}

interface RequestInterceptor {
    fun onInterceptUrlRequest(
        request: WebRequest,
        navigator: WebViewController,
    ): WebRequestInterceptResult
}

data class WebRequest(
    val url: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val isForMainFrame: Boolean = false,
    val isRedirect: Boolean = false,
    val method: String = "GET",
)
