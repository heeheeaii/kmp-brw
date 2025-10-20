package com.treevalue.beself

import androidx.compose.runtime.Composable
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.web.PlatformWebViewParams
import com.treevalue.beself.web.WebViewCallback
import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()

@Composable
actual fun getPlatformWebViewParamsWithBackend(backend: InterceptRequestBackend): PlatformWebViewParams? = null

@Composable
actual fun getOptimizedWebViewCallback(): WebViewCallback? = null
