package com.treevalue.beself.persistence

import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json

expect class PersistentStorage() {
    fun saveData(key: String, data: String): Boolean
    fun loadData(key: String): String?
    fun removeData(key: String): Boolean
    fun exists(key: String): Boolean
}

expect class PermissionChecker() {
    fun hasExternalStoragePermission(): Boolean
    fun requestExternalStoragePermission(callback: (Boolean) -> Unit)
}

class PersistenceManager {
    private val persistentStorage = PersistentStorage()
    private val settings: Settings = Settings() // multiplatform-settings 作为备用方案
    private val permissionChecker = PermissionChecker()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    companion object {
        private const val KEY_BROWSER_STATE = "browser_state_v2"
        private const val KEY_BROWSER_STATE_BACKUP = "browser_state_backup_v2" // 备用方案的key
        private const val KEY_PRIMARY_STORAGE_FAILED = "primary_storage_failed"
    }

    fun saveBrowserState(state: BrowserState) {
        try {
            val jsonString = json.encodeToString(BrowserState.serializer(), state)

            // 检查外部存储权限
            if (permissionChecker.hasExternalStoragePermission()) {
                val primarySuccess = persistentStorage.saveData(KEY_BROWSER_STATE, jsonString)
                if (primarySuccess) {
                    settings.remove(KEY_PRIMARY_STORAGE_FAILED)
                    saveToBackupStorage(jsonString)
                    return
                }
            } else {
                
            }

            // 外部存储失败或无权限，使用备用存储
            
            settings[KEY_PRIMARY_STORAGE_FAILED] = true
            val backupSuccess = saveToBackupStorage(jsonString)
            if (backupSuccess) {
                
            } else {
                
            }
        } catch (e: Exception) {
            
            e.printStackTrace()
            tryBackupSave(state)
        }
    }

    fun loadBrowserState(): BrowserState? {
        // 检查权限后再决定是否尝试主存储
        val hasPermission = permissionChecker.hasExternalStoragePermission()
        val primaryStorageFailed = settings.getBoolean(KEY_PRIMARY_STORAGE_FAILED, false)

        if (!primaryStorageFailed && hasPermission) {
            val primaryResult = loadFromPrimaryStorage()
            if (primaryResult != null) {
                return primaryResult
            }
            
            settings[KEY_PRIMARY_STORAGE_FAILED] = true
        } else {
            if (!hasPermission) {
                
            }
        }

        return loadFromBackupStorage()
    }

    // 添加权限请求方法
    fun requestPermissionAndSave(state: BrowserState) {
        if (permissionChecker.hasExternalStoragePermission()) {
            saveBrowserState(state)
        } else {
            permissionChecker.requestExternalStoragePermission { granted ->
                if (granted) {
                    settings.remove(KEY_PRIMARY_STORAGE_FAILED)
                    saveBrowserState(state)
                } else {
                    
                    settings[KEY_PRIMARY_STORAGE_FAILED] = true
                    saveBrowserState(state)
                }
            }
        }
    }

    // 添加权限状态检查方法
    fun checkAndMigrateToExternalStorage(): Boolean {
        if (!permissionChecker.hasExternalStoragePermission()) {
            return false
        }

        // 有权限了，尝试迁移数据
        return migrateFromBackupToPrimary()
    }

    // 修改重置存储策略方法
    fun resetStorageStrategy() {
        if (permissionChecker.hasExternalStoragePermission()) {
            settings.remove(KEY_PRIMARY_STORAGE_FAILED)
        } else {
            
        }
    }

    // 添加获取存储状态的方法
    fun getStorageStatus(): String {
        val hasPermission = permissionChecker.hasExternalStoragePermission()
        val primaryFailed = settings.getBoolean(KEY_PRIMARY_STORAGE_FAILED, false)

        return when {
            !hasPermission -> "无外部存储权限，使用备用存储"
            primaryFailed -> "外部存储失败，使用备用存储"
            else -> "使用外部存储"
        }
    }
    private fun saveToBackupStorage(jsonString: String): Boolean {
        return try {
            settings[KEY_BROWSER_STATE_BACKUP] = jsonString
            true
        } catch (e: Exception) {
            
            false
        }
    }

