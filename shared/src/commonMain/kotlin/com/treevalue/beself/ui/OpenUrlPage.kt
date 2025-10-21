package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.values.urlDefaultPrefix
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.net.getHostnameFromUrl
import com.treevalue.beself.net.isFileUrl
import com.treevalue.beself.net.isValidUrlOrDomain
import com.treevalue.beself.util.isFilePath
import com.treevalue.beself.util.validateFilePath
import com.treevalue.beself.util.convertToFileUrl
import com.treevalue.beself.util.getFileDisplayName
import com.treevalue.beself.util.getFileTypeEmoji

@Composable
fun OpenUrlPage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    var url by remember { mutableStateOf(urlDefaultPrefix) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isFilePathInput by remember { mutableStateOf(false) }
    var fileTypeEmoji by remember { mutableStateOf("") }

    // 实时验证输入
    LaunchedEffect(url) {
        val trimmedUrl = url.trim()

        if (trimmedUrl.isBlank()) {
            validationError = null
            permissionError = null
            isFilePathInput = false
            fileTypeEmoji = ""
            return@LaunchedEffect
        }

        // 检查是否为文件路径
        if (isFilePath(trimmedUrl)) {
            isFilePathInput = true
            fileTypeEmoji = getFileTypeEmoji(trimmedUrl)

            // 验证文件路径
            val (isValid, error) = validateFilePath(trimmedUrl)
            if (!isValid) {
                validationError = error
                permissionError = null
            } else {
                validationError = null
                permissionError = null
            }
        } else {
            // 作为 URL 验证
            isFilePathInput = false
            fileTypeEmoji = ""
            validationError = if (!isValidUrlOrDomain(trimmedUrl)) {
                Pages.OpenURLPage.EnterValidURL.getLang()
            } else {
                null
            }
        }

        // 清除权限错误当URL格式无效时
        if (validationError != null) {
            permissionError = null
        }
    }

    // 检查网站权限（仅对URL，不对文件路径）
    LaunchedEffect(url, validationError, isFilePathInput) {
        if (isFilePathInput) {
            // 文件路径不需要检查权限
            permissionError = null
            return@LaunchedEffect
        }

        isChecking = true
        val trimmedUrl = url.trim()
        if (trimmedUrl.isNotBlank() && validationError == null && backend != null) {
            val canOpen = backend.isUrlAllowed(trimmedUrl) || isFileUrl(trimmedUrl)
            permissionError = if (!canOpen) {
                Pages.OpenURLPage.NotAllowedToOpen.getLang()
            } else {
                null
            }
        } else {
            permissionError = null
        }
        isChecking = false
    }

    val canOpen = url.isNotBlank() &&
            validationError == null &&
            permissionError == null &&
            !isChecking

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp)
        ) {
            // 顶部返回按钮和标题
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.size(40.dp)
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
                    text = Pages.FunctionPage.OpenURL.getLang(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }

            // 输入框和按钮
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                focusedIndicatorColor = when {
                                    validationError != null -> Color.Red
                                    permissionError != null -> Color.Red
                                    else -> MaterialTheme.colors.primary
                                },
                                unfocusedIndicatorColor = when {
                                    validationError != null -> Color.Red.copy(alpha = 0.7f)
                                    permissionError != null -> Color.Red.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                }
                            ),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = Pages.OpenURLPage.EnterURLOrPath.getLang(),
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        )

                        if (url.isNotEmpty()) {
                            IconButton(
                                onClick = { url = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (canOpen) {
                                    val trimmedUrl = url.trim()
                                    val finalUrl = if (isFilePathInput) {
                                        convertToFileUrl(trimmedUrl)
                                    } else {
                                        trimmedUrl
                                    }

                                    val tabTitle = if (isFilePathInput) {
                                        getFileDisplayName(trimmedUrl)
                                    } else {
                                        getHostnameFromUrl(trimmedUrl)
                                    }

                                    EventBus.publish(
                                        TabEvent.RequestNewTab(finalUrl, tabTitle)
                                    )
                                    EventBus.publish(PopEvent.HidePop)
                                }
                            },
                            enabled = canOpen,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (canOpen) MaterialTheme.colors.primary else Color.Gray,
                                contentColor = if (canOpen) MaterialTheme.colors.onPrimary else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(Pages.OpenURLPage.Open.getLang())
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 状态提示信息
                    when {
                        validationError != null -> {
                            Text(
                                text = "❌ $validationError",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }

                        permissionError != null -> {
                            Text(
                                text = "🚫 $permissionError",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }

                        isChecking -> {
                            Text(
                                text = "🔍 检查网站权限中...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.primary
                            )
                        }

                        isFilePathInput && canOpen -> {
                            Text(
                                text = "$fileTypeEmoji ${Pages.OpenURLPage.LocalFileDetected.getLang()}",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        else -> {
                            Text(
                                text = Pages.OpenURLPage.SupportsURLsAndFiles.getLang(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // 权限提示卡片
            if (permissionError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.Red.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = Pages.OpenURLPage.SiteAccessRestricted.getLang(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Pages.OpenURLPage.SiteNotAllowed.getLang(),
                            fontSize = 14.sp,
                            color = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 成功状态提示
            if (canOpen) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.Green.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFilePathInput) Icons.Default.InsertDriveFile else Icons.Default.OpenInBrowser,
                            contentDescription = Pages.OpenURLPage.CanOpen.getLang(),
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isFilePathInput) Pages.OpenURLPage.LocalFileVerified.getLang() else Pages.OpenURLPage.Accessible.getLang(),
                                fontSize = 14.sp,
                                color = Color.Green,
                                fontWeight = FontWeight.Medium
                            )
                            if (isFilePathInput) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getFileDisplayName(url),
                                    fontSize = 12.sp,
                                    color = Color.Green.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 文件类型支持说明
            if (isFilePathInput || url.trim().contains(":\\") || url.trim().startsWith("/")) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = Pages.OpenURLPage.SupportedFileFormats.getLang(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Pages.OpenURLPage.SupportedFileFormatsDesc.getLang(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
