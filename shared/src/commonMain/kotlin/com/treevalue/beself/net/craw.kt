package com.treevalue.beself.net

import kotlinx.coroutines.flow.Flow

data class CrawlResult(
    val mainUrl: String,
    val urls: List<String>,
    val success: Boolean,
    val error: String? = null,
)

data class CrawlProgress(
    val currentUrl: String,
    val foundUrls: Int,
    val isComplete: Boolean = false,
)

data class GrabbedSite(
    val domain: String,
    val urls: List<String>,
    var isExpanded: Boolean = false,
    var isSelected: Boolean = false,
    val selectedUrls: List<String> = emptyList(),
)

expect class NetworkCrawler() {
    suspend fun downloadFile(
        url: String,
        onProgress: (Float, String?) -> Unit,
    )

    suspend fun fetchHtml(url: String): String
    suspend fun crawlWebsite(url: String): Flow<CrawlResult>
}
