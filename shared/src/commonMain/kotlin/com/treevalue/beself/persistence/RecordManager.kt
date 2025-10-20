package com.treevalue.beself.persistence

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RecordManager {
    private val settings: Settings = Settings()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // 内存缓存
    private var cachedDeleteRecordState: DeleteRecordState? = null
    private var cacheInitialized = false

    private companion object {
        private const val KEY_DELETE_RECORDS = "delete_records"
    }

    private suspend fun loadDeleteRecordsFromPersistent(): DeleteRecordState = withContext(Dispatchers.Default) {
        try {
            val jsonString = PersistentDeleteStorage.loadDeleteRecords()
            if (!jsonString.isNullOrBlank()) {
                
                return@withContext json.decodeFromString<DeleteRecordState>(jsonString)
            }
        } catch (e: Exception) {
            
        }
        return@withContext DeleteRecordState()
    }

    private suspend fun loadDeleteRecordsFromSettings(): DeleteRecordState = withContext(Dispatchers.Default) {
        try {
            val jsonString = settings.getStringOrNull(KEY_DELETE_RECORDS)
            if (jsonString != null) {
                
                return@withContext json.decodeFromString<DeleteRecordState>(jsonString)
            }
        } catch (e: Exception) {
            
        }
        return@withContext DeleteRecordState()
    }

    // 保存删除记录到持久化存储
    private suspend fun saveDeleteRecordsToDisk(state: DeleteRecordState): Boolean =
        withContext(Dispatchers.Default) {
            try {
                val jsonString = json.encodeToString(state)
                val success = PersistentDeleteStorage.saveDeleteRecords(jsonString)
                if (success) {
                    
                } else {
                    
                }
                return@withContext success
            } catch (e: Exception) {
                
                return@withContext false
            }
        }

    // 保存删除记录到Settings（备用方案）
    private suspend fun saveDeleteRecordsToSettings(state: DeleteRecordState): Boolean =
        withContext(Dispatchers.Default) {
            try {
                val jsonString = json.encodeToString(state)
                settings.putString(KEY_DELETE_RECORDS, jsonString)
                
                return@withContext true
            } catch (e: Exception) {
                
                return@withContext false
            }
        }

    // 更新缓存
    private fun updateCache(state: DeleteRecordState) {
        cachedDeleteRecordState = state
        cacheInitialized = true
    }

    // 初始化缓存（如果还未初始化）
    private suspend fun ensureCacheInitialized(): DeleteRecordState {
        if (!cacheInitialized || cachedDeleteRecordState == null) {
            val state = loadDeleteRecords()
            updateCache(state)
            return state
        }
        return cachedDeleteRecordState!!
    }

    // 主要的加载方法
    suspend fun loadDeleteRecords(): DeleteRecordState = withContext(Dispatchers.Default) {
        // 首先尝试从持久化存储加载
        val persistentState = loadDeleteRecordsFromPersistent()

        // 如果持久化存储为空，但Settings中有记录，则进行数据迁移
        if (persistentState.records.isEmpty()) {
            val settingsState = loadDeleteRecordsFromSettings()
            if (settingsState.records.isNotEmpty()) {
                
                val migrationSuccess = saveDeleteRecordsToDisk(settingsState)
                if (migrationSuccess) {
                    
                }
                // 更新缓存
                updateCache(settingsState)
                return@withContext settingsState
            }
        }

        // 更新缓存
        updateCache(persistentState)
        return@withContext persistentState
    }

    // 主要的保存方法
    suspend fun saveDeleteRecords(state: DeleteRecordState) = withContext(Dispatchers.Default) {
        // 优先保存到持久化存储
        val persistentSuccess = saveDeleteRecordsToDisk(state)

        // 同时也保存到Settings作为备用
        val settingsSuccess = saveDeleteRecordsToSettings(state)

        when {
            persistentSuccess && settingsSuccess -> {
                
            }

            persistentSuccess -> {
                
            }

            settingsSuccess -> {
                
            }

            else -> {
                
            }
        }

        // 更新缓存
        updateCache(state)
    }

    suspend fun recordDelete(hostOrId: String): DeleteRecord = withContext(Dispatchers.Default) {
        val currentState = loadDeleteRecords()
        val existingRecord = currentState.records.find { it.hostOrId == hostOrId }

        val newRecord = existingRecord?.copy(
            deleteCount = existingRecord.deleteCount + 1,
            lastDeleteTime = System.currentTimeMillis()
        )
            ?: DeleteRecord(
                hostOrId = hostOrId,
                deleteCount = 1,
                lastDeleteTime = System.currentTimeMillis()
            )

        val updatedRecords = currentState.records.filter { it.hostOrId != hostOrId } + newRecord
        val newState = currentState.copy(records = updatedRecords)
        saveDeleteRecords(newState)

        
        newRecord
    }

    suspend fun canAddSite(hostOrId: String): Boolean = withContext(Dispatchers.Default) {
        val currentState = loadDeleteRecords()
        val record = currentState.records.find { it.hostOrId == hostOrId } ?: return@withContext true

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - record.lastDeleteTime

        when (record.deleteCount) {
            1 -> {
                // 一个月 = 30 * 24 * 60 * 60 * 1000
                val oneMonth = 30L * 24 * 60 * 60 * 1000
                timeDiff >= oneMonth
            }

            2 -> {
                // 半年 = 6 * 30 * 24 * 60 * 60 * 1000
                val sixMonths = 6L * 30 * 24 * 60 * 60 * 1000
                timeDiff >= sixMonths
            }

            else -> {
                // 3次或以上，永远无法添加
                false
            }
        }
    }

    /**
     * 快速检查是否可以添加站点（使用内存缓存，避免IO操作）
     * @param hostOrId 主机或ID
     * @return 是否可以添加
     */
    suspend fun canAddSiteQuick(hostOrId: String): Boolean = withContext(Dispatchers.Default) {
        val currentState = ensureCacheInitialized()
        val record = currentState.records.find { it.hostOrId == hostOrId } ?: return@withContext true

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - record.lastDeleteTime

        when (record.deleteCount) {
            1 -> {
                // 一个月 = 30 * 24 * 60 * 60 * 1000
                val oneMonth = 30L * 24 * 60 * 60 * 1000
                timeDiff >= oneMonth
            }

            2 -> {
                // 半年 = 6 * 30 * 24 * 60 * 60 * 1000
                val sixMonths = 6L * 30 * 24 * 60 * 60 * 1000
                timeDiff >= sixMonths
            }

            else -> {
                // 3次或以上，永远无法添加
                false
            }
        }
    }

    suspend fun getDeleteRestriction(hostOrId: String): DeleteRestriction = withContext(Dispatchers.Default) {
        val currentState = loadDeleteRecords()
        val record = currentState.records.find { it.hostOrId == hostOrId }

        if (record == null || record.deleteCount == 0) {
            return@withContext DeleteRestriction.NONE
        }

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - record.lastDeleteTime

        return@withContext when (record.deleteCount) {
            1 -> {
                val oneMonth = 30L * 24 * 60 * 60 * 1000
                if (timeDiff >= oneMonth) {
                    DeleteRestriction.NONE
                } else {
                    val remainingTime = oneMonth - timeDiff
                    val days = remainingTime / (24 * 60 * 60 * 1000)
                    val hours = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                    DeleteRestriction(
                        "此网站已被删除1次，还需等待 ${days}天${hours}小时 才能再次添加"
                    )
                }
            }

            2 -> {
                val sixMonths = 6L * 30 * 24 * 60 * 60 * 1000
                if (timeDiff >= sixMonths) {
                    DeleteRestriction.NONE
                } else {
                    val remainingTime = sixMonths - timeDiff
                    val days = remainingTime / (24 * 60 * 60 * 1000)
                    DeleteRestriction(
                        "此网站已被删除2次，还需等待 ${days}天 才能再次添加"
                    )
                }
            }

            else -> DeleteRestriction.PERMANENT
        }
    }

    /**
     * 手动刷新缓存（从存储重新加载数据）
     */
    suspend fun refreshCache() = withContext(Dispatchers.Default) {
        
        cacheInitialized = false
        cachedDeleteRecordState = null
        loadDeleteRecords()
    }
}
