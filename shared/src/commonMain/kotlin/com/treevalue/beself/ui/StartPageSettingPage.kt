package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.SiteStatus

@Composable
fun CustomHomeTextPage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    var currentSavedText by remember { mutableStateOf(backend?.getCustomHomeText() ?: "Hee") }
    var textInput by remember { mutableStateOf(currentSavedText) }


    LaunchedEffect(backend?.getCustomHomeText()) {
        currentSavedText = backend?.getCustomHomeText() ?: "Hee"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        // 顶部导航栏
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
                text = "自定义主页文字",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
        }

        // 当前文字显示
        Card(
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = currentSavedText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }

        // 输入框
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "新文字内容",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        focusedIndicatorColor = MaterialTheme.colors.primary,
                        unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    ),
                    placeholder = {
                        Text("输入你想显示的文字...")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "字符数: ${textInput.length}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        backend?.setCustomHomeText(textInput)
                        backend?.setCustomHomeText(textInput)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = textInput.isNotBlank()
            ) {
                Text("保存", color = Color.White)
            }
        }
    }
}


@Composable
fun StartPageSettingPage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    var searchText by remember { mutableStateOf("") }
    val allSites = backend?.getAllSitesIncludeHidden() ?: emptyList()
    val currentStartPage = backend?.getStartPageSetting()
    var selectedSite by remember { mutableStateOf<SiteInfo?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCustomTextPage by remember { mutableStateOf(false) }
    if (showCustomTextPage) {
        CustomHomeTextPage(
            onBackClicked = { showCustomTextPage = false },
            backend = backend
        )
        return
    }

    // 初始化当前选中的网站
    LaunchedEffect(allSites) {
        if (selectedSite == null) {
            selectedSite = if (currentStartPage != null) {
                allSites.find { it.id == currentStartPage }
            } else {
                null
            }
        }
    }

    val filteredSites = remember(allSites, searchText) {
        if (searchText.isBlank()) {
            allSites
        } else {
            allSites.filter { site ->
                site.label.contains(searchText, ignoreCase = true) ||
                        site.host.contains(searchText, ignoreCase = true)
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
                text = Pages.StartPageSettings.StartPageSettings.getLang(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
        }

        // 当前设置信息
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = Pages.HideSitePage.Info.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Pages.StartPageSettings.CurrentStartPage.getLang(),
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentStartPage != null) {
                        val currentSite = allSites.find { it.id == currentStartPage }
                        if (currentSite != null) {
                            "${currentSite.label} (${currentSite.host})"
                        } else {
                            Pages.StartPageSettings.DefaultPage.getLang()
                        }
                    } else {
                        Pages.StartPageSettings.DefaultPage.getLang()
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                // 新增：自定义主页文字按钮
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 修改：编辑按钮点击打开自定义文字页面
                    Button(
                        onClick = { showCustomTextPage = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "自定义主页文字",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("自定义文字", color = Color.White)
                    }

                    if (currentStartPage != null) {
                        Button(
                            onClick = {
                                selectedSite = null
                                showConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(Pages.StartPageSettings.RestoreDefaultPage.getLang(), color = Color.White)
                        }
                    }
                }
            }
        }

        // 搜索框
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    focusedIndicatorColor = MaterialTheme.colors.primary,
                    unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = { searchText = "" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 网站列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSites) { site ->
                StartPageSiteItem(
                    site = site,
                    isSelected = selectedSite?.id == site.id,
                    onSelected = {
                        selectedSite = site
                        showConfirmDialog = true
                    },
                    backend = backend
                )
            }
        }
    }

    // 确认对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = Pages.StartPageSettings.Confirm.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Pages.StartPageSettings.SetStartPage.getLang())
                }
            },
            text = {
                val message = selectedSite?.let { site ->
                    "${Pages.StartPageSettings.ConfirmSetQuestion.getLang()} \"${site.label}\" ${Pages.StartPageSettings.AsStartPage.getLang()}"
                } ?: Pages.StartPageSettings.ConfirmRestoreDefault.getLang()

                Text(message)
            },
            confirmButton = {
                Button(
                    onClick = {
                        backend?.setStartPageSetting(selectedSite?.id)
                        showConfirmDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Pages.BlockSitePage.OK.getLang())
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(Pages.AddSitePage.Cancel.getLang())
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StartPageSiteItem(
    site: SiteInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    val isHidden = backend?.isSiteHidden(site.id) ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        elevation = if (isSelected) 8.dp else 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (isSelected)
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else if (isHidden)
            MaterialTheme.colors.surface.copy(alpha = 0.7f)
        else
            MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中状态指示器
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = Pages.StartPageSettings.Selected.getLang(),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 网站图标占位
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colors.primary
                        else MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = site.label.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = site.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isHidden)
                            MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        else if (isSelected)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isHidden) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Pages.StartPageSettings.Hidden.getLang(),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (site.status == SiteStatus.FAILED) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Pages.StartPageSettings.Hidden.getLang(),
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = site.host,
                    fontSize = 14.sp,
                    color = if (isSelected)
                        MaterialTheme.colors.primary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
