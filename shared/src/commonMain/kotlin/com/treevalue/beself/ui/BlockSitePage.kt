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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.getHostnameFromUrl
import com.treevalue.beself.net.isValidDomain
import com.treevalue.beself.net.isValidUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BlockSitePage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    var blockedUrls by remember { mutableStateOf("") }
    var isBlocking by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showFailDialog by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var countdown by remember { mutableStateOf(10) }
    var countdownActive by remember { mutableStateOf(false) }
    var matchedBlockedSites by remember { mutableStateOf<List<SiteInfo>>(emptyList()) }
    var showDetailedHelp by remember { mutableStateOf(false) }
    val pageScope = rememberCoroutineScope()

    // 检查输入的URL/域名是否匹配已有网站
    fun checkMatchedSites(input: String): List<SiteInfo> {
        if (input.isBlank() || backend == null) return emptyList()

        val allSites = backend.getAllSitesIncludeHidden()
        val items = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return items.flatMap { item ->
            allSites.filter { site ->
                when {
                    // 如果输入的是URL，提取hostname进行匹配
                    isValidUrl(item) -> {
                        val inputHostname = getHostnameFromUrl(item)
                        site.host.equals(inputHostname, ignoreCase = true)
                    }
                    // 如果输入的是hostname/域名，直接匹配
                    else -> {
                        site.host.equals(item, ignoreCase = true)
                    }
                }
            }
        }.distinctBy { it.id }
    }

    // 验证输入的URL/域名
    fun validateInputs(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        val errors = mutableListOf<String>()
        val items = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        items.forEachIndexed { index, item ->
            val isValid = when {
                // 检查是否是有效URL
                item.contains("://") -> isValidUrl(item)
                // 检查是否是有效域名/hostname
                else -> isValidDomain(item)
            }

            if (!isValid) {
                errors.add("${Pages.BlockSitePage.Item.getLang()}${index + 1}${Pages.BlockSitePage.ItemCount.getLang()} \"$item\" ${Pages.BlockSitePage.InvalidFormat.getLang()}")
            }
        }

        return errors
    }

    // 实时验证
    LaunchedEffect(blockedUrls) {
        validationErrors = validateInputs(blockedUrls)
        matchedBlockedSites = checkMatchedSites(blockedUrls)
    }

    // 倒计时处理
    LaunchedEffect(countdownActive) {
        if (countdownActive) {
            while (countdown > 0 && countdownActive) {
                delay(1000)
                countdown--
            }
        }
    }

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
                text = Pages.BlockSitePage.BlockDomainOrURL.getLang(),
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
                        value = blockedUrls,
                        onValueChange = { blockedUrls = it },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = MaterialTheme.colors.surface,
                            focusedIndicatorColor = if (validationErrors.isEmpty()) MaterialTheme.colors.primary else Color.Red,
                            unfocusedIndicatorColor = if (validationErrors.isEmpty())
                                MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            else
                                Color.Red.copy(alpha = 0.7f)
                        ),
                        singleLine = true
                    )

                    if (blockedUrls.isNotEmpty()) {
                        IconButton(
                            onClick = { blockedUrls = "" }
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
                            if (blockedUrls.isNotBlank() && validationErrors.isEmpty()) {
                                showConfirmDialog = true
                                countdown = 10
                                countdownActive = true
                            }
                        },
                        enabled = blockedUrls.isNotBlank() && validationErrors.isEmpty() && !isBlocking,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isBlocking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(Pages.BlockSitePage.Block.getLang())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 错误提示
                if (validationErrors.isNotEmpty()) {
                    Column {
                        validationErrors.forEach { error ->
                            Text(
                                text = "❌ $error",
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    // 简化的提示信息
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 问号提示按钮
                        IconButton(
                            onClick = { showDetailedHelp = !showDetailedHelp },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = Pages.BlockSitePage.Details.getLang(),
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 详细帮助信息（可展开）
                    if (showDetailedHelp) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            elevation = 2.dp,
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = Pages.BlockSitePage.BlockingRules.getLang(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Pages.BlockSitePage.SupportsDomainsAndURLs.getLang(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Pages.BlockSitePage.DomainBlockingDesc.getLang(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Pages.BlockSitePage.URLBlockingDesc.getLang(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 匹配的网站显示
        if (matchedBlockedSites.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${Pages.BlockSitePage.SitesToBeBlocked.getLang()} (${matchedBlockedSites.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    matchedBlockedSites.forEach { site ->
                        BlockedSiteItem(site = site)
                    }
                }
            }
        }
    }

    // 确认对话框（带倒计时）
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                countdownActive = false
                countdown = 10
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = Pages.BlockSitePage.Warning.getLang(),
                        tint = Color.Yellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Pages.BlockSitePage.ConfirmBlock.getLang(),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = Pages.BlockSitePage.BlockWarning.getLang(),
                        fontSize = 14.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(Pages.BlockSitePage.ConfirmBlockQuestion.getLang())

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 2.dp,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = blockedUrls.replace(",", "\n"),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        countdownActive = false
                        isBlocking = true
                        pageScope.launch {
                            if (backend == null) {
                                return@launch
                            }
                            try {
                                val items = blockedUrls.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                backend.blockSites(items)
                                isBlocking = false
                                showSuccessDialog = true
                                blockedUrls = ""
                            } catch (e: Exception) {
                                isBlocking = false
                                showFailDialog = true
                            }
                        }
                    },
                    enabled = countdown <= 0,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (countdown <= 0) Color.Red else Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (countdown > 0) {
                        Text("${Pages.BlockSitePage.ConfirmBlock.getLang()} (${countdown}s)")
                    } else {
                        Text(Pages.BlockSitePage.ConfirmBlock.getLang())
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        countdownActive = false
                        countdown = 10
                    }
                ) {
                    Text(Pages.AddSitePage.Cancel.getLang())
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 成功对话框
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = Pages.BlockSitePage.Success.getLang(),
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Pages.BlockSitePage.BlockedSuccessfully.getLang())
                }
            },
            text = { Text(Pages.BlockSitePage.DomainURLBlocked.getLang()) },
            confirmButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text(Pages.BlockSitePage.OK.getLang())
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 失败对话框
    if (showFailDialog) {
        AlertDialog(
            onDismissRequest = { showFailDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = Pages.BlockSitePage.Failed.getLang(),
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Pages.BlockSitePage.BlockFailed.getLang())
                }
            },
            text = { Text(Pages.BlockSitePage.BlockFailedRetry.getLang()) },
            confirmButton = {
                TextButton(
                    onClick = { showFailDialog = false }
                ) {
                    Text(Pages.BlockSitePage.OK.getLang())
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun BlockedSiteItem(site: SiteInfo) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        elevation = 2.dp,
        backgroundColor = Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 红色状态指示器
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = site.host,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = Pages.BlockSitePage.WillBeBlocked.getLang(),
                fontSize = 12.sp,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
