package com.treevalue.beself

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.config.BrowserConfig
import com.treevalue.beself.config.applyDefault
import com.treevalue.beself.net.createRequestInterceptor
import com.treevalue.beself.ui.BrowserTopBar
import com.treevalue.beself.ui.ConfirmDialog
import com.treevalue.beself.ui.DownloadSidebar
import com.treevalue.beself.ui.SiteSidebar
import com.treevalue.beself.ui.TabBar
import com.treevalue.beself.ui.enhanceToggleForceDarkMode
import com.treevalue.beself.ui.toast
import com.treevalue.beself.ui.toggleForceDarkMode
import com.treevalue.beself.web.LoadingState
import com.treevalue.beself.web.PlatformWebViewParams
import com.treevalue.beself.web.webView
import com.treevalue.beself.web.WebViewCallback
import com.treevalue.beself.web.WebViewController
import com.treevalue.beself.web.rememberWebViewState
import com.treevalue.beself.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import popControl

@Composable
fun interceptRequestEnter() {
    val scope = rememberCoroutineScope()
    val backend = remember { InterceptRequestBackend.getInstance(scope) }

    val isInitialized by backend.isInitialized.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    DisposableEffect(Unit) {
        onDispose {
            backend.cleanup()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // 保存一次时间
            backend.saveState()
        }
    }

    if (!isInitialized) {
        return
    }

    val activeTabState = backend.getActiveTabState()?.first
    val colors = if (backend.forceDark.value) {
        darkColors()
    } else {
        lightColors(
            primary = Color.Black,
            background = Color.Black,
            surface = Color.Gray
        )
    }

    MaterialTheme(
        colors = colors
    ) {
        Box(Modifier.fillMaxSize()) {
            Column {
                BrowserTopBar(
                    navigator = backend.activeNavigator.value,
                    forceDark = backend.forceDark.value,
                    sidebarVisible = backend.sidebarVisible.value,
                    downloadState = backend.downloadIndicatorState.value,
                    backend = backend,
                    onToggleForceDark = { backend.toggleForceDark() },
                    onToggleSidebar = { backend.toggleSidebar() },
                    onDownloadClick = { backend.toggleDownloadSidebar() },
                    onCopyUrl = {
                        val activeTab = backend.getActiveTabInfo()
                        val currentUrl = backend.getActiveTabState()?.first?.lastLoadedUrl
                            ?: activeTab?.initialUrl
                        currentUrl?.let { url ->
                            clipboardManager.setText(AnnotatedString(url))
                        }
                    }
                )

                TabBar(
                    tabs = backend.tabs,
                    activeTabIndex = backend.activeTabIndex.intValue,
                    onTabSelected = { index -> backend.selectTab(index) },
                    onTabClosed = { tabInfo -> backend.closeTab(tabInfo) },
                    onNewTab = { backend.addNewTab() }
                )

                val loadingState = activeTabState?.loadingState
                if (loadingState is LoadingState.Loading) {
                    LinearProgressIndicator(loadingState.progress, Modifier.fillMaxWidth())
                }

                Row(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = backend.sidebarVisible.value || backend.downloadSidebarVisible.value,
                        enter = slideInHorizontally { -it },
                        exit = slideOutHorizontally { -it }
                    ) {
                        when {
                            backend.sidebarVisible.value -> {
                                SiteSidebar(
                                    backend = backend,
                                    onSiteClick = { label, urlOrHost ->
                                        backend.addSiteFromSidebar(label, urlOrHost)
                                    }
                                )
                            }

                            backend.downloadSidebarVisible.value -> {
                                DownloadSidebar(
                                    downloadTasks = backend.downloadTasks,
                                    availableDownloads = backend.availableDownloads,
                                    onDeleteTask = { backend.deleteDownloadTask(it) },
                                    onPauseTask = { backend.pauseDownloadTask(it) },
                                    onResumeTask = { backend.resumeDownloadTask(it) },
                                    onStartDownload = { url, filename ->
                                        backend.startDownload(url, filename)
                                    },
                                    onRemoveAvailableDownload = { url ->
                                        backend.removeAvailableDownload(url)
                                    }
                                )
                            }
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxSize().zIndex(-1.0f)) {
                        val boxScope = rememberCoroutineScope()
                        backend.tabs.forEachIndexed { index, tabInfo ->
                            key(tabInfo.id) {
                                var state = backend.getTabState(tabInfo.id)?.first
                                val isHasCache = state != null

                                if (state == null) {
                                    state = if (tabInfo.initialHtml != null) {
                                        rememberWebViewStateWithHTMLData(data = tabInfo.initialHtml)
                                    } else if (tabInfo.initialUrl != null) {
                                        rememberWebViewState(url = tabInfo.initialUrl)
                                    } else {
                                        rememberWebViewStateWithHTMLData(data = BrowserConfig.INITIAL_HTML)
                                    }
                                    LaunchedEffect(Unit) {
                                        state.webSettings.applyDefault()
                                    }
                                }

                                var navigator = backend.getTabState(tabInfo.id)?.second
                                if (navigator == null) {
                                    navigator = remember(boxScope) {
                                        WebViewController(
                                            boxScope,
                                            createRequestInterceptor(backend)
                                        )
                                    }
                                }

                                if (!isHasCache) {
                                    backend.cacheTabState(tabInfo.id, state, navigator)
                                }

                                if (index == backend.activeTabIndex.intValue) {
                                    LaunchedEffect(navigator) {
                                        backend.setActiveNavigator(navigator)
                                    }
                                    LaunchedEffect(
                                        backend.forceDark.value,
                                        state.loadingState,
                                        backend.activeTabIndex.intValue,
                                        state.lastLoadedUrl
                                    ) {
                                        // 只有当是激活标签页时才应用黑暗模式
                                        if (index == backend.activeTabIndex.intValue && backend.forceDark.value) {
                                            when (state.loadingState) {
                                                is LoadingState.Finished -> {
                                                    enhanceToggleForceDarkMode(navigator)
                                                }

                                                is LoadingState.Loading -> {
                                                    // 页面开始加载时立即应用（处理页面内导航）
                                                    delay(100) // 短暂延迟确保页面开始渲染
                                                    toggleForceDarkMode(true, navigator)
                                                }

                                                else -> {
                                                    if (tabInfo.initialHtml != null) {
                                                        toggleForceDarkMode(true, navigator)
                                                    } else {
                                                        delay(200)
                                                        toggleForceDarkMode(true, navigator)
                                                    }
                                                }
                                            }
                                        } else if (index == backend.activeTabIndex.intValue && !backend.forceDark.value) {
                                            toggleForceDarkMode(false, navigator)
                                        }
                                    }
                                }
                                webView(
                                    state = state,
                                    navigator = navigator,
                                    modifier = if ((index == backend.activeTabIndex.intValue)
                                        && !backend.showPop.value
                                    ) {
                                        Modifier.fillMaxSize()
                                    } else {
                                        Modifier.alpha(0f).size(0.dp)
                                    },
                                    webViewCallback = getOptimizedWebViewCallback(),
                                    platformWebViewParams = getPlatformWebViewParamsWithBackend(backend),
                                    serviceProvider = backend
                                )
                            }
                        }
                    }
                }
            }
        }
        backend.toastMessage.value?.let { message ->
            toast(message = message) {
                backend.clearToast()
            }
        }

        backend.confirmDialog.value?.let { dialogState ->
            ConfirmDialog(state = dialogState)
        }

        popControl(backend = backend) { backend.setShowPop(it) }
    }
}

expect fun randomUUID(): String

@Composable
expect fun getPlatformWebViewParamsWithBackend(backend: InterceptRequestBackend): PlatformWebViewParams?

@Composable
expect fun getOptimizedWebViewCallback(): WebViewCallback?
