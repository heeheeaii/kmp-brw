package com.treevalue.beself.persistence

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.treevalue.beself.data.PersistentProgressItem
import com.treevalue.beself.data.PersistentScheduleItem
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@Serializable
data class ScheduleState(
    val schedules: List<PersistentScheduleItem> = emptyList(),
    val progressItems: List<PersistentProgressItem> = emptyList()
)

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
    private val settings: Settings = Settings()
    private val permissionChecker = PermissionChecker()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    companion object {
        private const val KEY_BROWSER_STATE = "browser_state_v2"
        private const val KEY_BROWSER_STATE_BACKUP = "browser_state_backup_v2"
        private const val KEY_PRIMARY_STORAGE_FAILED = "primary_storage_failed"

        private const val KEY_SCHEDULE_STATE = "schedule_state_v1"
        private const val KEY_SCHEDULE_STATE_BACKUP = "schedule_state_backup_v1"
        private const val KEY_SCHEDULE_STORAGE_FAILED = "schedule_storage_failed"
    }

    /**
     * 存储配置
     */
    private data class StorageConfig<T>(
        val primaryKey: String,
        val backupKey: String,
        val failedFlagKey: String,
        val serializer: KSerializer<T>,
        val dataTypeName: String
    )

    private val scheduleConfig = StorageConfig(
        primaryKey = KEY_SCHEDULE_STATE,
        backupKey = KEY_SCHEDULE_STATE_BACKUP,
        failedFlagKey = KEY_SCHEDULE_STORAGE_FAILED,
        serializer = ScheduleState.serializer(),
        dataTypeName = "日程"
    )

    private val browserConfig = StorageConfig(
        primaryKey = KEY_BROWSER_STATE,
        backupKey = KEY_BROWSER_STATE_BACKUP,
        failedFlagKey = KEY_PRIMARY_STORAGE_FAILED,
        serializer = BrowserState.serializer(),
        dataTypeName = "浏览器状态"
    )

    fun saveScheduleState(state: ScheduleState) {
        saveState(state, scheduleConfig)
    }

    fun loadScheduleState(): ScheduleState? {
        return loadState(scheduleConfig)
    }

    fun saveBrowserState(state: BrowserState) {
        saveState(state, browserConfig)
    }

    fun loadBrowserState(): BrowserState? {
        return loadState(browserConfig)
    }

    fun requestPermissionAndSave(state: BrowserState) {
        if (permissionChecker.hasExternalStoragePermission()) {
            saveBrowserState(state)
        } else {
            permissionChecker.requestExternalStoragePermission { granted ->
                settings[browserConfig.failedFlagKey] = !granted
                if (granted) {
                    settings.remove(browserConfig.failedFlagKey)
                }
                saveBrowserState(state)
            }
        }
    }

    fun resetStorageStrategy() {
        if (permissionChecker.hasExternalStoragePermission()) {
            settings.remove(KEY_PRIMARY_STORAGE_FAILED)
        }
    }

    private fun <T> saveState(state: T, config: StorageConfig<T>) {
        try {
            val jsonString = json.encodeToString(config.serializer, state)

            // 检查外部存储权限
            if (permissionChecker.hasExternalStoragePermission()) {
                val primarySuccess = persistentStorage.saveData(config.primaryKey, jsonString)
                if (primarySuccess) {
                    settings.remove(config.failedFlagKey)
                    saveToBackupStorage(jsonString, config.backupKey)
                    KLogger.dd { "${config.dataTypeName}已保存到主存储" }
                    return
                }
            }

            // 主存储失败，标记并使用备用存储
            settings[config.failedFlagKey] = true
            saveToBackupStorage(jsonString, config.backupKey)
            KLogger.dd { "${config.dataTypeName}已保存到备用存储" }
        } catch (e: Exception) {
            KLogger.de { "保存${config.dataTypeName}失败: ${e.message}" }
            e.printStackTrace()
            tryBackupSave(state, config)
        }
    }

    private fun <T> loadState(config: StorageConfig<T>): T? {
        val hasPermission = permissionChecker.hasExternalStoragePermission()
        val primaryStorageFailed = settings.getBoolean(config.failedFlagKey, false)

        // 如果主存储未失败且有权限，先尝试主存储
        if (!primaryStorageFailed && hasPermission) {
            val primaryResult = loadFromPrimaryStorage(config)
            if (primaryResult != null) {
                KLogger.dd { "从主存储加载了${config.dataTypeName}" }
                return primaryResult
            }

            // 主存储失败，标记并尝试备用存储
            settings[config.failedFlagKey] = true
        }

        // 从备用存储加载
        val backupResult = loadFromBackupStorage(config)
        if (backupResult != null) {
            KLogger.dd { "从备用存储加载了${config.dataTypeName}" }
        }
        return backupResult
    }

    private fun saveToBackupStorage(jsonString: String, backupKey: String): Boolean {
        return try {
            settings[backupKey] = jsonString
            true
        } catch (e: Exception) {
            KLogger.de { "备用存储保存失败: ${e.message}" }
            false
        }
    }

    private fun <T> tryBackupSave(state: T, config: StorageConfig<T>) {
        try {
            val jsonString = json.encodeToString(config.serializer, state)
            settings[config.backupKey] = jsonString
            settings[config.failedFlagKey] = true
        } catch (e: Exception) {
            KLogger.de { "备用保存${config.dataTypeName}失败: ${e.message}" }
            e.printStackTrace()
        }
    }

    private fun <T> loadFromPrimaryStorage(config: StorageConfig<T>): T? {
        return try {
            val jsonString = persistentStorage.loadData(config.primaryKey)
            if (jsonString != null) {
                json.decodeFromString(config.serializer, jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            KLogger.de { "从主存储加载${config.dataTypeName}失败: ${e.message}" }
            null
        }
    }

    private fun <T> loadFromBackupStorage(config: StorageConfig<T>): T? {
        return try {
            val jsonString: String? = settings[config.backupKey]
            if (jsonString != null) {
                json.decodeFromString(config.serializer, jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            KLogger.de { "从备用存储加载${config.dataTypeName}失败: ${e.message}" }
            null
        }
    }
}
