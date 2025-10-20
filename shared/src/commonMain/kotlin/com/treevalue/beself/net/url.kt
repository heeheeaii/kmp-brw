package com.treevalue.beself.net

import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.config.BrowserConfig
import com.treevalue.beself.web.RequestInterceptor
import com.treevalue.beself.web.WebRequest
import com.treevalue.beself.web.WebRequestInterceptResult
import com.treevalue.beself.web.WebViewController

fun isUrlAllowed(url: String): Boolean {
    if (BrowserConfig.ALLOWED_SITES.any { url.contains(it.host, ignoreCase = true) }) return true
    return BrowserConfig.ALLOWED_PATTERNS.any { it.matches(url) }
}

fun createRequestInterceptor(backend: InterceptRequestBackend? = null): RequestInterceptor =
    object : RequestInterceptor {
        override fun onInterceptUrlRequest(
            request: WebRequest,
            navigator: WebViewController,
        ): WebRequestInterceptResult {
            if (!request.isForMainFrame) return WebRequestInterceptResult.Allow
            val url = request.url // 移除 lowercase() 保持原始大小写

            // 首先检查是否被屏蔽
            if (backend?.isUrlBlocked(url) == true) {
                return WebRequestInterceptResult.Reject
            }

            // 检查静态允许的网站
            if (isUrlAllowed(url)) {
                return WebRequestInterceptResult.Allow
            }

            // 检查动态添加的网站
            if (backend != null) {
                val dynamicSites = backend.getAllSites()
                val isDynamicAllowed = dynamicSites.any { site ->
                    url.contains(site.host, ignoreCase = true)
                }
                if (isDynamicAllowed) {
                    return WebRequestInterceptResult.Allow
                }
                val isDynamicPattern = InterceptRequestBackend.getInstance()?.customRegexPatterns?.any { pattern ->
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE).matches(url)
                    } catch (e: Exception) {
                        false
                    }
                }
                if (isDynamicPattern == true) {
                    return WebRequestInterceptResult.Allow
                }
            }

            return WebRequestInterceptResult.Reject
        }
    }

fun urlDecode(encodedUrl: String): String {
    val result = StringBuilder()
    var idx = 0

    while (idx < encodedUrl.length) {
        val char = encodedUrl[idx]

        if (char == '%' && idx + 2 < encodedUrl.length) {
            // 获取%后面的两个十六进制字符
            val hex = encodedUrl.substring(idx + 1, idx + 3)

            try {
                // 将十六进制字符串转换为整数，再转换为字符
                val decodedChar = hex.toInt(16).toChar()
                result.append(decodedChar)
                idx += 3 // 跳过 %XX
            } catch (e: NumberFormatException) {
                // 如果不是有效的十六进制，保持原样
                result.append(char)
                idx++
            }
        } else if (char == '+') {
            // URL编码中 + 代表空格
            result.append(' ')
            idx++
        } else {
            result.append(char)
            idx++
        }
    }

    return result.toString()
}

fun String.urlToPlain(): String = urlDecode(this)

fun getHostnameFromUrl(url: String): String {
    var plainUrl = url.urlToPlain()
    if (plainUrl.startsWith(newTabPrefix)) {
        plainUrl = plainUrl.replace(newTabPrefix, "")
    }
    return try {
        // 移除协议
        val afterProtocol = plainUrl.substringAfter("://")

        // 移除用户信息（如果存在）
        val afterUserInfo = if (afterProtocol.contains("@")) {
            afterProtocol.substringAfter("@")
        } else {
            afterProtocol
        }

        // 查找路径、查询参数或锚点的开始位置
        val endChars = listOf('/', '?', '#')
        var endIndex = -1

        endChars.forEach { char ->
            val index = afterUserInfo.indexOf(char)
            if (index != -1 && (endIndex == -1 || index < endIndex)) {
                endIndex = index
            }
        }

        val hostnameWithPort = if (endIndex != -1) {
            afterUserInfo.substring(0, endIndex)
        } else {
            afterUserInfo
        }

        // 处理IPv6地址（用方括号包围）
        if (hostnameWithPort.startsWith("[")) {
            val closingBracket = hostnameWithPort.indexOf("]")
            if (closingBracket != -1) {
                return hostnameWithPort.substring(0, closingBracket + 1)
            }
        }

        // 移除端口号
        val colonIndex = hostnameWithPort.lastIndexOf(":")
        if (colonIndex != -1) {
            val possiblePort = hostnameWithPort.substring(colonIndex + 1)
            // 检查冒号后面是否是数字（端口号）
            if (possiblePort.all { it.isDigit() }) {
                return hostnameWithPort.substring(0, colonIndex)
            }
        }

        hostnameWithPort
    } catch (e: Exception) {
        ""
    }
}

