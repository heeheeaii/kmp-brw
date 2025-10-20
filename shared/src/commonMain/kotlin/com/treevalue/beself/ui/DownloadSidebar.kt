package com.treevalue.beself.ui

import androidx.compose.material.Divider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.net.openDownloadDirectory
import com.treevalue.beself.values.startDownload
import com.treevalue.beself.net.DownloadStatus
import com.treevalue.beself.net.DownloadTask


@Composable
fun DownloadSidebar(
    downloadTasks: List<DownloadTask>,
    availableDownloads: List<Pair<String, String>>,
    onDeleteTask: (DownloadTask) -> Unit,
    onPauseTask: (DownloadTask) -> Unit,
    onResumeTask: (DownloadTask) -> Unit,
    onStartDownload: (String, String) -> Unit,
    onRemoveAvailableDownload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(200.dp)
            .padding(4.dp)
            .background(MaterialTheme.colors.surface),
        verticalArrangement = Arrangement.Top
    ) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 1.dp, top = 1.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "下载",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
            )

            IconButton(
                onClick = {
                    openDownloadDirectory()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "打开下载目录",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 下载任务列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 显示可下载项
            if (availableDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "可下载",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                items(availableDownloads) { (url, filename) ->
                    AvailableDownloadItem(
                        url = url,
                        filename = filename,
                        onStartDownload = { onStartDownload(url, filename) },
                        onRemove = { onRemoveAvailableDownload(url) }
                    )
                }
            }

            // 分隔线
            if (availableDownloads.isNotEmpty() && downloadTasks.isNotEmpty()) {
                item {
                    Divider(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                    )
                }
            }

            // 显示下载任务
            if (downloadTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "下载任务",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                items(downloadTasks) { task ->
                    DownloadTaskItem(
                        task = task,
                        onDelete = { onDeleteTask(task) },
                        onPause = { onPauseTask(task) },
                        onResume = { onResumeTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 文件名和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除下载",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 进度条和控制按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 圆形进度条
                CircularProgressIndicator(
                    progress = task.progress,
                    status = task.status,
                    modifier = Modifier.size(40.dp)
                )

                // 控制按钮
                IconButton(
                    onClick = {
                        when (task.status) {
                            DownloadStatus.DOWNLOADING -> onPause()
                            DownloadStatus.PAUSED -> onResume()
                            DownloadStatus.FAILED -> onResume()
                            else -> {}
                        }
                    },
                    modifier = Modifier.size(32.dp),
                    enabled = task.status != DownloadStatus.COMPLETED
                ) {
                    val icon = when (task.status) {
                        DownloadStatus.DOWNLOADING -> Icons.Default.Pause
                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> Icons.Default.Refresh
                        else -> Icons.Default.Pause
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = when (task.status) {
                            DownloadStatus.DOWNLOADING -> "暂停"
                            DownloadStatus.PAUSED -> "继续"
                            DownloadStatus.FAILED -> "重试"
                            else -> ""
                        },
                        tint = if (task.status == DownloadStatus.COMPLETED) Color.Gray else MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CircularProgressIndicator(
    progress: Float,
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val progressColor = when (status) {
            DownloadStatus.DOWNLOADING -> MaterialTheme.colors.primary
            DownloadStatus.PAUSED -> Color(0xFFE2865A)
            DownloadStatus.COMPLETED -> Color.Green
            DownloadStatus.FAILED -> Color.Red
        }

        Canvas(modifier = Modifier.size(40.dp)) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // 背景圆圈
            drawCircle(
                color = Color.LightGray,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // 进度圆弧
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 进度文字
        Text(
            text = if (status == DownloadStatus.COMPLETED) "✓" else "${(progress * 100).toInt()}%",
            fontSize = 10.sp,
            color = progressColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AvailableDownloadItem(
    url: String,
    filename: String,
    onStartDownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable { onStartDownload() }, // 点击开始下载
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 文件名和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filename,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = startDownload,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "移除",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
