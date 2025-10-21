package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.platform.g_desktop
import com.treevalue.beself.platform.getPlatformName

@Composable
fun OtherFunctionPage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    val showSystemSettings = remember { mutableStateOf(false) }
    val showVideoDialog = remember { mutableStateOf(false) }
    val showCalculator = remember { mutableStateOf(false) }
    val showSchedule = remember { mutableStateOf(false) }
    val videoEnabled = backend?.featureSettings?.value?.videoEnabled ?: false
    val remainingTime = backend?.getRemainingVideoTimeToday() ?: 0L
    val notIsDeskTop = getPlatformName() != g_desktop

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked, modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Pages.FunctionPage.Back.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Pages.FunctionPage.OtherFunctions.getLang(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (notIsDeskTop) 3 else 6),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = PaddingValues(3.dp)
            ) {
                item {
                    FunctionItem(
                        icon = Icons.Default.Settings,
                        title = Pages.SystemSettingsPage.SystemSettings.getLang(),
                        onClick = { showSystemSettings.value = true }
                    )
                }
                if (notIsDeskTop) {
                    item {
                        FunctionItem(
                            icon = Icons.Default.PlayArrow,
                            title = if (videoEnabled) Pages.OtherFunctionsPage.TurnOffVideo.getLang() else Pages.OtherFunctionsPage.TemporarilyEnableVideo.getLang(),
                            onClick = {
                                if (backend != null) {
                                    showVideoDialog.value = true
                                }
                            }
                        )
                    }
                }

                item {
                    FunctionItem(
                        icon = Icons.Default.Calculate,
                        title = Pages.OtherFunctionsPage.Calculator.getLang(),
                        onClick = { showCalculator.value = true }
                    )
                }

                item {
                    FunctionItem(
                        icon = Icons.Default.CalendarToday,
                        title = Pages.SchedulePage.ScheduleManagement.getLang(),
                        onClick = { showSchedule.value = true }
                    )
                }
            }
        }
        if (showSystemSettings.value) {
            SystemSettingsPage(
                onBackClicked = { showSystemSettings.value = false }
            )
        }

        // 计算器全屏覆盖
        if (showCalculator.value) {
            CalculatorPage(
                onBackClicked = { showCalculator.value = false },
            )
        }

        // 日程管理全屏覆盖
        if (showSchedule.value) {
            SchedulePage(
                onBackClicked = { showSchedule.value = false }
            )
        }
    }

    if (notIsDeskTop && showVideoDialog.value && backend != null) {
        AlertDialog(onDismissRequest = { showVideoDialog.value = false }, title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = Pages.OtherFunctionsPage.Video.getLang(),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Pages.OtherFunctionsPage.VideoPlaybackFunction.getLang())
            }
        }, text = {
            Column {
                Text("要${if (videoEnabled) Pages.OtherFunctionsPage.TurnOff.getLang() else Pages.OtherFunctionsPage.Enable.getLang()}视频播放功能吗")

                if (!videoEnabled && remainingTime > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Pages.OtherFunctionsPage.RemainingTimeToday.getLang()}：${
                            backend.formatTime(
                                remainingTime
                            )
                        }",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.primary
                    )
                } else if (!videoEnabled && remainingTime <= 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Pages.OtherFunctionsPage.MustStop.getLang(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }, confirmButton = {
            TextButton(
                onClick = {
                    if (videoEnabled || remainingTime > 0) {
                        backend.switchVideoEnable()
                    }
                    showVideoDialog.value = false
                }, enabled = videoEnabled || remainingTime > 0, shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (videoEnabled) Pages.OtherFunctionsPage.TurnOff.getLang() else Pages.OtherFunctionsPage.Enable.getLang(),
                    color = if (videoEnabled || remainingTime > 0) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            }
        }, dismissButton = {
            TextButton(onClick = { showVideoDialog.value = false }) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        }, shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FunctionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colors.surface
    val iconTintColor = MaterialTheme.colors.primary.copy(alpha = 0.6f)
    val textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            elevation = 1.dp,
            backgroundColor = backgroundColor,
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTintColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 8.sp,
            fontWeight = FontWeight.Normal,
            color = textColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
