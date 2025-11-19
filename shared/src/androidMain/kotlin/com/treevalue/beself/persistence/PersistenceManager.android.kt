package com.treevalue.beself.persistence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.treevalue.beself.platform.AndroidContextProvider
import com.treevalue.beself.values.appFolder
import java.io.File

actual class PersistentStorage actual constructor() {

    /**
     * 获取公共 Documents 目录下的应用文件夹
     * 应用卸载后数据会保留
     */
    private fun getPublicExternalStorageDir(): File? {
        return try {
            // Android 10+ 需要特殊处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 使用 Documents 公共目录
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val appDir = File(documentsDir, appFolder)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                appDir
            } else {
                // Android 9 及以下
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val appDir = File(documentsDir, appFolder)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                appDir
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取应用专用外部存储目录（备用方案）
     * 应用卸载后会被删除
     */
    private fun getAppExternalStorageDir(): File? {
        return try {
            val context = getApplicationContext()
            val appExternalDir = context.getExternalFilesDir("persistent_data")
            if (appExternalDir != null) {
                if (!appExternalDir.exists()) {
                    appExternalDir.mkdirs()
                }
                appExternalDir
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取内部存储备份目录（最后备用方案）
     */
    private fun getInternalBackupDir(): File? {
        return try {
            val context = getApplicationContext()
            val backupDir = File(context.filesDir, "persistent_backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            backupDir
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun saveData(key: String, data: String): Boolean {
        return try {
            var savedToPublic = false

            // 优先尝试保存到公共 Documents 目录
            if (hasExternalStoragePermission()) {
                val publicDir = getPublicExternalStorageDir()
                if (publicDir != null && publicDir.canWrite()) {
                    try {
                        val file = File(publicDir, "$key.json")
                        file.writeText(data)
                        savedToPublic = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 同时保存到应用专用外部存储作为备份
            try {
                val appExternalDir = getAppExternalStorageDir()
                if (appExternalDir != null) {
                    val file = File(appExternalDir, "$key.json")
                    file.writeText(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 同时保存到内部存储作为最后备份
            try {
                val internalDir = getInternalBackupDir()
                if (internalDir != null) {
                    val backupFile = File(internalDir, "$key.json")
                    backupFile.writeText(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 只要成功保存到公共目录就返回true
            savedToPublic
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun loadData(key: String): String? {
        return try {
            // 优先从公共 Documents 目录加载（最持久的存储）
            val publicDir = getPublicExternalStorageDir()
            if (publicDir != null) {
                val file = File(publicDir, "$key.json")
                if (file.exists() && file.canRead()) {
                    return file.readText()
                }
            }

            // 其次从应用专用外部存储加载
            val appExternalDir = getAppExternalStorageDir()
            if (appExternalDir != null) {
                val file = File(appExternalDir, "$key.json")
                if (file.exists()) {
                    return file.readText()
                }
            }

            // 最后从内部存储加载
            val internalDir = getInternalBackupDir()
            if (internalDir != null) {
                val file = File(internalDir, "$key.json")
                if (file.exists()) {
                    return file.readText()
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun removeData(key: String): Boolean {
        return try {
            var success = true

            // 从公共 Documents 目录删除
            val publicDir = getPublicExternalStorageDir()
            if (publicDir != null) {
                val file = File(publicDir, "$key.json")
                if (file.exists()) {
                    success = file.delete() && success
                }
            }

            // 从应用专用外部存储删除
            val appExternalDir = getAppExternalStorageDir()
            if (appExternalDir != null) {
                val file = File(appExternalDir, "$key.json")
                if (file.exists()) {
                    success = file.delete() && success
                }
            }

            // 从内部存储删除
            val internalDir = getInternalBackupDir()
            if (internalDir != null) {
                val file = File(internalDir, "$key.json")
                if (file.exists()) {
                    success = file.delete() && success
                }
            }

            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun exists(key: String): Boolean {
        return try {
            // 优先检查公共 Documents 目录
            val publicDir = getPublicExternalStorageDir()
            if (publicDir != null) {
                val file = File(publicDir, "$key.json")
                if (file.exists()) return true
            }

            // 检查应用专用外部存储
            val appExternalDir = getAppExternalStorageDir()
            if (appExternalDir != null) {
                val file = File(appExternalDir, "$key.json")
                if (file.exists()) return true
            }

            // 检查内部存储
            val internalDir = getInternalBackupDir()
            if (internalDir != null) {
                val file = File(internalDir, "$key.json")
                return file.exists()
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun hasExternalStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 对于 Documents 目录的访问更宽松
                // 不一定需要 MANAGE_EXTERNAL_STORAGE
                try {
                    val publicDir = getPublicExternalStorageDir()
                    publicDir != null && publicDir.canWrite()
                } catch (e: Exception) {
                    false
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val context = getApplicationContext()
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    private fun getApplicationContext(): Context {
        return AndroidContextProvider.getContext()
    }
}

actual class PermissionChecker actual constructor() {

    actual fun hasExternalStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 检查是否可以写入 Documents 目录
                try {
                    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val testDir = File(documentsDir, appFolder)
                    testDir.mkdirs()
                    testDir.canWrite()
                } catch (e: Exception) {
                    false
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val context = AndroidContextProvider.getContext()
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    actual fun requestExternalStoragePermission(callback: (Boolean) -> Unit) {
        try {
            val hasPermission = hasExternalStoragePermission()
            if (hasPermission) {
                callback(true)
            } else {
                // 通知MainActivity需要请求权限
                notifyPermissionNeeded(callback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }

    private fun notifyPermissionNeeded(callback: (Boolean) -> Unit) {
        // 这里可以使用事件总线或其他机制通知UI
        // 暂时直接返回false
        callback(false)
    }
}
