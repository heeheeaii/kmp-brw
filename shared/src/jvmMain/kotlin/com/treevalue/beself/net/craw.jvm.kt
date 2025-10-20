package com.treevalue.beself.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

actual class NetworkCrawler actual constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        // 添加拦截器来处理编码问题
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val request = originalRequest.newBuilder()
                .removeHeader("Accept-Encoding") // 移除可能导致问题的编码头
                .build()
            chain.proceed(request)
        }
        .build()

    actual suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                // 移除 Accept-Encoding 头，让 OkHttp 自动处理
                .addHeader("Connection", "keep-alive")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val htmlContent = response.body?.string() ?: ""

                    // 检查内容是否为有效的HTML
                    val isValidHtml = htmlContent.contains("<html", ignoreCase = true) ||
                            htmlContent.contains("<!DOCTYPE", ignoreCase = true) ||
                            htmlContent.contains("<head", ignoreCase = true)


                    // 打印HTML的前500个字符用于调试
                    if (htmlContent.length > 500) {
                        val preview = htmlContent.substring(0, 500)

                        // 如果仍然是乱码，尝试检测编码
                        if (!isValidHtml) {

                            // 尝试使用字节数组重新读取
                            return@use tryAlternativeDecoding(response, url)
                        }
                    }

                    return@withContext htmlContent
                } else {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("无法获取网页内容: ${e.message}")
        }
    }

    // 备用解码方法
    private suspend fun tryAlternativeDecoding(response: okhttp3.Response, url: String): String {
        return try {
            // 重新请求，明确指定不要压缩
            val alternativeRequest = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; WebCrawler/1.0)")
                .addHeader("Accept", "text/html")
                .addHeader("Accept-Charset", "utf-8")
                .build()

            client.newCall(alternativeRequest).execute().use { altResponse ->
                if (altResponse.isSuccessful) {
                    val content = altResponse.body?.string() ?: ""

                    // 再次检查是否为有效HTML
                    if (content.contains("<html", ignoreCase = true) ||
                        content.contains("<!DOCTYPE", ignoreCase = true)
                    ) {
                        content
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    actual suspend fun crawlWebsite(url: String): Flow<CrawlResult> = flow {
        try {
            val html = fetchHtml(url)
            val urlExtractor = UrlExtractor()
            val extractedUrls = urlExtractor.extractUrlsFromHtml(html, url)

            emit(
                CrawlResult(
                    mainUrl = url,
                    urls = extractedUrls,
                    success = true
                )
            )
        } catch (e: Exception) {
            emit(
                CrawlResult(
                    mainUrl = url,
                    urls = emptyList(),
                    success = false,
                    error = e.message
                )
            )
        }
    }

    // 下载文件函数
    actual suspend fun downloadFile(
        url: String,
        onProgress: (Float, String?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onProgress(0f, "HTTP ${response.code}: ${response.message}")
                    return@withContext
                }

                val body = response.body ?: run {
                    onProgress(0f, "响应体为空")
                    return@withContext
                }

                val contentLength = body.contentLength()
                val fileName = extractFileNameFromUrl(url) ?: "downloaded_file"

                // 获取下载目录
                val downloadDir = getDownloadDirectory()
                val file = File(downloadDir, fileName)

                var downloadedBytes = 0L
                val buffer = ByteArray(8192)

                body.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (contentLength > 0) {
                                val progress = downloadedBytes.toFloat() / contentLength.toFloat()
                                onProgress(progress, null)
                            }
                        }
                    }
                }

                onProgress(1f, null)
            }
        } catch (e: Exception) {
            onProgress(0f, e.message ?: "未知错误")
        }
    }

    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            val uri = URI(url)
            val path = uri.path
            if (path.isNotEmpty()) {
                val fileName = path.substringAfterLast('/')
                if (fileName.isNotEmpty() && fileName.contains('.')) {
                    fileName
                } else {
                    "download_${System.currentTimeMillis()}"
                }
            } else {
                "download_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }
}

// 下载文件函数
suspend fun downloadFile(
    url: String,
    onProgress: (Float, String?) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onProgress(0f, "HTTP ${response.code}: ${response.message}")
                return@withContext
            }

            val body = response.body ?: run {
                onProgress(0f, "响应体为空")
                return@withContext
            }

            val contentLength = body.contentLength()
            val fileName = extractFileNameFromUrl(url) ?: "downloaded_file"

            // 获取下载目录
            val downloadDir = getDownloadDirectory()
            val file = File(downloadDir, fileName)

            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            body.byteStream().use { inputStream ->
                file.outputStream().use { outputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (contentLength > 0) {
                            val progress = downloadedBytes.toFloat() / contentLength.toFloat()
                            onProgress(progress, null)
                        }
                    }
                }
            }

            onProgress(1f, null)
        }
    } catch (e: Exception) {
        onProgress(0f, e.message ?: "未知错误")
    }
}

// 提取文件名
fun extractFileNameFromUrl(url: String): String? {
    return try {
        val uri = java.net.URI(url)
        val path = uri.path
        if (path.isNotEmpty()) {
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotEmpty() && fileName.contains('.')) {
                fileName
            } else {
                "download_${System.currentTimeMillis()}"
            }
        } else {
            "download_${System.currentTimeMillis()}"
        }
    } catch (e: Exception) {
        "download_${System.currentTimeMillis()}"
    }
}

// 获取下载目录 (需要根据平台实现)
expect fun getDownloadDirectory(): File
