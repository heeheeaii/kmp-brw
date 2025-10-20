package com.treevalue.beself.persistence

import java.io.File

actual class PersistentStorage actual constructor() {
    private fun getPersistentDir(): File {
        return try {
            // Windows: C:\Users\[username]\AppData\Roaming\HeeApp
            // macOS: ~/Library/Application Support/HeeApp
            // Linux: ~/.config/HeeApp
            val userHome = System.getProperty("user.home")
            val appDataDir = when {
                System.getProperty("os.name").lowercase().contains("win") -> {
                    File(System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming", "HeeApp")
                }

                System.getProperty("os.name").lowercase().contains("mac") -> {
                    File(userHome, "Library/Application Support/HeeApp")
                }

                else -> { // Linux and others
                    File(userHome, ".config/HeeApp")
                }
            }

            if (!appDataDir.exists()) {
                appDataDir.mkdirs()
            }

            appDataDir
        } catch (e: Exception) {
            // 备用方案：使用当前工作目录
            val fallbackDir = File(System.getProperty("user.dir"), ".heeapp_data")
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs()
            }
            fallbackDir
        }
    }

    actual fun saveData(key: String, data: String): Boolean {
        return try {
            val dir = getPersistentDir()
            val file = File(dir, "$key.json")
            file.writeText(data)
            
            true
        } catch (e: Exception) {
            
            false
        }
    }

    actual fun loadData(key: String): String? {
        return try {
            val dir = getPersistentDir()
            val file = File(dir, "$key.json")
            if (file.exists()) {
                
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            
            null
        }
    }

    actual fun removeData(key: String): Boolean {
        return try {
            val dir = getPersistentDir()
            val file = File(dir, "$key.json")
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            
            false
        }
    }

    actual fun exists(key: String): Boolean {
        return try {
            val dir = getPersistentDir()
            val file = File(dir, "$key.json")
            file.exists()
        } catch (e: Exception) {
            
            false
        }
    }
}

actual class PermissionChecker actual constructor() {
    actual fun hasExternalStoragePermission(): Boolean = true

    actual fun requestExternalStoragePermission(callback: (Boolean) -> Unit) {
    }
}
