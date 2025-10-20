package com.treevalue.beself.net

import com.treevalue.beself.net.getDownloadDirectory
import java.util.Locale

actual fun openDownloadDirectory() {
    try {
        val downloadDir = getDownloadDirectory()
        if (downloadDir.exists()) {
            if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("win")) {
                // Windows
                Runtime.getRuntime().exec("explorer.exe ${downloadDir.absolutePath}")
            } else if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("mac")) {
                // macOS
                Runtime.getRuntime().exec("open ${downloadDir.absolutePath}")
            } else {
                // Linux
                Runtime.getRuntime().exec("xdg-open ${downloadDir.absolutePath}")
            }
        }
    } catch (e: Exception) {
        
    }
}
