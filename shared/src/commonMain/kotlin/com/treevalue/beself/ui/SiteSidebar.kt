package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.net.SiteStatus

@Composable
fun SiteSidebar(
    backend: InterceptRequestBackend,
    onSiteClick: (label: String, urlOrHost: String) -> Unit,
) {
    val allSites = backend.getAllSites()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(100.dp)
            .padding(4.dp)
            .background(MaterialTheme.colors.surface),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(allSites) { site ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                    ) {
                        val buttonColor = when (site.status) {
                            SiteStatus.PENDING -> ButtonDefaults.buttonColors(
                                backgroundColor = Color.Yellow.copy(alpha = 0.7f)
                            )

                            SiteStatus.COMPLETED -> ButtonDefaults.buttonColors()
                            SiteStatus.FAILED -> ButtonDefaults.buttonColors(
                                backgroundColor = Color.Red.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = {
                                if (site.status == SiteStatus.COMPLETED) {
                                    onSiteClick(site.label, site.originalUrl ?: site.host)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColor,
                            enabled = site.status == SiteStatus.COMPLETED
                        ) {
                            Text(
                                text = site.label,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = {
                                backend.requestDeleteSite(site)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Delete site",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White
            IconButton(
                onClick = { EventBus.publish(PopEvent.HelpPop) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = iconColor
                )
            }

            IconButton(
                onClick = { EventBus.publish(PopEvent.FunctionMenu) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Function Menu",
                    tint = iconColor
                )
            }
        }
    }
}
