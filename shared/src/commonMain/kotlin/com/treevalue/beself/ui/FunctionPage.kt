package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.bus.TabEvent

@Composable
fun FunctionPage(
    backend: InterceptRequestBackend? = null,
    onBackClicked: () -> Unit = {},
) {
    var currentPage by remember { mutableStateOf(FunctionPageType.HOME) }

    when (currentPage) {
        FunctionPageType.ADD_SITE -> {
            AddSitePage(
                onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend
            )
        }

        FunctionPageType.HOME -> {
            FunctionHomePage(
                onBackClicked = onBackClicked,
                onOptionSelected = { option ->
                    currentPage = option
                },
                backend = backend,
            )
        }

        FunctionPageType.HIDE_SITE -> {
            HideSitePage(
                onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend
            )
        }

        FunctionPageType.BLOCK_SITE -> {
            BlockSitePage(onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend)
        }

        FunctionPageType.GRAB_SITE -> {
            GrabSitePage(onBackClicked = { currentPage = FunctionPageType.HOME })
        }

        FunctionPageType.START_PAGE_SETTING -> {
            StartPageSettingPage(
                onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend
            )
        }

        FunctionPageType.OPEN_URL -> {
            OpenUrlPage(
                onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend
            )
        }

        FunctionPageType.OTHER_FUNCTIONS -> {
            OtherFunctionPage(
                onBackClicked = { currentPage = FunctionPageType.HOME }, backend = backend
            )
        }
    }
}

@Composable
fun FunctionHomePage(
    onBackClicked: () -> Unit,
    onOptionSelected: (FunctionPageType) -> Unit,
    backend: InterceptRequestBackend? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(16.dp)
    ) {
        // 顶部返回按钮和标题
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked, modifier = Modifier.size(40.dp)
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
                text = Pages.FunctionPage.FunctionSettings.getLang(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp
            )
        ) {
            item {
                FunctionCard(icon = Icons.Default.Add,
                    title = Pages.FunctionPage.AddAllow.getLang(),
                    description = Pages.FunctionPage.AddAllowDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.ADD_SITE) })
            }
            item {
                FunctionCard(icon = Icons.Default.OpenInBrowser,
                    title = Pages.FunctionPage.OpenURL.getLang(),
                    description = Pages.FunctionPage.OpenURLDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.OPEN_URL) })
            }
            item {
                FunctionCard(icon = Icons.Default.VisibilityOff,
                    title = Pages.FunctionPage.HideSite.getLang(),
                    description = Pages.FunctionPage.HideSiteDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.HIDE_SITE) })
            }

            item {
                FunctionCard(icon = Icons.Default.Block,
                    title = Pages.FunctionPage.BlockSite.getLang(),
                    description = Pages.FunctionPage.BlockSiteDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.BLOCK_SITE) })
            }

            item {
                FunctionCard(icon = Icons.Default.GetApp,
                    title = Pages.FunctionPage.GrabSite.getLang(),
                    description = Pages.FunctionPage.GrabSiteDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.GRAB_SITE) })
            }
            item {
                FunctionCard(icon = Icons.Default.Home,
                    title = Pages.FunctionPage.StartPage.getLang(),
                    description = Pages.FunctionPage.StartPageDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.START_PAGE_SETTING) })
            }
            item {
                FunctionCard(icon = Icons.Default.Settings,
                    title = Pages.FunctionPage.OtherFunctions.getLang(),
                    description = Pages.FunctionPage.OtherFunctionsDescription.getLang(),
                    onClick = { onOptionSelected(FunctionPageType.OTHER_FUNCTIONS) })
            }
            item {
                FunctionCard(
                    icon = Icons.Default.Update,
                    title = Pages.FunctionPage.GetUpdate.getLang(),
                    description = Pages.FunctionPage.GetLatestVersion.getLang(),
                    onClick = {
                        backend?.let {
                            EventBus.publish(
                                TabEvent.RequestNewTab(
                                    "https://heeheeaii.github.io", Pages.FunctionPage.UpdatePage.getLang()
                                )
                            )
                            EventBus.publish(PopEvent.HidePop)
                        }
                    })
            }
        }
    }
}

@Composable
fun FunctionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标背景
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description, fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = Pages.FunctionPage.Enter.getLang(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

enum class FunctionPageType {
    HOME, HIDE_SITE, BLOCK_SITE, GRAB_SITE, START_PAGE_SETTING, OPEN_URL, ADD_SITE, OTHER_FUNCTIONS
}