fun isValidUrlOrDomain(input: String): Boolean {
    if (input.isBlank()) return false

    val trimmed = input.trim()

    // 检查是否是完整URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
        trimmed.startsWith("ftp://") || trimmed.startsWith("ftps://")
    ) {
        return isValidUrl(trimmed)
    }

    // 检查是否是域名
    return isValidDomain(trimmed)
}

const val newTabPrefix = "newtab://"

fun isValidUrl(url: String): Boolean {
    return try {
        if (url.startsWith(newTabPrefix)) return true
        // 更全面的URL正则表达式
        val pattern = Regex(
            "^(https?|ftp|ftps)://" +                           // 协议
                    "(" +
                    "([a-zA-Z0-9\\-._~%!$&'()*+,;=]+@)?" +         // 用户信息（可选）
                    "(" +
                    "\\[([a-fA-F0-9:]+)\\]" +                  // IPv6地址
                    "|" +
                    "([0-9]{1,3}\\.){3}[0-9]{1,3}" +           // IPv4地址
                    "|" +
                    "([a-zA-Z0-9\\-._~%]+\\.)*[a-zA-Z0-9\\-._~%]+" + // 域名
                    ")" +
                    "(:[0-9]{1,5})?" +                             // 端口（可选）
                    ")" +
                    "(/[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)?" +          // 路径（可选）
                    "(\\?[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/?]*)?" +       // 查询参数（可选）
                    "(#[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/?]*)?$"          // 锚点（可选）
        )

        if (!pattern.matches(url)) return false

        // 额外验证：检查域名部分是否有效
        val hostname = getHostnameFromUrl(url)
        return isValidHostname(hostname)

    } catch (e: Exception) {
        false
    }
}

fun isValidDomain(domain: String): Boolean {
    return try {
        val trimmed = domain.trim()

        // 域名不能包含协议
        if (trimmed.contains("://")) return false

        // 域名不能包含路径、查询参数或锚点
        if (trimmed.contains("/") || trimmed.contains("?") || trimmed.contains("#")) return false

        // 域名不能包含空格
        if (trimmed.contains(" ")) return false

        // 检查是否是有效的主机名
        return isValidHostname(trimmed)

    } catch (e: Exception) {
        false
    }
}

fun isValidHostname(hostname: String): Boolean {
    return try {
        val trimmed = hostname.trim()

        if (trimmed.isEmpty()) return false

        // 检查是否是IPv4地址
        if (isValidIPv4(trimmed)) return true

        // 检查是否是IPv6地址
        if (isValidIPv6(trimmed)) return true

        // 检查是否是有效域名
        return isValidDomainName(trimmed)

    } catch (e: Exception) {
        false
    }
}

fun isValidIPv4(ip: String): Boolean {
    return try {
        val parts = ip.split(".")
        if (parts.size != 4) return false

        parts.forEach { part ->
            val num = part.toIntOrNull() ?: return false
            if (num < 0 || num > 255) return false
            // 不允许前导零（除了"0"本身）
            if (part.length > 1 && part.startsWith("0")) return false
        }

        true
    } catch (e: Exception) {
        false
    }
}

