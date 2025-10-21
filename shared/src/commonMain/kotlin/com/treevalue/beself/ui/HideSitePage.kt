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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
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

data class HiddenSiteInfo(
    val siteInfo: com.treevalue.beself.net.SiteInfo,
    val isHidden: Boolean,
)

@Composable
fun HideSitePage(
    onBackClicked: () -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    val allSites = backend?.getAllSitesIncludeHidden() ?: emptyList()

    val hiddenSites = allSites.map { site ->
        HiddenSiteInfo(
            siteInfo = site,
            isHidden = backend?.isSiteHidden(site.id) ?: false
        )
    }

    // 添加搜索状态
    var searchText by remember { mutableStateOf("") }

    // 过滤网站列表
    val filteredHiddenSites = remember(hiddenSites, searchText) {
        if (searchText.isBlank()) {
            hiddenSites
        } else {
            hiddenSites.filter { hiddenSite ->
                hiddenSite.siteInfo.label.contains(searchText, ignoreCase = true) ||
                        hiddenSite.siteInfo.host.contains(searchText, ignoreCase = true)
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
                text = Pages.FunctionPage.HideSite.getLang(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
        }

        // 搜索框
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
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
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = { searchText = "" },
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

        // 统计信息
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
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = Pages.HideSitePage.Info.getLang(),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${Pages.HideSitePage.Total.getLang()} ${filteredHiddenSites.size} ${Pages.HideSitePage.SitesHidden.getLang()} ${filteredHiddenSites.count { it.isHidden }} ${Pages.HideSitePage.Count.getLang()}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 网站列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredHiddenSites) { hiddenSite ->
                HideSiteItem(
                    hiddenSite = hiddenSite,
                    onToggleVisibility = {
                        backend?.toggleSiteVisibility(hiddenSite.siteInfo.id)
                    }
                )
            }
        }
    }
}

@Composable
fun HideSiteItem(
    hiddenSite: HiddenSiteInfo,
    onToggleVisibility: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (hiddenSite.isHidden)
            MaterialTheme.colors.surface.copy(alpha = 0.7f)
        else
            MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 网站图标占位
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hiddenSite.siteInfo.label.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = hiddenSite.siteInfo.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hiddenSite.isHidden)
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hiddenSite.siteInfo.host,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 眼睛图标按钮
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (hiddenSite.isHidden)
                            Color.Gray.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    )
            ) {
                Icon(
                    imageVector = if (hiddenSite.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (hiddenSite.isHidden) Pages.HideSitePage.Show.getLang() else Pages.HideSitePage.Hide.getLang(),
                    tint = if (hiddenSite.isHidden) Color.Gray else MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
