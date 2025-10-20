package com.treevalue.beself.web

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class WebViewController(val scope: CoroutineScope, val requestInterceptor: RequestInterceptor? = null) {
    private sealed interface NavigationEvent {
        data object Back : NavigationEvent

        data object Forward : NavigationEvent

        data object Reload : NavigationEvent

        data object StopLoading : NavigationEvent

        data class LoadUrl(
            val url: String,
            val additionalHttpHeaders: Map<String, String> = emptyMap(),
        ) : NavigationEvent

        data class LoadHtml(
            val html: String,
            val baseUrl: String? = null,
            val mimeType: String? = null,
            val encoding: String? = "utf-8",
            val historyUrl: String? = null,
        ) : NavigationEvent

        data class LoadHtmlFile(
            val fileName: String,
        ) : NavigationEvent

        data class PostUrl(
            val url: String,
            val postData: ByteArray,
        ) : NavigationEvent {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as PostUrl

                if (url != other.url) return false
                if (!postData.contentEquals(other.postData)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = url.hashCode()
                result = 31 * result + postData.contentHashCode()
                return result
            }
        }

        data class EvaluateJavaScript(
            val script: String,
            val callback: ((String) -> Unit)?,
        ) : NavigationEvent
    }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow(replay = 1)

    internal suspend fun IWebView.handleNavigationEvents(): Nothing =
        withContext(Dispatchers.Main) {
            navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.Back -> goBack()
                    is NavigationEvent.Forward -> goForward()
                    is NavigationEvent.Reload -> reload()
                    is NavigationEvent.StopLoading -> stopLoading()
                    is NavigationEvent.LoadHtml ->
                        loadHtml(
                            event.html,
                            event.baseUrl,
                            event.mimeType,
                            event.encoding,
                            event.historyUrl,
                        )

                    is NavigationEvent.LoadHtmlFile -> {
                        loadHtmlFile(event.fileName)
                    }

                    is NavigationEvent.LoadUrl -> {
                        loadUrl(event.url, event.additionalHttpHeaders)
                    }

                    is NavigationEvent.PostUrl -> {
                        postUrl(event.url, event.postData)
                    }

                    is NavigationEvent.EvaluateJavaScript -> {
                        evaluateJavaScript(event.script, event.callback)
                    }
                }
            }
        }

    var canGoBack: Boolean by mutableStateOf(false)
        internal set

    var canGoForward: Boolean by mutableStateOf(false)
        internal set

    fun loadUrl(
        url: String,
        additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) {
        scope.launch {
            navigationEvents.emit(
                NavigationEvent.LoadUrl(
                    url,
                    additionalHttpHeaders,
                ),
            )
        }
    }

    fun evaluateJavaScript(
        script: String,
        callback: ((String) -> Unit)? = null,
    ) {
        scope.launch {
            navigationEvents.emit(
                NavigationEvent.EvaluateJavaScript(
                    script,
                    callback,
                ),
            )
        }
    }

    fun navigateBack() {
        scope.launch {
            navigationEvents.emit(NavigationEvent.Back)
        }
    }

    fun navigateForward() {
        scope.launch { navigationEvents.emit(NavigationEvent.Forward) }
    }

    fun reload() {
        scope.launch { navigationEvents.emit(NavigationEvent.Reload) }
    }

    fun stopLoading() {
        scope.launch { navigationEvents.emit(NavigationEvent.StopLoading) }
    }
}
