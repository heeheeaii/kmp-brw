package com.treevalue.beself.net

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import java.net.URI

data class DownloadTask(
    val id: String,
    val url: String,
    val fileName: String,
    val status: DownloadStatus,
    val progress: Float,
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

object FileUrlDetector {
    private val downloadableExtensions = setOf(
        // 文档类型
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
        // 压缩文件
        "zip", "rar", "7z", "tar", "gz", "bz2",
        // 图片类型
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico",
        // 音频类型
        "mp3", "wav", "aac", "ogg", "flac", "m4a",
        // 视频类型
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
        // 其他
        "exe", "dmg", "iso", "torrent"
    )

    fun isBlockedFileType(url: String): Boolean {
        return try {
            val uri = URI(url)
            val path = uri.path?.lowercase() ?: return false
            val extension = path.substringAfterLast('.', "")

            val blockedExtensions = setOf("apk", "ipa")
            extension in blockedExtensions
        } catch (e: Exception) {
            false
        }
    }

    fun isDownloadableUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val path = uri.path?.lowercase() ?: return false
            val extension = path.substringAfterLast('.', "")

            // 检查扩展名
            val hasDownloadableExtension = extension in downloadableExtensions

            // 额外检查：如果是图片或媒体文件的直接链接
            val isDirectFileAccess = path.contains('.') &&
                    !path.endsWith('/') &&
                    !path.contains("?") &&
                    !path.contains("#")

            hasDownloadableExtension && isDirectFileAccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从URL提取文件名
     */
    fun extractFilename(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path
            if (path.isNullOrEmpty()) {
                "download"
            } else {
                path.substringAfterLast('/')
                    .takeIf { it.isNotEmpty() && it.contains('.') }
                    ?: "download"
            }
        } catch (e: Exception) {
            "download"
        }
    }

}


class RealDownloadManager(private val scope: CoroutineScope) {

    private val _downloadTasks = mutableStateListOf<DownloadTask>()
    val downloadTasks: List<DownloadTask> = _downloadTasks

    fun pauseDownload(taskId: String) {
        updateDownloadStatus(
            taskId, DownloadStatus.PAUSED,
            _downloadTasks.find { it.id == taskId }?.progress ?: 0f
        )
    }

    fun resumeDownload(taskId: String) {
        updateDownloadStatus(
            taskId, DownloadStatus.DOWNLOADING,
            _downloadTasks.find { it.id == taskId }?.progress ?: 0f
        )
    }

    fun deleteDownload(taskId: String) {
        _downloadTasks.removeAll { it.id == taskId }
    }

    // 添加下载任务
    fun addDownloadTask(task: DownloadTask) :Boolean{
        if (FileUrlDetector.isBlockedFileType(task.url)) {
            return false
        }
        if (!_downloadTasks.any { it.id == task.id }) {
            _downloadTasks.add(task)
            return true
        }
        return false
    }

    // 更新下载任务状态
    fun updateDownloadStatus(taskId: String, status: DownloadStatus, progress: Float) {
        val index = _downloadTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val oldTask = _downloadTasks[index]
            // 创建新的任务对象替换旧的
            _downloadTasks[index] = oldTask.copy(
                status = status,
                progress = progress
            )
        } else {
            // 如果找不到任务，打印调试信息

        }
    }
}
