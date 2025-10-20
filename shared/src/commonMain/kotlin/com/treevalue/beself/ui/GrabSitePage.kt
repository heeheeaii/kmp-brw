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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.net.GrabbedSite
import com.treevalue.beself.net.WebsiteCrawlManager
import com.treevalue.beself.net.openDownloadDirectory
import com.treevalue.beself.platform.g_desktop
import com.treevalue.beself.platform.getPlatformName
import com.treevalue.beself.values.urlDefaultPrefix
import com.treevalue.beself.net.getHostnameFromUrl
import com.treevalue.beself.net.isValidUrlOrDomain
import kotlinx.coroutines.launch


object WebsiteCrawlManagerSingleton {
    val instance: WebsiteCrawlManager by lazy {
        WebsiteCrawlManager()
    }
}

@Composable
fun GrabSitePage(onBackClicked: () -> Unit) {
    var url by remember { mutableStateOf(urlDefaultPrefix) }
    var isGrabbing by remember { mutableStateOf(false) }
    var grabbedSites by remember { mutableStateOf<List<GrabbedSite>>(emptyList()) }
    var canCancel by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var allSelected by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var downloadUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val crawler = WebsiteCrawlManagerSingleton.instance

    suspend fun performCrawl(targetUrl: String) {
        isGrabbing = true
        grabbedSites = emptyList()

        try {
            crawler.crawlWebsite(targetUrl).collect { result ->
                if (result.success) {
                    // 按域名分组URL
                    val groupedUrls = result.urls.groupBy { getHostnameFromUrl(it) }

                    grabbedSites = groupedUrls.map { (domain, urls) ->
                        GrabbedSite(domain, urls)
                    }
                } else {
                    toastMessage = "抓取失败: ${result.error}"
                }
                isGrabbing = false
            }
        } catch (e: Exception) {
            toastMessage = "抓取出错: ${e.message}"
            isGrabbing = false
        }
    }

    // 实时验证输入
    LaunchedEffect(url) {
        validationError = if (url.isNotBlank() && !isValidUrlOrDomain(url.trim())) {
            "请输入有效的网址或域名格式"
        } else {
            null
        }
    }

    // 更新全选状态（只影响域名选择）
    LaunchedEffect(grabbedSites) {
        allSelected = grabbedSites.isNotEmpty() && grabbedSites.all { it.isSelected }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp)
        ) {
            // 顶部返回按钮和标题 - 保持不变
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
                        contentDescription = "返回",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "抓取网站",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }

            // 输入框和按钮 - 保持不变
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
                                focusedIndicatorColor = if (validationError == null) MaterialTheme.colors.primary else Color.Red,
                                unfocusedIndicatorColor = if (validationError == null)
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                                else
                                    Color.Red.copy(alpha = 0.7f)
                            ),
                            singleLine = true
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
                                if (isGrabbing && canCancel) {
                                    isGrabbing = false
                                    grabbedSites = emptyList()
                                } else if (url.isNotBlank() && validationError == null) {
                                    isGrabbing = true
                                    canCancel = true
                                    grabbedSites = emptyList()

                                    kotlinx.coroutines.GlobalScope.launch {
                                        performCrawl(url.trim())
                                    }
                                }
                            },
                            enabled = url.isNotBlank() && validationError == null || isGrabbing,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isGrabbing) Color.Red else MaterialTheme.colors.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isGrabbing) {
                                if (canCancel) {
                                    Text("取消", color = Color.White)
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                Text("抓取")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (validationError != null) {
                        Text(
                            text = "❌ $validationError",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    } else {
                        Text(
                            text = "💡 输入完整的网址或域名开始抓取",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 下载功能区域
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = downloadUrl,
                            onValueChange = {
                                downloadUrl = it
                                downloadError = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                focusedIndicatorColor = MaterialTheme.colors.primary,
                                unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )

                        if (downloadUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { downloadUrl = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 下载按钮
                        IconButton(
                            onClick = {
                                if (downloadUrl.isNotBlank() && !isDownloading) {
                                    // 调用下载函数
                                    kotlinx.coroutines.GlobalScope.launch {
                                        crawler.downloadFile(downloadUrl.trim()) { progress, error ->
                                            downloadProgress = progress
                                            downloadError = error
                                            if (progress >= 1f || error != null) {
                                                isDownloading = false
                                                if (error == null) {
                                                    toastMessage = "下载完成"
                                                }
                                            }
                                        }
                                    }
                                    isDownloading = true
                                }
                            },
                            enabled = downloadUrl.isNotBlank() && !isDownloading,
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "下载",
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 打开下载目录按钮 - 改为图标按钮
                        IconButton(
                            onClick = {
                                openDownloadDirectory()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "打开下载目录",
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    val helpText =
                        if (!isDownloading && downloadUrl.isNotBlank() && downloadError == null) "💡 点击下载按钮开始下载文件" else "💡 输入完整的文件地址开始下载"
                    Text(
                        text = helpText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 下载进度显示
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "下载进度",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }

                    // 下载错误显示
                    downloadError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❌ 下载失败: $error",
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }

                }
            }

            // 抓取状态 - 保持不变
            if (isGrabbing) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colors.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在抓取网站信息...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 抓取结果
            if (grabbedSites.isNotEmpty()) {
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
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                grabbedSites = grabbedSites.map { site ->
                                    site.copy(isSelected = checked)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Green
                            )
                        )

                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "成功",
                            tint = Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "抓取成功！共找到 ${grabbedSites.size} 个网站",
                            fontSize = 14.sp,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        // 修复全量复制逻辑
                        IconButton(
                            onClick = {
                                val selectedItems = mutableListOf<String>()

                                // 添加选中的域名
                                grabbedSites.filter { it.isSelected }.forEach { site ->
                                    selectedItems.add(site.domain)
                                }

                                // 添加选中的URL
                                grabbedSites.forEach { site ->
                                    selectedItems.addAll(site.selectedUrls)
                                }

                                if (selectedItems.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(selectedItems.joinToString(",")))
                                    toastMessage = "已复制 ${selectedItems.size} 个项目到剪贴板"
                                }
                            },
                            enabled = grabbedSites.any { it.isSelected } || grabbedSites.any { it.selectedUrls.isNotEmpty() }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制全部",
                                tint = if (grabbedSites.any { it.isSelected } || grabbedSites.any { it.selectedUrls.isNotEmpty() })
                                    Color.Green else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(grabbedSites) { site ->
                        GrabbedSiteItemEnhanced(
                            site = site,
                            onSiteSelectionChanged = { selected ->
                                grabbedSites = grabbedSites.map {
                                    if (it.domain == site.domain) it.copy(isSelected = selected) else it
                                }
                            },
                            onUrlSelectionChanged = { url, selected ->
                                // 修复URL选择状态更新
                                grabbedSites = grabbedSites.map { grabbedSite ->
                                    if (grabbedSite.domain == site.domain) {
                                        val newSelectedUrls = if (selected) {
                                            grabbedSite.selectedUrls + url
                                        } else {
                                            grabbedSite.selectedUrls - url
                                        }
                                        grabbedSite.copy(selectedUrls = newSelectedUrls)
                                    } else {
                                        grabbedSite
                                    }
                                }
                            },
                            onExpandChanged = { expanded ->
                                grabbedSites = grabbedSites.map {
                                    if (it.domain == site.domain) it.copy(isExpanded = expanded) else it
                                }
                            },
                            clipboardManager = clipboardManager,
                            onShowToast = { message ->
                                toastMessage = message
                            }
                        )
                    }
                }
            }
        }

        // 显示 Toast 消息
        toastMessage?.let { message ->
            toast(
                message = message,
                onDismiss = { toastMessage = null }
            )
        }
    }
}

@Composable
fun GrabbedSiteItemEnhanced(
    site: GrabbedSite,
    onSiteSelectionChanged: (Boolean) -> Unit,
    onUrlSelectionChanged: (String, Boolean) -> Unit,
    onExpandChanged: (Boolean) -> Unit,
    clipboardManager: ClipboardManager,
    onShowToast: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 主要域名行
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 域名选择复选框
                Checkbox(
                    checked = site.isSelected,
                    onCheckedChange = onSiteSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    )
                )

                if (getPlatformName() == g_desktop) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // 状态指示器
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 网站图标占位
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = site.domain.take(1).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = site.domain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${site.urls.size} 个链接${if (site.selectedUrls.isNotEmpty()) ", ${site.selectedUrls.size} 个URL已选中" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                // 复制域名按钮
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(site.domain))
                        onShowToast("已复制域名: ${site.domain}")
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制域名",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 展开/收起按钮
                IconButton(
                    onClick = { onExpandChanged(!site.isExpanded) }
                ) {
                    Icon(
                        if (site.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (site.isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 展开的URL列表
            if (site.isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    site.urls.forEach { url ->
                        UrlItem(
                            url = url,
                            isSelected = site.selectedUrls.contains(url),
                            onSelectionChanged = { selected ->
                                onUrlSelectionChanged(url, selected)
                            },
                            clipboardManager = clipboardManager,
                            onShowToast = onShowToast
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UrlItem(
    url: String,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    clipboardManager: ClipboardManager,
    onShowToast: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // URL选择复选框（独立于域名选择）
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.secondary
            ),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = url,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 复制URL按钮
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(url))
                onShowToast("已复制URL: $url")
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "复制URL",
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
