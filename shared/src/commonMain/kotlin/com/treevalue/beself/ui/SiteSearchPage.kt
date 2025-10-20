package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.SiteStatus
import com.treevalue.beself.platform.g_desktop
import com.treevalue.beself.platform.getPlatformName

@Composable
fun SiteSearchPage(
    backend: InterceptRequestBackend
) {
    val searchState = backend.searchState
    val allSites = backend.getAllSites()

    // 当搜索文本改变时，更新过滤结果
    LaunchedEffect(searchState.searchText.value) {
        searchState.filterSites(allSites)
    }

    // 初始化时过滤网站
    LaunchedEffect(allSites) {
        searchState.filterSites(allSites)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // 顶部控制栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 保留搜索内容复选框
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                searchState.toggleKeepSearchContent()
            }) {
                Icon(
                    imageVector = if (searchState.keepSearchContent.value) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Keep search content",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保留搜索内容", fontSize = 14.sp, color = MaterialTheme.colors.onSurface
                )
            }
        }

        // 搜索框
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = searchState.searchText.value,
                    onValueChange = { searchState.updateSearchText(it) },
                    placeholder = {
                        Text(
                            text = if (getPlatformName() != g_desktop) "名称或地址" else "搜索网站名称或地址",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchState.searchText.value.isNotEmpty()) {
                    IconButton(
                        onClick = { searchState.clearSearchText() },
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
        }

        // 搜索结果
        if (searchState.filteredSites.value.isNotEmpty()) {
            Text(
                text = "找到 ${searchState.filteredSites.value.size} 个网站",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchState.filteredSites.value) { site ->
                    SiteSearchItem(
                        site = site,
                        backend = backend
                    )
                }
            }
        } else if (searchState.searchText.value.isNotEmpty()) {
            // 无搜索结果
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔍", fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "未找到匹配的网站",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "尝试使用不同的关键词搜索",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // 显示所有网站
            Text(
                text = "所有网站 (${allSites.size})",
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allSites) { site ->
                    SiteSearchItem(
                        site = site,
                        backend = backend
                    )
                }
            }
        }
    }
}

@Composable
fun SiteSearchItem(
    site: SiteInfo,
    backend: InterceptRequestBackend
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = site.status == SiteStatus.COMPLETED) {
            backend.addSiteFromSidebar(site.label, site.host)
            EventBus.publish(PopEvent.HidePop)
        }, elevation = 2.dp, backgroundColor = when (site.status) {
            SiteStatus.PENDING -> Color.Yellow.copy(alpha = 0.1f)
            SiteStatus.COMPLETED -> MaterialTheme.colors.surface
            SiteStatus.FAILED -> Color.Red.copy(alpha = 0.1f)
        }, shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态指示器
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape).background(
                    when (site.status) {
                        SiteStatus.PENDING -> Color.Yellow
                        SiteStatus.COMPLETED -> Color.Green
                        SiteStatus.FAILED -> Color.Red
                    }
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = site.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (site.status == SiteStatus.COMPLETED) {
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = site.host,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 状态文本
            if (site.status != SiteStatus.COMPLETED) {
                Text(
                    text = when (site.status) {
                        SiteStatus.PENDING -> "验证中"
                        SiteStatus.FAILED -> "失败"
                        else -> ""
                    }, fontSize = 12.sp, color = when (site.status) {
                        SiteStatus.PENDING -> Color.Yellow
                        SiteStatus.FAILED -> Color.Red
                        else -> Color.Transparent
                    }
                )
            }
        }
    }
}
