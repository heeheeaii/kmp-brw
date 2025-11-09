package com.treevalue.beself.backend

import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.de

/**
 * 进度项：未固定上限 10，固定项不限
 */
@Serializable
data class PersistentProgressItem(
    val id: String,
    val content: String,
    val createdAt: Long,
    val pinned: Boolean,
)

class ProgressBackend private constructor(
    private val scope: CoroutineScope,
) {
    companion object {
        @Volatile
        private var INSTANCE: ProgressBackend? = null

        private const val KEY_PROGRESS = "progress_data_v1"

        fun getInstance(scope: CoroutineScope): ProgressBackend {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProgressBackend(scope).also { INSTANCE = it }
            }
        }

        fun getInstance(): ProgressBackend? = INSTANCE
    }

    private val settings: Settings = Settings()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _items = mutableStateListOf<PersistentProgressItem>()
    val items: List<PersistentProgressItem> = _items

    init {
        load()
    }

    fun getAll(): List<PersistentProgressItem> = _items.toList()

    /**
     * 新增进展
     * - 未固定：若未固定数量已达 10，自动删最老未固定后再插入
     * - 固定：不限量
     * - 新项默认插入到列表头部
     */
    fun add(content: String, pinned: Boolean) {
        val now = System.currentTimeMillis()
        val newItem = PersistentProgressItem(
            id = java.util.UUID.randomUUID().toString(),
            content = content,
            createdAt = now,
            pinned = pinned
        )

        if (!pinned) {
            val unpinned = _items.filter { !it.pinned }.sortedBy { it.createdAt }
            if (unpinned.size >= 10) {
                val oldest = unpinned.firstOrNull()
                if (oldest != null) {
                    _items.removeAll { it.id == oldest.id }
                }
            }
        }
        _items.add(0, newItem)
        save()
    }

    /**
     * 更新进展：可改 content 与 pinned
     */
    fun update(id: String, content: String? = null, pinned: Boolean? = null) {
        val idx = _items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = _items[idx]
            val updated = old.copy(
                content = content ?: old.content,
                pinned = pinned ?: old.pinned
            )
            _items[idx] = updated

            // 若改为未固定且超过上限，立即执行上限策略
            enforceUnpinnedLimitIfNeeded()
            save()
        }
    }

    fun delete(id: String) {
        _items.removeAll { it.id == id }
        save()
    }

    fun batchDelete(ids: Set<String>) {
        if (ids.isEmpty()) return
        _items.removeAll { it.id in ids }
        save()
    }

    private fun enforceUnpinnedLimitIfNeeded() {
        val unpinnedSorted = _items.filter { !it.pinned }.sortedBy { it.createdAt }
        val overflow = unpinnedSorted.size - 10
        if (overflow > 0) {
            val toRemove = unpinnedSorted.take(overflow).map { it.id }.toSet()
            _items.removeAll { it.id in toRemove }
        }
    }

    private fun save() {
        try {
            val encoded = json.encodeToString(_items.toList())
            settings.putString(KEY_PROGRESS, encoded)
        } catch (e: Exception) {
            KLogger.de { "保存进度失败: ${e.message}" }
        }
    }

    private fun load() {
        try {
            val txt = settings.getStringOrNull(KEY_PROGRESS) ?: return
            val list = json.decodeFromString<List<PersistentProgressItem>>(txt)
            _items.clear()
            _items.addAll(list)
            // 保险：加载后也执行一次未固定上限约束
            enforceUnpinnedLimitIfNeeded()
        } catch (e: Exception) {
            KLogger.de { "加载进度失败: ${e.message}" }
        }
    }
}
