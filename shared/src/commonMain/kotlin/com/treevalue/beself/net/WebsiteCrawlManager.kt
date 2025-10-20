package com.treevalue.beself.net

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WebsiteCrawlManager {
    private val crawler = NetworkCrawler()
    private val urlExtractor = UrlExtractor()


    suspend fun downloadFile(
        url: String,
        onProgress: (Float, String?) -> Unit,
    ) {
        crawler.downloadFile(url, onProgress)

    }

    fun crawlWebsite(url: String): Flow<CrawlResult> = flow {
        try {
            

            // 确保URL格式正确
            val normalizedUrl = normalizeUrl(url)
            

            // 获取HTML内容
            val html = crawler.fetchHtml(normalizedUrl)
            

            if (html.isNotEmpty()) {
                // 提取所有URL
                val extractedUrls = urlExtractor.extractUrlsFromHtml(html, normalizedUrl)
                

                emit(
                    CrawlResult(
                        mainUrl = normalizedUrl,
                        urls = extractedUrls,
                        success = true
                    )
                )
            } else {
                
                emit(
                    CrawlResult(
                        mainUrl = normalizedUrl,
                        urls = emptyList(),
                        success = false,
                        error = "网页内容为空，无法获取 HTML 内容"
                    )
                )
            }
        } catch (e: Exception) {
            
            e.printStackTrace()
            emit(
                CrawlResult(
                    mainUrl = url,
                    urls = emptyList(),
                    success = false,
                    error = "抓取失败: ${e.message}"
                )
            )
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmedUrl = url.trim()
        return when {
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> trimmedUrl
            else -> "https://$trimmedUrl"
        }
    }
}

expect fun openDownloadDirectory()
