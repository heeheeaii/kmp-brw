package com.treevalue.beself.util

import java.io.File

/**
 * 浏览器可预览的文件扩展名白名单
 */
private val BROWSER_PREVIEWABLE_EXTENSIONS = setOf(
    // 文档
    "pdf",

    // 图片
    "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",

    // 网页
    "html", "htm", "xml",

    // 文本
    "txt", "json", "csv",

    // 媒体（部分浏览器支持）
    "mp4", "webm", "ogg", "mp3", "wav"
)

/**
 * 危险/不可预览的文件扩展名黑名单
 */
private val BLOCKED_EXTENSIONS = setOf(
    // 可执行文件
    "exe", "dll", "bat", "cmd", "com", "msi", "scr",

    // 脚本
    "vbs", "ps1", "sh", "bash",

    // 其他危险格式
    "apk", "jar", "deb", "rpm"
)

/**
 * 检测字符串是否为文件路径
 */
fun isFilePath(input: String): Boolean {
    val trimmed = input.trim()

    // Windows 路径检测
    // C:\ 或 D:\ 等盘符开头
    val windowsPattern = Regex("""^[a-zA-Z]:\\.*""")
    if (windowsPattern.matches(trimmed)) {
        return true
    }

    // UNC 路径 \\server\share
    if (trimmed.startsWith("\\\\")) {
        return true
    }

    // Unix/Linux/Mac 绝对路径
    if (trimmed.startsWith("/")) {
        return true
    }

    // 相对路径（包含 ./ 或 ../)
    if (trimmed.startsWith("./") || trimmed.startsWith("../")) {
        return true
    }

    // file:// 协议
    if (trimmed.startsWith("file://")) {
        return true
    }

    return false
}

/**
 * 验证文件路径的合法性
 * @return Pair<Boolean, String?> - (是否合法, 错误信息)
 */
fun validateFilePath(path: String): Pair<Boolean, String?> {
    val trimmed = path.trim()

    // 如果是 file:// 协议，先转换为普通路径
    val actualPath = if (trimmed.startsWith("file://")) {
        trimmed.removePrefix("file://").removePrefix("/")
    } else {
        trimmed
    }

    val file = File(actualPath)

    // 检查文件是否存在
    if (!file.exists()) {
        return Pair(false, "文件不存在")
    }

    // 检查是否为目录
    if (file.isDirectory) {
        return Pair(false, "不支持打开文件夹")
    }

    // 检查是否可读
    if (!file.canRead()) {
        return Pair(false, "文件无读取权限")
    }

    // 获取文件扩展名
    val extension = file.extension.lowercase()

    // 检查是否在黑名单中
    if (extension in BLOCKED_EXTENSIONS) {
        return Pair(false, "不支持打开 .$extension 类型文件")
    }

    // 检查是否在白名单中
    if (extension !in BROWSER_PREVIEWABLE_EXTENSIONS) {
        return Pair(false, "浏览器无法预览 .$extension 文件")
    }

    return Pair(true, null)
}

/**
 * 将文件路径转换为 file:// URL
 */
fun convertToFileUrl(path: String): String {
    val trimmed = path.trim()

    // 如果已经是 file:// 格式，直接返回
    if (trimmed.startsWith("file://")) {
        return trimmed
    }

    val file = File(trimmed)
    val absolutePath = file.absolutePath

    // 检测操作系统
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("win") -> {
            // Windows: file:///C:/path/to/file.pdf
            "file:///${absolutePath.replace("\\", "/")}"
        }
        else -> {
            // Unix/Linux/Mac: file:///path/to/file.pdf
            "file://$absolutePath"
        }
    }
}

/**
 * 获取文件的友好显示名称
 */
fun getFileDisplayName(path: String): String {
    val file = File(path.trim().removePrefix("file://").removePrefix("/"))
    return file.name
}

/**
 * 检测文件类型的图标提示
 */
fun getFileTypeEmoji(path: String): String {
    val extension = File(path).extension.lowercase()
    return when (extension) {
        "pdf" -> "📄"
        in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") -> "🖼️"
        in setOf("html", "htm") -> "🌐"
        in setOf("txt", "json", "csv", "xml") -> "📝"
        in setOf("mp4", "webm", "ogg") -> "🎬"
        in setOf("mp3", "wav") -> "🎵"
        else -> "📁"
    }
}