    private fun tryBackupSave(state: BrowserState) {
        try {
            val jsonString = json.encodeToString(BrowserState.serializer(), state)
            settings[KEY_BROWSER_STATE_BACKUP] = jsonString
            settings[KEY_PRIMARY_STORAGE_FAILED] = true
            
        } catch (e: Exception) {
            
        }
    }

    private fun loadFromPrimaryStorage(): BrowserState? {
        return try {
            val jsonString = persistentStorage.loadData(KEY_BROWSER_STATE)
            if (jsonString != null) {
                json.decodeFromString(BrowserState.serializer(), jsonString)
            } else {
                
                null
            }
        } catch (e: Exception) {
            
            null
        }
    }

    private fun loadFromBackupStorage(): BrowserState? {
        return try {
            val jsonString: String? = settings[KEY_BROWSER_STATE_BACKUP]
            if (jsonString != null) {
                
                json.decodeFromString(BrowserState.serializer(), jsonString)
            } else {
                
                null
            }
        } catch (e: Exception) {
            
            e.printStackTrace()
            null
        }
    }

    fun clearBrowserState() {
        var primaryCleared = false
        var backupCleared = false

        // 清除主存储
        try {
            primaryCleared = persistentStorage.removeData(KEY_BROWSER_STATE)
            if (primaryCleared) {
                
            }
        } catch (e: Exception) {
            
        }

        // 清除备用存储
        try {
            settings.remove(KEY_BROWSER_STATE_BACKUP)
            backupCleared = true
            
        } catch (e: Exception) {
            
        }

        // 清除失败标记
        settings.remove(KEY_PRIMARY_STORAGE_FAILED)

        if (!primaryCleared && !backupCleared) {
            
        }
    }

    fun debugPrintSavedState() {
        

        // 检查主存储
        val primaryJsonString = try {
            persistentStorage.loadData(KEY_BROWSER_STATE)
        } catch (e: Exception) {
            
            null
        }

        if (primaryJsonString != null) {
            
        } else {
            
        }

        // 检查备用存储
        val backupJsonString: String? = try {
            settings[KEY_BROWSER_STATE_BACKUP]
        } catch (e: Exception) {
            
            null
        }

        if (backupJsonString != null) {
            
        } else {
            
        }

        // 检查失败标记
        val primaryFailed = settings.getBoolean(KEY_PRIMARY_STORAGE_FAILED, false)
        

        
    }

    fun hasPersistentData(): Boolean {
        // 检查主存储
        val primaryHasData = try {
            persistentStorage.exists(KEY_BROWSER_STATE)
        } catch (e: Exception) {
            false
        }

        // 检查备用存储
        val backupHasData = try {
            settings.contains(KEY_BROWSER_STATE_BACKUP)
        } catch (e: Exception) {
            false
        }

        return primaryHasData || backupHasData
    }

    /**
     * 获取当前使用的存储类型
     */
    fun getCurrentStorageType(): String {
        val primaryFailed = settings.getBoolean(KEY_PRIMARY_STORAGE_FAILED, false)
        return if (primaryFailed) "备用存储(multiplatform-settings)" else "主存储(PersistentStorage)"
    }

    /**
     * 数据迁移：从备用存储迁移到主存储（当主存储恢复正常时）
     */
    fun migrateFromBackupToPrimary(): Boolean {
        try {
            val backupData: String? = settings[KEY_BROWSER_STATE_BACKUP]
            if (backupData != null) {
                val success = persistentStorage.saveData(KEY_BROWSER_STATE, backupData)
                if (success) {
                    
                    settings.remove(KEY_PRIMARY_STORAGE_FAILED)
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            
            return false
        }
    }
}
