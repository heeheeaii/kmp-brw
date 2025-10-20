package com.treevalue.beself.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlExtractor {

    suspend fun extractUrlsFromHtml(html: String, baseUrl: String): List<String> = withContext(Dispatchers.Default) {
        if (html.isBlank()) {
            
            return@withContext emptyList<String>()
        }

        try {
            
            

            val doc = org.jsoup.Jsoup.parse(html, baseUrl)
            val urls = mutableSetOf<String>()

            // 1. 图片资源
            val images = doc.select("img[src]")
            
            images.forEach { element ->
                val src = element.attr("abs:src")
                if (src.isNotBlank()) {
                    urls.add(src)
                    
                }

                // 处理 srcset
                val srcset = element.attr("srcset")
                if (srcset.isNotBlank()) {
                    extractUrlsFromSrcset(srcset, baseUrl).forEach {
                        urls.add(it)
                        
                    }
                }
            }

            // 2. CSS文件
            val cssLinks = doc.select("link[rel*=stylesheet]")
            
            cssLinks.forEach { element ->
                val href = element.attr("abs:href")
                if (href.isNotBlank()) {
                    urls.add(href)
                    
                }
            }

            // 3. JavaScript文件
            val scripts = doc.select("script[src]")
            
            scripts.forEach { element ->
                val src = element.attr("abs:src")
                if (src.isNotBlank()) {
                    urls.add(src)
                    
                }
            }

            // 4. 所有链接
            val links = doc.select("a[href]")
            
            links.forEach { element ->
                val href = element.attr("abs:href")
                if (href.isNotBlank()) {
                    urls.add(href)
                    
                }
            }

            // 5. 表单action
            val forms = doc.select("form[action]")
            
            forms.forEach { element ->
                val action = element.attr("abs:action")
                if (action.isNotBlank()) {
                    urls.add(action)
                    
                }
            }

            // 6. iframe源
            val iframes = doc.select("iframe[src]")
            
            iframes.forEach { element ->
                val src = element.attr("abs:src")
                if (src.isNotBlank()) {
                    urls.add(src)
                    
                }
            }

            // 7. 媒体资源
            val media = doc.select("video[src], audio[src], source[src]")
            
            media.forEach { element ->
                val src = element.attr("abs:src")
                if (src.isNotBlank()) {
                    urls.add(src)
                    
                }
            }

            // 8. 视频 poster
            val videoPosters = doc.select("video[poster]")
            
            videoPosters.forEach { element ->
                val poster = element.attr("abs:poster")
                if (poster.isNotBlank()) {
                    urls.add(poster)
                    
                }
            }

            // 9. 背景图片
            val styledElements = doc.select("*[style*=background]")
            
            styledElements.forEach { element ->
                extractUrlsFromStyle(element.attr("style"), baseUrl).forEach {
                    urls.add(it)
                    
                }
            }

            // 10. 图标
            val icons = doc.select("link[rel*=icon]")
            
            icons.forEach { element ->
                val href = element.attr("abs:href")
                if (href.isNotBlank()) {
                    urls.add(href)
                    
                }
            }

            // 11. Preload 资源
            val preloadLinks = doc.select("link[rel=preload], link[rel=prefetch]")
            
            preloadLinks.forEach { element ->
                val href = element.attr("abs:href")
                if (href.isNotBlank()) {
                    urls.add(href)
                    
                }
            }

            // 12. Manifest
            val manifests = doc.select("link[rel=manifest]")
            
            manifests.forEach { element ->
                val href = element.attr("abs:href")
                if (href.isNotBlank()) {
                    urls.add(href)
                    
                }
            }

            // 13. 字体
            val styleElements = doc.select("style")
            
            styleElements.forEach { styleElement ->
                extractFontUrlsFromCss(styleElement.html(), baseUrl).forEach {
                    urls.add(it)
                    
                }
            }

            // 14. 自定义属性
            val customElements = doc.select("[data-src], [data-background]")
            
            customElements.forEach { element ->
                listOf("data-src", "data-background").forEach { attr ->
                    val value = element.attr(attr)
                    if (value.isNotBlank()) {
                        try {
                            val absoluteUrl = java.net.URL(java.net.URL(baseUrl), value).toString()
                            urls.add(absoluteUrl)
                            
                        } catch (e: Exception) {
                            
                        }
                    }
                }
            }

            // 过滤和排序
            val filteredUrls = urls.filter { isValidUrl(it) }.toList().sorted()
            

            return@withContext filteredUrls

        } catch (e: Exception) {
            
            e.printStackTrace()
            return@withContext emptyList<String>()
        }
    }

    private fun extractUrlsFromSrcset(srcset: String, baseUrl: String): List<String> {
        return srcset.split(",").mapNotNull { srcsetItem ->
            val imageUrl = srcsetItem.trim().split(" ")[0]
            if (imageUrl.isNotBlank()) {
                try {
                    java.net.URL(java.net.URL(baseUrl), imageUrl).toString()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun extractUrlsFromStyle(style: String, baseUrl: String): List<String> {
        val urlPattern = Regex("""url\(['"]?([^'")\s]+)['"]?\)""")
        return urlPattern.findAll(style).mapNotNull { match ->
            val relativeUrl = match.groupValues[1]
            try {
                java.net.URL(java.net.URL(baseUrl), relativeUrl).toString()
            } catch (e: Exception) {
                null
            }
        }.toList()
    }

    private fun extractFontUrlsFromCss(cssContent: String, baseUrl: String): List<String> {
        val fontUrlPattern = Regex("""@font-face[^}]*url\(['"]?([^'")\s]+)['"]?\)""")
        return fontUrlPattern.findAll(cssContent).mapNotNull { match ->
            val fontUrl = match.groupValues[1]
            try {
                java.net.URL(java.net.URL(baseUrl), fontUrl).toString()
            } catch (e: Exception) {
                null
            }
        }.toList()
    }

    private fun isValidUrl(url: String): Boolean {
        val isValid = url.startsWith("http") &&
                !url.contains("javascript:") &&
                !url.contains("mailto:") &&
                !url.contains("tel:")

        if (!isValid) {
            
        }

        return isValid
    }
}
