package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.web.WebViewController

enum class DownloadIndicatorState {
    NONE,        // 灰色，没有下载任务
    DOWNLOADING, // 亮色，正在下载
    HAS_NEW      // 红点，有新下载任务
}

@Composable
fun BrowserTopBar(
    navigator: WebViewController?,
    forceDark: Boolean,
    sidebarVisible: Boolean,
    downloadState: DownloadIndicatorState = DownloadIndicatorState.NONE,
    onToggleForceDark: () -> Unit,
    onToggleSidebar: () -> Unit,
    onDownloadClick: () -> Unit = {},
    backend: InterceptRequestBackend? = null,
    onCopyUrl: () -> Unit = {},
) {
    TopAppBar(
        title = {},
        backgroundColor = MaterialTheme.colors.primarySurface,
        modifier = Modifier.height(30.dp),
        actions = {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    IconButton(onClick = {
                        val failedSite = backend?.getActiveTabFailedSite()
                        if (failedSite != null) {
                            backend.retryFailedSite(failedSite)
                        } else {
                            navigator?.reload()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                item {
                    IconButton(onClick = {
                        backend?.searchState?.resetForNewSearch()
                        EventBus.publish(PopEvent.SearchSite)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search sites",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                item {
                    IconButton(
                        onClick = { navigator?.navigateBack() },
                        enabled = true,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                item {
                    IconButton(
                        onClick = { navigator?.navigateForward() },
                        enabled = true,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                }
                item {
                    IconButton(onClick = onToggleForceDark) {
                        Text(if (forceDark) "\uD83D\uDD05" else "🔆", fontSize = 20.sp)
                    }
                }

                item {
                    IconButton(onClick = {
                        backend?.closeAllTabs()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Close All Tabs",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                item {
                    IconButton(onClick = onCopyUrl) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "copy site",
                        )
                    }
                }
                item {
                    IconButton(onClick = onToggleSidebar) {
                        Icon(
                            imageVector = if (sidebarVisible) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (sidebarVisible) "Hide sidebar" else "Show sidebar"
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            val iconColor = when (downloadState) {
                                DownloadIndicatorState.NONE -> Color.Gray
                                DownloadIndicatorState.DOWNLOADING -> MaterialTheme.colors.primary
                                DownloadIndicatorState.HAS_NEW -> MaterialTheme.colors.primary
                            }

                            Icon(
                                imageVector = Icons.Default.GetApp,
                                contentDescription = "download manage",
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 红点指示器
                        if (downloadState == DownloadIndicatorState.HAS_NEW) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
    )
}
