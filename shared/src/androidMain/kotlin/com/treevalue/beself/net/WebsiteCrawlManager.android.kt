package com.treevalue.beself.net

import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.treevalue.beself.platform.AndroidContextProvider

actual fun openDownloadDirectory() {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath),
                "resource/folder"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // 需要通过Context启动，这里需要从Application获取Context
        AndroidContextProvider.getContext().startActivity(intent)
    } catch (e: Exception) {
        // 如果无法打开文件管理器，可以尝试其他方式
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            AndroidContextProvider.getContext().startActivity(intent)
        } catch (ex: Exception) {
            
        }
    }
}
