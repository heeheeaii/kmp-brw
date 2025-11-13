package com.treevalue.beself.persistence

import com.treevalue.beself.backend.FeatureSettings
import com.treevalue.beself.net.SiteStatus
import kotlinx.serialization.Serializable

@Serializable
data class PersistentTabInfo(
    val id: String = "",
    val initialUrl: String? = null,
    val initialHtml: String? = null,
    val title: String = "",
)

@Serializable
data class PersistentSiteInfo(
    val id: String = "",
    val label: String = "",
    val host: String = "",
    val status: SiteStatus,
    val originalUrl: String? = null,
)

@Serializable
data class BrowserState(
    val tabs: List<PersistentTabInfo> = emptyList(),
    val activeTabIndex: Int = -1,
    val dynamicSites: List<PersistentSiteInfo> = emptyList(),
    val excludedSiteIds: List<String> = emptyList(),
    val hiddenSiteIds: List<String> = emptyList(),
    val forceDark: Boolean = false,
    val sidebarVisible: Boolean = true,
    val startPageSiteId: String? = null,
    val blockedSites: List<String> = emptyList(),
    val customRegexPatterns: List<String> = emptyList(),
    val featureSettings: FeatureSettings = FeatureSettings(),
    val customHomeText: String = "Hee"
)
