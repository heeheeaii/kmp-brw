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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.SiteStatus

@Composable
fun StartPageSettingPage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null
) {
    var searchText by remember { mutableStateOf("") }
    val allSites = backend?.getAllSitesIncludeHidden() ?: emptyList()
    val currentStartPage = backend?.getStartPageSetting()
    var selectedSite by remember { mutableStateOf<SiteInfo?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                    contentDescription = "返回",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "开始页面设置",
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
                        contentDescription = "信息",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前开始页面",
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
                            "默认页面 (系统内置HTML页面)"
                        }
                    } else {
                        "默认页面 (系统内置HTML页面)"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                if (currentStartPage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
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
                        Text("恢复默认页面", color = Color.White)
                    }
                }
            }
        }

        // 网站选择说明
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
                        contentDescription = "确认",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("设置开始页面")
                }
            },
            text = {
                val message = selectedSite?.let { site ->
                    "确定要将 \"${site.label}\" 设置为开始页面吗？"
                } ?: "确定要恢复到默认开始页面吗？"

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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text("取消")
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
    backend: InterceptRequestBackend? = null
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
                    contentDescription = "已选中",
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
                            text = "(已隐藏)",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (site.status == SiteStatus.FAILED) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(验证失败)",
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
