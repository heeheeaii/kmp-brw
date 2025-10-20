package com.treevalue.beself.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.data.TabInfo

@Composable
fun TabBar(
    tabs: List<TabInfo>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit = {},
    onTabClosed: (TabInfo) -> Unit = {},
    onNewTab: () -> Unit = {}
) {
    ScrollableTabRow(
        selectedTabIndex = activeTabIndex,
        edgePadding = 0.dp,
        modifier = Modifier.fillMaxWidth().height(30.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        tabs.forEachIndexed { index, tabInfo ->
            Tab(
                modifier = Modifier.width(40.dp).fillMaxHeight(),
                selected = index == activeTabIndex,
                onClick = { onTabSelected(index) },
                text = {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = tabInfo.title.value,
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )

                        IconButton(
                            onClick = { onTabClosed(tabInfo) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(15.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tab",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            )
        }

        IconButton(onClick = onNewTab, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        }
    }
}
