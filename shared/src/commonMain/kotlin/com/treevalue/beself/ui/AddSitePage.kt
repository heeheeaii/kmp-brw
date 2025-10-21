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
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.values.urlDefaultPrefix
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
                    text = Pages.AddSitePage.ErrorBackendUnavailable.getLang(),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBackClicked,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Pages.FunctionPage.Back.getLang())
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
                        nameRestrictionMessage.value = Pages.AddSitePage.DisplayNameExists.getLang()
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
                        urlRestrictionMessage.value = Pages.FunctionPage.URLFormatInvalid.getLang()
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
                    regexErrorMessage.value = "${Pages.AddSitePage.RegexSyntaxError.getLang()}: ${e.message}"
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
                        contentDescription = Pages.FunctionPage.Back.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Pages.AddSitePage.AddSiteRegex.getLang(),
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
                                contentDescription = Pages.AddSitePage.Website.getLang(),
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Pages.AddSitePage.AddSite.getLang(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            // 显示名称输入
                            Column {
                                Text(
                                    text = Pages.AddSitePage.DisplayName.getLang(),
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
                                        text = Pages.AddSitePage.CheckingName.getLang(),
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // URL输入
                            Column {
                                Text(
                                    text = Pages.AddSitePage.WebsiteURL.getLang(),
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
                                        text = Pages.AddSitePage.CheckingSiteRestrictions.getLang(),
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
                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.SiteAddedSuccess.getLang())
                                                } else {
                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.SiteAddFailed.getLang())
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(Pages.AddSitePage.SiteAddFailedRetry.getLang())
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
                                    text = if (isCheckingName.value || isCheckingUrl.value) Pages.AddSitePage.Checking.getLang() else Pages.AddSitePage.AddSite.getLang(),
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
                                contentDescription = Pages.AddSitePage.Regex.getLang(),
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Pages.AddSitePage.AddRegex.getLang(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 正则式输入
                            Column {
                                Text(
                                    text = Pages.AddSitePage.RegularExpression.getLang(),
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
                                                contentDescription = Pages.AddSitePage.RegexHelp.getLang(),
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = if (showRegexHelp.value) Pages.AddSitePage.ClickToCollapseHelp.getLang() else Pages.AddSitePage.ViewRegexExamples.getLang(),
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
                                                    text = Pages.AddSitePage.RegexExampleTitle.getLang(),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colors.onSurface
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchSpecificDomain.getLang(),
                                                    regex = ".*://.*example\\.com.*",
                                                    description = Pages.AddSitePage.MatchSpecificDomainDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchAllSubdomains.getLang(),
                                                    regex = ".*://.*\\.github\\.com.*",
                                                    description = Pages.AddSitePage.MatchAllSubdomainsDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchLocalNetwork.getLang(),
                                                    regex = ".*://.*192\\.168\\..*",
                                                    description = Pages.AddSitePage.MatchLocalNetworkDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchHTTPS.getLang(),
                                                    regex = "^https://.*",
                                                    description = Pages.AddSitePage.MatchHTTPSDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchSpecificPort.getLang(),
                                                    regex = ".*://.*:8080.*",
                                                    description = Pages.AddSitePage.MatchSpecificPortDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                RegexExampleInline(
                                                    title = Pages.AddSitePage.MatchFileTypes.getLang(),
                                                    regex = ".*\\.(jpg|png|gif|pdf)$",
                                                    description = Pages.AddSitePage.MatchFileTypesDesc.getLang(),
                                                    snackbarHostState = snackbarHostState,
                                                    scope = scope
                                                )

                                                Card(
                                                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = Pages.AddSitePage.RegexNote.getLang(),
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
                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.RegexAddedSuccess.getLang())
                                                } else {
                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.RegexAddFailed.getLang())
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(Pages.AddSitePage.RegexAddFailedRetry.getLang())
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
                                    text = Pages.AddSitePage.AddRegex.getLang(),
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
                                    contentDescription = Pages.AddSitePage.Manage.getLang(),
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = Pages.AddSitePage.ManageAddedItems.getLang(),
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
                                    contentDescription = if (showManagement.value) Pages.AddSitePage.Collapse.getLang() else Pages.AddSitePage.Expand.getLang(),
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
                                                text = "${Pages.AddSitePage.CustomRegex.getLang()} (${customRegexPatterns.size})",
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
                                                                title = Pages.AddSitePage.DeleteRegex.getLang(),
                                                                message = "${Pages.AddSitePage.ConfirmDeleteSelected.getLang()} ${selectedRegexPatterns.value.size} ${Pages.AddSitePage.RegexPatternsUndone.getLang()}",
                                                                onConfirm = {
                                                                    scope.launch {
                                                                        selectedRegexPatterns.value.forEach { pattern ->
                                                                            backend.removeCustomRegexPattern(pattern)
                                                                        }
                                                                        selectedRegexPatterns.value = emptySet()
                                                                        snackbarHostState.showSnackbar(Pages.AddSitePage.SelectedRegexDeleted.getLang())
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
                                                        contentDescription = Pages.AddSitePage.DeleteSelected.getLang(),
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
                                                            title = Pages.AddSitePage.DeleteRegex.getLang(),
                                                            message = "${Pages.AddSitePage.ConfirmDeleteSelected.getLang()} \"$pattern\" ${Pages.AddSitePage.Undone.getLang()}",
                                                            onConfirm = {
                                                                scope.launch {
                                                                    backend.removeCustomRegexPattern(pattern)
                                                                    selectedRegexPatterns.value =
                                                                        selectedRegexPatterns.value - pattern
                                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.RegexDeleted.getLang())
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
                                                text = "${Pages.AddSitePage.AddedSites.getLang()} (${dynamicSites.size})",
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
                                                                title = Pages.AddSitePage.DeleteSite.getLang(),
                                                                message = "${Pages.AddSitePage.ConfirmDeleteSelected.getLang()} ${selectedSiteIds.value.size} ${Pages.AddSitePage.Sites.getLang()}\n\n${Pages.AddSitePage.DeleteSiteWarning.getLang()}",
                                                                onConfirm = {
                                                                    scope.launch {
                                                                        selectedSites.forEach { site ->
                                                                            backend.requestDeleteSite(site)
                                                                        }
                                                                        selectedSiteIds.value = emptySet()
                                                                        snackbarHostState.showSnackbar(Pages.AddSitePage.SelectedSitesDeleted.getLang())
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
                                                            title = Pages.AddSitePage.DeleteSite.getLang(),
                                                            message = "${Pages.AddSitePage.ConfirmDeleteSelected.getLang()} \"${site.label}\" \n\n${Pages.AddSitePage.DeleteSiteSingleWarning.getLang()}",
                                                            onConfirm = {
                                                                scope.launch {
                                                                    backend.requestDeleteSite(site)
                                                                    selectedSiteIds.value =
                                                                        selectedSiteIds.value - site.id
                                                                    snackbarHostState.showSnackbar(Pages.AddSitePage.SiteDeleted.getLang())
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
                                        text = Pages.AddSitePage.NoItemsToManage.getLang(),
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
                            snackbarHostState.showSnackbar(Pages.AddSitePage.CopiedToClipboard.getLang())
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = Pages.AddSitePage.Copy.getLang(),
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
                    contentDescription = Pages.AddSitePage.Delete.getLang(),
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
                            text = "${Pages.AddSitePage.Status.getLang()}: ${site.status}",
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
                    contentDescription = Pages.AddSitePage.Status.getLang(),
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
                        Text(Pages.AddSitePage.Cancel.getLang())
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text(Pages.AddSitePage.ConfirmDelete.getLang())
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
