package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.values.urlDefaultPrefix
import com.treevalue.beself.values.urlFormatInvalid
import com.treevalue.beself.net.isValidUrl
import kotlinx.coroutines.launch

@Composable
fun AddSitePage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (backend == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "错误：无法访问后端服务",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBackClicked,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("返回")
                }
            }
            return@Scaffold
        }

        // 网站添加相关状态
        val labelValue = remember { mutableStateOf("") }
        val urlValue = remember { mutableStateOf(urlDefaultPrefix) }
        val canAddUrl = remember { mutableStateOf(true) }
        val urlRestrictionMessage = remember { mutableStateOf("") }
        val isCheckingUrl = remember { mutableStateOf(false) }
        val canAddName = remember { mutableStateOf(true) }
        val nameRestrictionMessage = remember { mutableStateOf("") }
        val isCheckingName = remember { mutableStateOf(false) }

        // 正则式相关状态
        val regexValue = remember { mutableStateOf("") }
        val regexErrorMessage = remember { mutableStateOf("") }
        val showRegexHelp = remember { mutableStateOf(false) }

        // 管理区域状态
        val showManagement = remember { mutableStateOf(false) }
        val selectedRegexPatterns = remember { mutableStateOf(setOf<String>()) }
        val selectedSiteIds = remember { mutableStateOf(setOf<String>()) }

        // 确认对话框状态
        val showConfirmDialog = remember { mutableStateOf(false) }
        val confirmDialogData = remember { mutableStateOf<ConfirmDialogData?>(null) }

        // 网站名称检查逻辑
        LaunchedEffect(labelValue.value) {
            if (labelValue.value.isNotBlank()) {
                isCheckingName.value = true
                try {
                    val isDuplicate = backend.isNameDuplicate(labelValue.value)
                    if (isDuplicate) {
                        canAddName.value = false
                        nameRestrictionMessage.value = "该显示名称已存在，请使用其他名称"
                    } else {
                        canAddName.value = true
                        nameRestrictionMessage.value = ""
                    }
                } catch (e: Exception) {
                    canAddName.value = true
                    nameRestrictionMessage.value = ""
                } finally {
                    isCheckingName.value = false
                }
            } else {
                canAddName.value = true
                nameRestrictionMessage.value = ""
                isCheckingName.value = false
            }
        }

        // 网站URL检查逻辑
        LaunchedEffect(urlValue.value) {
            if (urlValue.value.isNotBlank()) {
                isCheckingUrl.value = true
                try {
                    val isValidFormat = isValidUrl(urlValue.value)
                    if (!isValidFormat) {
                        canAddUrl.value = false
                        urlRestrictionMessage.value = urlFormatInvalid
                        return@LaunchedEffect
                    }
                    val (canAddResult, message) = backend.canAddSiteWithDetails(urlValue.value)
                    canAddUrl.value = canAddResult
                    urlRestrictionMessage.value = message
                } catch (e: Exception) {
                    canAddUrl.value = true
                    urlRestrictionMessage.value = ""
                } finally {
                    isCheckingUrl.value = false
                }
            } else {
                canAddUrl.value = true
                urlRestrictionMessage.value = ""
                isCheckingUrl.value = false
            }
        }

        // 正则式验证
        LaunchedEffect(regexValue.value) {
            if (regexValue.value.isNotBlank()) {
                try {
                    Regex(regexValue.value, RegexOption.IGNORE_CASE)
                    regexErrorMessage.value = ""
                } catch (e: Exception) {
                    regexErrorMessage.value = "正则表达式语法错误: ${e.message}"
                }
            } else {
                regexErrorMessage.value = ""
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    text = "添加网站/正则式",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "网站",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "添加网站",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            // 显示名称输入
                            Column {
                                Text(
                                    text = "显示名称",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                TextField(
                                    value = labelValue.value,
                                    onValueChange = { labelValue.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.textFieldColors(
                                        backgroundColor = MaterialTheme.colors.background,
                                        focusedIndicatorColor = if (canAddName.value) MaterialTheme.colors.primary else Color.Red,
                                        unfocusedIndicatorColor = if (canAddName.value) MaterialTheme.colors.onSurface.copy(
                                            alpha = 0.3f
                                        ) else Color.Red,
                                        textColor = MaterialTheme.colors.onSurface
                                    ),
                                    singleLine = true,
                                    isError = !canAddName.value && labelValue.value.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (!canAddName.value && nameRestrictionMessage.value.isNotEmpty()) {
                                    Text(
                                        text = "⚠️ ${nameRestrictionMessage.value}",
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (isCheckingName.value) {
                                    Text(
                                        text = "检查名称中...",
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // URL输入
                            Column {
                                Text(
                                    text = "网站地址",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                TextField(
                                    value = urlValue.value,
                                    onValueChange = { urlValue.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.textFieldColors(
                                        backgroundColor = MaterialTheme.colors.background,
                                        focusedIndicatorColor = if (canAddUrl.value) MaterialTheme.colors.primary else Color.Red,
                                        unfocusedIndicatorColor = if (canAddUrl.value) MaterialTheme.colors.onSurface.copy(
                                            alpha = 0.3f
                                        ) else Color.Red,
                                        textColor = MaterialTheme.colors.onSurface
                                    ),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            "https://example.com",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    },
                                    isError = !canAddUrl.value && urlValue.value.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (!canAddUrl.value && urlRestrictionMessage.value.isNotEmpty()) {
                                    Text(
                                        text = "⚠️ ${urlRestrictionMessage.value}",
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (isCheckingUrl.value) {
                                    Text(
                                        text = "检查网站限制中...",
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (labelValue.value.isNotBlank() &&
                                        urlValue.value.isNotBlank() &&
                                        canAddName.value &&
                                        canAddUrl.value &&
                                        !isCheckingName.value &&
                                        !isCheckingUrl.value
                                    ) {
                                        scope.launch {
                                            try {
                                                val success = backend.addSite(labelValue.value, urlValue.value)
                                                if (success) {
                                                    labelValue.value = ""
                                                    urlValue.value = ""
                                                    snackbarHostState.showSnackbar("网站添加成功")
                                                } else {
                                                    snackbarHostState.showSnackbar("添加网站失败")
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("添加网站失败，请重试")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (canAddName.value && canAddUrl.value) MaterialTheme.colors.primary else Color.Gray,
                                    contentColor = if (canAddName.value && canAddUrl.value) MaterialTheme.colors.onPrimary else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = labelValue.value.isNotBlank() &&
                                        urlValue.value.isNotBlank() &&
                                        canAddName.value &&
                                        canAddUrl.value &&
                                        !isCheckingName.value &&
                                        !isCheckingUrl.value
                            ) {
                                Text(
                                    text = if (isCheckingName.value || isCheckingUrl.value) "检查中..." else "添加网站",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "正则式",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "添加正则式",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 正则式输入
                            Column {
                                Text(
                                    text = "正则表达式",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                TextField(
                                    value = regexValue.value,
                                    onValueChange = { regexValue.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.textFieldColors(
                                        backgroundColor = MaterialTheme.colors.background,
                                        focusedIndicatorColor = if (regexErrorMessage.value.isEmpty()) MaterialTheme.colors.primary else Color.Red,
                                        unfocusedIndicatorColor = if (regexErrorMessage.value.isEmpty()) MaterialTheme.colors.onSurface.copy(
                                            alpha = 0.3f
                                        ) else Color.Red,
                                        textColor = MaterialTheme.colors.onSurface
                                    ),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            ".*://.*example\\.com.*",
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    },
                                    isError = regexErrorMessage.value.isNotEmpty(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (regexErrorMessage.value.isNotEmpty()) {
                                    Text(
                                        text = "⚠️ ${regexErrorMessage.value}",
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    // 帮助按钮
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showRegexHelp.value = !showRegexHelp.value },
                                            modifier = Modifier.size(20.dp).offset(y = 2.5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = "正则式帮助",
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = if (showRegexHelp.value) "点击收起帮助" else "查看正则式示例",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    // 详细帮助信息（可展开）
                                    if (showRegexHelp.value) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            elevation = 2.dp,
                                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .height(300.dp)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "正则表达式示例：",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colors.onSurface
                                                )

                                                RegexExampleInline(
                                                    title = "匹配特定域名",
                                                    regex = ".*://.*example\\.com.*",
                                                    description = "匹配 example.com 域名下的所有URL",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = "匹配所有子域名",
                                                    regex = ".*://.*\\.github\\.com.*",
                                                    description = "匹配 *.github.com 的所有子域名",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = "匹配本地网络",
                                                    regex = ".*://.*192\\.168\\..*",
                                                    description = "匹配 192.168.x.x 网段的所有地址",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = "匹配HTTPS协议",
                                                    regex = "^https://.*",
                                                    description = "只匹配HTTPS协议的网址",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = "匹配特定端口",
                                                    regex = ".*://.*:8080.*",
                                                    description = "匹配8080端口的所有网址",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = "匹配文件类型",
                                                    regex = ".*\\.(jpg|png|gif|pdf)$",
                                                    description = "匹配特定文件扩展名",
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                Card(
                                                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "⚠️ 注意：正则式大小写不敏感，添加前请仔细测试！",
                                                        modifier = Modifier.padding(8.dp),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colors.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 添加正则式按钮
                            Button(
                                onClick = {
                                    if (regexValue.value.isNotBlank() && regexErrorMessage.value.isEmpty()) {
                                        scope.launch {
                                            try {
                                                val success = backend.addCustomRegexPattern(regexValue.value)
                                                if (success) {
                                                    regexValue.value = ""
                                                    snackbarHostState.showSnackbar("正则式添加成功")
                                                } else {
                                                    snackbarHostState.showSnackbar("正则式添加失败")
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("正则式添加失败，请重试")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (regexErrorMessage.value.isEmpty()) MaterialTheme.colors.primary else Color.Gray,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = regexValue.value.isNotBlank() && regexErrorMessage.value.isEmpty()
                            ) {
                                Text(
                                    text = "添加正则式",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // === 管理区域 ===
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // 管理区域标题和展开按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (showManagement.value) 16.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "管理",
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "管理已添加项目",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }

                            IconButton(
                                onClick = { showManagement.value = !showManagement.value },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showManagement.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showManagement.value) "收起" else "展开",
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 管理内容（可展开）
                        if (showManagement.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                // 正则式管理
                                val customRegexPatterns = backend.customRegexPatterns
                                if (customRegexPatterns.isNotEmpty()) {
                                    Column {
                                        // 正则式标题和全选/全删操作
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "自定义正则式 (${customRegexPatterns.size})",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.onSurface
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // 全选/全不选框
                                                val allRegexSelected = customRegexPatterns.all {
                                                    selectedRegexPatterns.value.contains(it)
                                                }
                                                Checkbox(
                                                    checked = allRegexSelected && customRegexPatterns.isNotEmpty(),
                                                    onCheckedChange = { checked ->
                                                        selectedRegexPatterns.value = if (checked) {
                                                            customRegexPatterns.toSet()
                                                        } else {
                                                            emptySet()
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colors.primary
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // 全量删除按钮
                                                IconButton(
                                                    onClick = {
                                                        if (selectedRegexPatterns.value.isNotEmpty()) {
                                                            confirmDialogData.value = ConfirmDialogData(
                                                                title = "删除正则式",
                                                                message = "确定要删除选中的 ${selectedRegexPatterns.value.size} 个正则式吗？此操作不可撤销。",
                                                                onConfirm = {
                                                                    scope.launch {
                                                                        selectedRegexPatterns.value.forEach { pattern ->
                                                                            backend.removeCustomRegexPattern(pattern)
                                                                        }
                                                                        selectedRegexPatterns.value = emptySet()
                                                                        snackbarHostState.showSnackbar("已删除选中的正则式")
                                                                    }
                                                                }
                                                            )
                                                            showConfirmDialog.value = true
                                                        }
                                                    },
                                                    enabled = selectedRegexPatterns.value.isNotEmpty(),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除选中项",
                                                        tint = if (selectedRegexPatterns.value.isNotEmpty())
                                                            MaterialTheme.colors.primary else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 正则式列表
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            customRegexPatterns.forEach { pattern ->
                                                RegexManagementItem(
                                                    pattern = pattern,
                                                    isSelected = selectedRegexPatterns.value.contains(pattern),
                                                    onSelectionChanged = { isSelected ->
                                                        selectedRegexPatterns.value = if (isSelected) {
                                                            selectedRegexPatterns.value + pattern
                                                        } else {
                                                            selectedRegexPatterns.value - pattern
                                                        }
                                                    },
                                                    onDelete = {
                                                        confirmDialogData.value = ConfirmDialogData(
                                                            title = "删除正则式",
                                                            message = "确定要删除正则式 \"$pattern\" 吗？此操作不可撤销。",
                                                            onConfirm = {
                                                                scope.launch {
                                                                    backend.removeCustomRegexPattern(pattern)
                                                                    selectedRegexPatterns.value =
                                                                        selectedRegexPatterns.value - pattern
                                                                    snackbarHostState.showSnackbar("已删除正则式")
                                                                }
                                                            }
                                                        )
                                                        showConfirmDialog.value = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 动态网站管理
                                val dynamicSites =
                                    backend.dynamicSites.filter { !backend.excludedSiteIds.contains(it.id) }
                                if (dynamicSites.isNotEmpty()) {
                                    Column {
                                        // 动态网站标题和全选/全删操作
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "已添加网站 (${dynamicSites.size})",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colors.onSurface
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // 全选/全不选框
                                                val allSitesSelected = dynamicSites.all { site ->
                                                    selectedSiteIds.value.contains(site.id)
                                                }
                                                Checkbox(
                                                    checked = allSitesSelected && dynamicSites.isNotEmpty(),
                                                    onCheckedChange = { checked ->
                                                        selectedSiteIds.value = if (checked) {
                                                            dynamicSites.map { it.id }.toSet()
                                                        } else {
                                                            emptySet()
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colors.primary
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // 全量删除按钮
                                                IconButton(
                                                    onClick = {
                                                        if (selectedSiteIds.value.isNotEmpty()) {
                                                            val selectedSites = dynamicSites.filter {
                                                                selectedSiteIds.value.contains(it.id)
                                                            }
                                                            confirmDialogData.value = ConfirmDialogData(
                                                                title = "删除网站",
                                                                message = "确定要删除选中的 ${selectedSiteIds.value.size} 个网站吗？\n\n⚠️ 警告：删除后可能会有时间限制无法再添加这些网站！",
                                                                onConfirm = {
                                                                    scope.launch {
                                                                        selectedSites.forEach { site ->
                                                                            backend.requestDeleteSite(site)
                                                                        }
                                                                        selectedSiteIds.value = emptySet()
                                                                        snackbarHostState.showSnackbar("已删除选中的网站")
                                                                    }
                                                                }
                                                            )
                                                            showConfirmDialog.value = true
                                                        }
                                                    },
                                                    enabled = selectedSiteIds.value.isNotEmpty(),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除选中项",
                                                        tint = if (selectedSiteIds.value.isNotEmpty())
                                                            MaterialTheme.colors.primary else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 动态网站列表
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            dynamicSites.forEach { site ->
                                                SiteManagementItem(
                                                    site = site,
                                                    isSelected = selectedSiteIds.value.contains(site.id),
                                                    onSelectionChanged = { isSelected ->
                                                        selectedSiteIds.value = if (isSelected) {
                                                            selectedSiteIds.value + site.id
                                                        } else {
                                                            selectedSiteIds.value - site.id
                                                        }
                                                    },
                                                    onDelete = {
                                                        confirmDialogData.value = ConfirmDialogData(
                                                            title = "删除网站",
                                                            message = "确定要删除网站 \"${site.label}\" 吗？\n\n⚠️ 警告：删除后可能会有时间限制无法再添加此网站！",
                                                            onConfirm = {
                                                                scope.launch {
                                                                    backend.requestDeleteSite(site)
                                                                    selectedSiteIds.value =
                                                                        selectedSiteIds.value - site.id
                                                                    snackbarHostState.showSnackbar("已删除网站")
                                                                }
                                                            }
                                                        )
                                                        showConfirmDialog.value = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 如果没有任何项目可管理
                                if (customRegexPatterns.isEmpty() && dynamicSites.isEmpty()) {
                                    Text(
                                        text = "暂无项目可管理",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 确认对话框
        if (showConfirmDialog.value && confirmDialogData.value != null) {
            ConfirmDialog(
                title = confirmDialogData.value!!.title,
                message = confirmDialogData.value!!.message,
                onConfirm = {
                    confirmDialogData.value!!.onConfirm()
                    showConfirmDialog.value = false
                    confirmDialogData.value = null
                },
                onDismiss = {
                    showConfirmDialog.value = false
                    confirmDialogData.value = null
                }
            )
        }
    }
}

@Composable
fun RegexExampleInline(
    title: String,
    regex: String,
    description: String,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 1.dp,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = regex,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(regex))
                        scope.launch {
                            snackbarHostState.showSnackbar("已复制到剪切板")
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RegexManagementItem(
    pattern: String,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        backgroundColor = MaterialTheme.colors.background,
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = pattern,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SiteManagementItem(
    site: com.treevalue.beself.net.SiteInfo,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        backgroundColor = MaterialTheme.colors.background,
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = site.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = site.host,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    if (site.status != com.treevalue.beself.net.SiteStatus.COMPLETED) {
                        Text(
                            text = "状态: ${site.status}",
                            fontSize = 11.sp,
                            color = if (site.status == com.treevalue.beself.net.SiteStatus.FAILED)
                                Color.Red else MaterialTheme.colors.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.surface,
                            contentColor = MaterialTheme.colors.onSurface
                        ),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("确认删除")
                    }
                }
            }
        }
    }
}

data class ConfirmDialogData(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
)