fun isValidIPv6(ip: String): Boolean {
    return try {
        // 移除方括号（如果存在）
        val cleanIp = ip.removePrefix("[").removeSuffix("]")

        // 简单的IPv6验证
        val pattern = Regex("^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$|^::$|^::1$")

        // 检查双冒号的使用（最多只能有一个::）
        val doubleColonCount = cleanIp.split("::").size - 1
        if (doubleColonCount > 1) return false

        pattern.matches(cleanIp)
    } catch (e: Exception) {
        false
    }
}

fun isValidDomainName(domain: String): Boolean {
    return try {
        // 域名长度限制
        if (domain.length > 253) return false

        // 域名不能以点开始或结束
        if (domain.startsWith(".") || domain.endsWith(".")) return false

        // 域名不能包含连续的点
        if (domain.contains("..")) return false

        // 分割域名标签
        val labels = domain.split(".")

        // 至少需要两个标签（如 example.com）
        if (labels.size < 2) return false

        // 检查每个标签
        labels.forEach { label ->
            if (!isValidDomainLabel(label)) return false
        }

        // 顶级域名检查（最后一个标签）
        val tld = labels.last()
        if (!isValidTLD(tld)) return false

        true
    } catch (e: Exception) {
        false
    }
}

fun isValidDomainLabel(label: String): Boolean {
    if (label.isEmpty()) return false
    if (label.length > 63) return false

    // 标签不能以连字符开始或结束
    if (label.startsWith("-") || label.endsWith("-")) return false

    // 标签只能包含字母、数字和连字符
    val pattern = Regex("^[a-zA-Z0-9-]+$")
    return pattern.matches(label)
}

fun isValidTLD(tld: String): Boolean {
    if (tld.isEmpty()) return false
    if (tld.length < 2) return false

    // TLD只能包含字母
    val pattern = Regex("^[a-zA-Z]+$")
    return pattern.matches(tld)
}

// 支持预览的文件类型
val previewableFileTypes = setOf(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "csv", "rtf", "odt", "ods", "odp"
)

// 支持直接显示的文件类型
val displayableFileTypes = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
    "mp4", "webm", "ogg", "mp3", "wav", "m4a"
)

private fun getLastPathSegment(url: String): String? {
    return try {
        // 移除协议部分
        val afterProtocol = url.substringAfter("://")

        // 移除用户信息（如果存在）
        val afterUserInfo = if (afterProtocol.contains("@")) {
            afterProtocol.substringAfter("@")
        } else {
            afterProtocol
        }

        // 查找路径开始位置
        val pathStart = afterUserInfo.indexOf('/')
        if (pathStart == -1) return null

        val pathWithQuery = afterUserInfo.substring(pathStart)

        // 移除查询参数和锚点
        val path = pathWithQuery
            .substringBefore('?')
            .substringBefore('#')

        // 分割路径并获取最后一个非空段
        val segments = path.split('/').filter { it.isNotEmpty() }
        segments.lastOrNull()
    } catch (e: Exception) {
        null
    }
}

fun isFileUrl(url: String): Boolean {
    return try {
        val lastSegment = getLastPathSegment(url) ?: return false

        if (lastSegment.contains('.')) {
            val extension = lastSegment.substringAfterLast('.').lowercase()
            return extension.isNotEmpty() &&
                    (previewableFileTypes.contains(extension) ||
                            displayableFileTypes.contains(extension) ||
                            isCommonFileExtension(extension))
        }

        false
    } catch (e: Exception) {
        false
    }
}

fun getFileExtension(url: String): String {
    return try {
        val lastSegment = getLastPathSegment(url) ?: return ""

        if (lastSegment.contains('.')) {
            lastSegment.substringAfterLast('.').lowercase()
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

private fun isCommonFileExtension(extension: String): Boolean {
    return setOf(
        "zip", "rar", "7z", "tar", "gz",
        "apk", "ipa", "exe", "dmg",
        "iso", "img", "bin"
    ).contains(extension)
}
