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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.SiteStatus
import com.treevalue.beself.platform.g_desktop
import com.treevalue.beself.platform.getPlatformName

data class FunctionItem(
    val type: FunctionPageType,
    val icon: ImageVector,
    val title: String,
    val description: String,
)

@Composable
fun SearchPage(
    backend: InterceptRequestBackend,
) {
    val searchState = backend.searchState
    val allSites = backend.getAllSites()
    val allFunctions = remember {
        listOf(
            FunctionItem(
                FunctionPageType.ADD_SITE,
                Icons.Default.Add,
                Pages.FunctionPage.AddAllow.getLang(),
                Pages.FunctionPage.AddAllowDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.OPEN_URL,
                Icons.Default.OpenInBrowser,
                Pages.FunctionPage.OpenURL.getLang(),
                Pages.FunctionPage.OpenURLDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.HIDE_SITE,
                Icons.Default.VisibilityOff,
                Pages.FunctionPage.HideSite.getLang(),
                Pages.FunctionPage.HideSiteDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.BLOCK_SITE,
                Icons.Default.Block,
                Pages.FunctionPage.BlockSite.getLang(),
                Pages.FunctionPage.BlockSiteDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.GRAB_SITE,
                Icons.Default.GetApp,
                Pages.FunctionPage.GrabSite.getLang(),
                Pages.FunctionPage.GrabSiteDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.START_PAGE_SETTING,
                Icons.Default.Home,
                Pages.FunctionPage.StartPage.getLang(),
                Pages.FunctionPage.StartPageDescription.getLang()
            ),
            FunctionItem(
                FunctionPageType.CALCULATOR,
                Icons.Default.Calculate,
                Pages.OtherFunctionsPage.Calculator.getLang(),
                "快速计算工具"
            ),
            FunctionItem(
                FunctionPageType.SCHEDULE,
                Icons.Default.CalendarToday,
                Pages.SchedulePage.ScheduleManagement.getLang(),
                "管理您的日程安排"
            ),
            FunctionItem(
                FunctionPageType.COMPRESSION,
                Icons.Default.Compress,
                "压缩",
                "图片和文件压缩工具"
            ),
            FunctionItem(
                FunctionPageType.SYSTEM_SETTINGS,
                Icons.Default.Settings,
                Pages.SystemSettingsPage.SystemSettings.getLang(),
                "系统设置和配置"
            )
        )
    }

    val filteredFunctions = remember(searchState.searchText.value) {
        mutableStateOf<List<FunctionItem>>(emptyList())
    }

    // 当搜索文本改变时，更新过滤结果
    LaunchedEffect(searchState.searchText.value) {
        searchState.filterSites(allSites)

        val searchText = searchState.searchText.value.trim().lowercase()
        filteredFunctions.value = if (searchText.isEmpty()) {
            emptyList()
        } else {
            allFunctions.filter { function ->
                function.title.lowercase().contains(searchText) ||
                        function.description.lowercase().contains(searchText)
            }
        }
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
                            text = if (getPlatformName() != g_desktop) "名称或地址" else "搜索网站名称、地址或功能",
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
        val hasSiteResults = searchState.filteredSites.value.isNotEmpty()
        val hasFunctionResults = filteredFunctions.value.isNotEmpty()
        val hasAnyResults = hasSiteResults || hasFunctionResults

        if (hasAnyResults) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 显示功能页搜索结果
                if (hasFunctionResults) {
                    item {
                        Text(
                            text = "功能 (${filteredFunctions.value.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredFunctions.value) { function ->
                        FunctionSearchItem(
                            function = function
                        )
                    }

                    // 分隔空间
                    if (hasSiteResults) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 显示网站搜索结果
                if (hasSiteResults) {
                    item {
                        Text(
                            text = "网站 (${searchState.filteredSites.value.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(searchState.filteredSites.value) { site ->
                        SiteSearchItem(
                            site = site,
                            backend = backend
                        )
                    }
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
                        text = "未找到匹配的网站或功能",
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
fun FunctionSearchItem(
    function: FunctionItem,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            // 发布对应的事件来打开功能页
            when (function.type) {
                FunctionPageType.ADD_SITE -> EventBus.publish(PopEvent.AddSite)
                FunctionPageType.OPEN_URL -> EventBus.publish(PopEvent.OpenUrl)
                FunctionPageType.HIDE_SITE -> EventBus.publish(PopEvent.HideSite)
                FunctionPageType.BLOCK_SITE -> EventBus.publish(PopEvent.BlockSite)
                FunctionPageType.GRAB_SITE -> EventBus.publish(PopEvent.GrabSite)
                FunctionPageType.START_PAGE_SETTING -> EventBus.publish(PopEvent.StartPageSetting)
                FunctionPageType.CALCULATOR -> EventBus.publish(PopEvent.Calculator)
                FunctionPageType.SCHEDULE -> EventBus.publish(PopEvent.Schedule)
                FunctionPageType.COMPRESSION -> EventBus.publish(PopEvent.Compression)
                FunctionPageType.SYSTEM_SETTINGS -> EventBus.publish(PopEvent.SystemSettings)
                else -> {}
            }
        },
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 功能图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = function.icon,
                    contentDescription = function.title,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = function.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = function.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 箭头图标
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "打开",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SiteSearchItem(
    site: SiteInfo,
    backend: InterceptRequestBackend,
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
                        SiteStatus.FAILED -> Pages.BlockSitePage.Failed.getLang()
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
