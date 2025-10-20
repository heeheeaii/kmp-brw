package com.treevalue.beself

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.web.OptimizedWebViewClient
import com.treevalue.beself.web.PlatformWebViewParams
import com.treevalue.beself.web.PopupSupportWebChromeClient
import com.treevalue.beself.web.WebViewCallback
import com.treevalue.beself.web.WebViewPermission
import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()

class OptimizedWebViewCallback : WebViewCallback {
    override fun onPermissionRequest(permission: WebViewPermission, callback: (Boolean) -> Unit) {
        when (permission) {
            WebViewPermission.CAMERA -> callback(true)
            WebViewPermission.MICROPHONE -> callback(true)
            WebViewPermission.LOCATION -> callback(true)
            WebViewPermission.MEDIA_AUTOPLAY -> callback(true)
            WebViewPermission.NOTIFICATIONS -> callback(false)
            WebViewPermission.STORAGE -> callback(true)
        }
    }

    override fun onDownloadStart(url: String, filename: String, mimeType: String) {
        
    }

    override fun onFileChooser(callback: (List<String>?) -> Unit) {
        
        callback(null) // 文件选择由ChromeClient处理
    }

    override fun onGeolocationRequest(origin: String, callback: (Boolean) -> Unit) {
        callback(false)
    }

    override fun onJsAlert(message: String, callback: () -> Unit) {
        callback()
    }

    override fun onJsConfirm(message: String, callback: (Boolean) -> Unit) {
        
        callback(true)
    }

    override fun onConsoleMessage(level: String, message: String, sourceId: String, line: Int) {
        val ignoredMessages = listOf(
            "reportClientInfo",
            "webkitRequestFullScreen",
            "Non-Error promise rejection captured",
            "ResizeObserver loop limit exceeded"
        )

        val shouldIgnore = ignoredMessages.any { message.contains(it, ignoreCase = true) }

        if (!shouldIgnore && level == "ERROR") {
            
        }
    }
}

class FileSupportChromeClient(
    private val onShowFilePicker: (Intent) -> Unit,
    private val onCreateNewTab: (String) -> Unit,
) : PopupSupportWebChromeClient(onShowFilePicker, onCreateNewTab) {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress % 25 == 0) { // 减少日志输出
            
        }
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        // optional: title can change use this title
        if (!title.isNullOrBlank() && !title.contains("about:blank")) {
            
        }
    }
}

private fun Intent.getUris(): List<Uri>? {
    val clipData = clipData ?: return null
    return (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
}

@Composable
actual fun getPlatformWebViewParamsWithBackend(backend: InterceptRequestBackend): PlatformWebViewParams? {
    var fileChooserIntent by remember { mutableStateOf<Intent?>(null) }

    val chromeClient = remember(backend) {
        FileSupportChromeClient(
            onShowFilePicker = { fileChooserIntent = it },
            onCreateNewTab = { url ->
                backend.activeNavigator.value?.loadUrl(url)
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) {
            chromeClient.cancelFileChooser()
            return@rememberLauncherForActivityResult
        }

        val intent = result.data
        if (intent == null) {
            chromeClient.cancelFileChooser()
            return@rememberLauncherForActivityResult
        }

        val singleFile: Uri? = intent.data
        val multiFiles: List<Uri>? = intent.getUris()

        when {
            singleFile != null -> chromeClient.onReceiveFiles(arrayOf(singleFile))
            multiFiles != null -> chromeClient.onReceiveFiles(multiFiles.toTypedArray())
            else -> chromeClient.cancelFileChooser()
        }
    }

    LaunchedEffect(key1 = fileChooserIntent) {
        if (fileChooserIntent != null) {
            try {
                launcher.launch(fileChooserIntent)
            } catch (e: ActivityNotFoundException) {
                chromeClient.cancelFileChooser()
            }
        }
    }

    return PlatformWebViewParams(
        chromeClient = chromeClient,
        webviewClient = OptimizedWebViewClient(backend)
    )
}

@Composable
actual fun getOptimizedWebViewCallback(): WebViewCallback? {
    return remember { OptimizedWebViewCallback() }
}
