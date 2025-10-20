package com.treevalue.beself.persistence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.treevalue.beself.platform.AndroidContextProvider
import java.io.File

actual class PersistentStorage actual constructor() {
    private fun getExternalStorageDir(): File? {
        return try {
            val context = getApplicationContext()
            // 使用应用专用的外部存储目录，不需要权限且应用卸载后会删除
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
            
            null
        }
    }

    // 如果确实需要在公共目录保存（应用卸载后保留），添加此方法
    private fun getPublicExternalStorageDir(): File? {
        return try {
            // 只有在明确获得 MANAGE_EXTERNAL_STORAGE 权限时才使用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()
            ) {
                return null
            }

            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(publicDir, "HeeAppData")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            appDir
        } catch (e: Exception) {
            
            null
        }
    }

    private fun getInternalBackupDir(): File? {
        return try {
            // 备用方案：使用内部存储
            val context = getApplicationContext() // 需要实现获取Context的方法
            val backupDir = File(context.filesDir, "persistent_backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            backupDir
        } catch (e: Exception) {
            
            null
        }
    }

    actual fun saveData(key: String, data: String): Boolean {
        // 在保存前检查权限
        if (!hasExternalStoragePermission()) {
            
            return saveToInternalOnly(key, data)
        }

        return try {
            val externalDir = getExternalStorageDir()
            if (externalDir != null) {
                val file = File(externalDir, "$key.json")
                file.writeText(data)

                // 同时保存备份
                val internalDir = getInternalBackupDir()
                if (internalDir != null) {
                    val backupFile = File(internalDir, "$key.json")
                    backupFile.writeText(data)
                }
                true
            } else {
                
                saveToInternalOnly(key, data)
            }
        } catch (e: Exception) {
            
            saveToInternalOnly(key, data)
        }
    }

    private fun hasExternalStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
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

    private fun saveToInternalOnly(key: String, data: String): Boolean {
        return try {
            val internalDir = getInternalBackupDir()
            if (internalDir != null) {
                val file = File(internalDir, "$key.json")
                file.writeText(data)
                
                false // 返回false表示未保存到外部存储
            } else {
                false
            }
        } catch (e: Exception) {
            
            false
        }
    }

    actual fun loadData(key: String): String? {
        return try {
            // 首先尝试从外部存储加载
            val externalDir = getExternalStorageDir()
            if (externalDir != null) {
                val file = File(externalDir, "$key.json")
                if (file.exists()) {
                    
                    return file.readText()
                }
            }

            // 如果外部存储没有，尝试从内部存储加载
            val internalDir = getInternalBackupDir()
            if (internalDir != null) {
                val file = File(internalDir, "$key.json")
                if (file.exists()) {
                    
                    return file.readText()
                }
            }

            null
        } catch (e: Exception) {
            
            null
        }
    }

    actual fun removeData(key: String): Boolean {
        return try {
            var success = true

            // 从外部存储删除
            val externalDir = getExternalStorageDir()
            if (externalDir != null) {
                val file = File(externalDir, "$key.json")
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
            
            false
        }
    }

    actual fun exists(key: String): Boolean {
        return try {
            // 检查外部存储
            val externalDir = getExternalStorageDir()
            if (externalDir != null) {
                val file = File(externalDir, "$key.json")
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
            
            false
        }
    }

    // 需要在Android平台实现获取Context的方法
    private fun getApplicationContext(): Context {
        return AndroidContextProvider.getContext()
    }
}

actual class PermissionChecker actual constructor() {

    actual fun hasExternalStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
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
        // 由于这是在后台服务中，无法直接请求权限
        // 需要通过事件总线通知UI层请求权限
        try {
            val hasPermission = hasExternalStoragePermission()
            if (hasPermission) {
                callback(true)
            } else {
                // 通知MainActivity需要请求权限
                notifyPermissionNeeded(callback)
            }
        } catch (e: Exception) {
            
            callback(false)
        }
    }

    private fun notifyPermissionNeeded(callback: (Boolean) -> Unit) {
        // 这里可以使用事件总线或其他机制通知UI
        // 暂时直接返回false
        
        callback(false)
    }
}
