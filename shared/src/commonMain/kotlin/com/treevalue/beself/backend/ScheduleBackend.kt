package com.treevalue.beself.backend

import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.Settings
import com.treevalue.beself.ui.ScheduleItem
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class PersistentScheduleItem(
    val id: String,
    val name: String,
    val note: String,
    val startTime: String,
    val endTime: String,
    val type: String,
    val repeatMode: String,
    val weekDays: List<String>,
    val cyclicTasks: List<PersistentCyclicTask>,
)

@Serializable
data class PersistentCyclicTask(
    val id: String,
    val name: String,
    val duration: Int,
)

// 语音播报接口
expect class TextToSpeechEngine() {
    fun initialize(onInitialized: (Boolean) -> Unit)
    fun speak(text: String)
    fun shutdown()
}

class ScheduleBackend private constructor(
    private val scope: CoroutineScope,
) {
    companion object {
        @Volatile
        private var INSTANCE: ScheduleBackend? = null

        private const val KEY_SCHEDULES = "schedules_data_v1"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val MIN_CHECK_INTERVAL = 15000L // 最小15秒
        private const val MAX_CHECK_INTERVAL = 3600000L // 最大1小时

        fun getInstance(scope: CoroutineScope): ScheduleBackend {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleBackend(scope).also { INSTANCE = it }
            }
        }

        fun getInstance(): ScheduleBackend? {
            return INSTANCE
        }
    }

    private val settings: Settings = Settings()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    // 日程列表
    private val _schedules = mutableStateListOf<ScheduleItem>()
    val schedules: List<ScheduleItem> = _schedules

    // 语音播报引擎
    private val ttsEngine = TextToSpeechEngine()
    private var isTtsReady = false
    private var _isTtsEnabled = true

    // 后台监控任务
    private var monitorJob: Job? = null
    private val notifiedSchedules = mutableSetOf<String>() // 已播报的日程ID（开始时间）
    private val notifiedEndSchedules = mutableSetOf<String>() // 已播报的日程ID（结束时间）

    // 动态检查间隔
    private var currentCheckInterval = MIN_CHECK_INTERVAL

    init {
        // 初始化语音引擎
        ttsEngine.initialize { success ->
            isTtsReady = success
            if (!success) {
                KLogger.de { "语音引擎初始化失败" }
            }
        }
        _isTtsEnabled = settings.getBoolean(KEY_TTS_ENABLED, true)

        // 加载数据
        loadSchedules()

        // 启动后台监控
        startBackgroundMonitor()
    }

    fun isTtsEnabled(): Boolean {
        return _isTtsEnabled
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled = enabled
        settings.putBoolean(KEY_TTS_ENABLED, enabled)
        KLogger.dd { "语音播报已${if (enabled) "开启" else "关闭"}" }
    }

    /**
     * 计算最优检查间隔
     * 根据未来24小时内的日程密度动态调整
     */
    private fun calculateOptimalCheckInterval(): Long {
        val now = LocalDateTime.now()
        val next24Hours = now.plusHours(24)

        // 获取未来24小时内的所有日程事件（开始和结束时间）
        val upcomingEvents = mutableListOf<LocalDateTime>()

        _schedules.forEach { schedule ->
            // 考虑重复模式
            val isApplicable = when (schedule.repeatMode) {
                com.treevalue.beself.ui.RepeatMode.ONCE -> {
                    schedule.startTime.isAfter(now) && schedule.startTime.isBefore(next24Hours)
                }

                com.treevalue.beself.ui.RepeatMode.DAILY -> true
                com.treevalue.beself.ui.RepeatMode.SPECIFIC_DAYS -> {
                    schedule.weekDays.contains(now.dayOfWeek) || schedule.weekDays.contains(now.plusDays(1).dayOfWeek)
                }
            }

            if (isApplicable) {
                val startTime = schedule.startTime.toLocalTime()
                val endTime = schedule.endTime.toLocalTime()

                // 添加今天的时间点
                val todayStart = now.toLocalDate().atTime(startTime)
                val todayEnd = now.toLocalDate().atTime(endTime)
                if (todayStart.isAfter(now) && todayStart.isBefore(next24Hours)) {
                    upcomingEvents.add(todayStart)
                }
                if (todayEnd.isAfter(now) && todayEnd.isBefore(next24Hours)) {
                    upcomingEvents.add(todayEnd)
                }

                // 添加明天的时间点（如果是每日或特定日）
                if (schedule.repeatMode != com.treevalue.beself.ui.RepeatMode.ONCE) {
                    val tomorrowStart = now.toLocalDate().plusDays(1).atTime(startTime)
                    val tomorrowEnd = now.toLocalDate().plusDays(1).atTime(endTime)
                    if (tomorrowStart.isBefore(next24Hours)) {
                        upcomingEvents.add(tomorrowStart)
                    }
                    if (tomorrowEnd.isBefore(next24Hours)) {
                        upcomingEvents.add(tomorrowEnd)
                    }
                }
            }
        }

        if (upcomingEvents.isEmpty()) {
            // 没有即将到来的日程，使用最大间隔
            return MAX_CHECK_INTERVAL
        }

        // 找到最近的事件
        val nearestEvent = upcomingEvents.minByOrNull {
            java.time.Duration.between(now, it).abs()
        }

        if (nearestEvent != null) {
            val minutesToEvent = java.time.Duration.between(now, nearestEvent).toMinutes()

            // 根据最近事件的距离动态调整
            val interval = when {
                minutesToEvent < 10 -> MIN_CHECK_INTERVAL // 10分钟内，15秒检查
                minutesToEvent < 30 -> 60000L // 30分钟内，1分钟检查
                minutesToEvent < 120 -> 300000L // 2小时内，5分钟检查
                minutesToEvent < 360 -> 600000L // 6小时内，10分钟检查
                else -> 1800000L // 否则30分钟检查
            }

            return interval
        }

        return MAX_CHECK_INTERVAL
    }

    /**
     * 添加日程
     */
    fun addSchedule(schedule: ScheduleItem) {
        _schedules.add(schedule)
        saveSchedules()

        // 重新计算检查间隔并重启监控
        restartMonitorWithNewInterval()
    }

    /**
     * 更新日程
     */
    fun updateSchedule(scheduleId: String, newSchedule: ScheduleItem) {
        val index = _schedules.indexOfFirst { it.id == scheduleId }
        if (index != -1) {
            _schedules[index] = newSchedule
            saveSchedules()
            restartMonitorWithNewInterval()
        }
    }

    /**
     * 删除日程
     */
    fun deleteSchedule(scheduleId: String) {
        _schedules.removeAll { it.id == scheduleId }
        notifiedSchedules.remove(scheduleId)
        notifiedEndSchedules.remove(scheduleId)
        saveSchedules()
        restartMonitorWithNewInterval()
    }

    /**
     * 批量添加日程
     */
    fun addSchedules(schedules: List<ScheduleItem>) {
        _schedules.addAll(schedules)
        saveSchedules()
        restartMonitorWithNewInterval()
    }

    /**
     * 获取所有日程
     */
    fun getAllSchedules(): List<ScheduleItem> {
        return _schedules.toList()
    }

    /**
     * 重启监控并更新检查间隔
     */
    private fun restartMonitorWithNewInterval() {
        val newInterval = calculateOptimalCheckInterval()
        if (newInterval != currentCheckInterval) {
            currentCheckInterval = newInterval
            startBackgroundMonitor()
        }
    }

    /**
     * 保存日程到本地存储
     */
    private fun saveSchedules() {
        try {
            val persistentSchedules = _schedules.map { schedule ->
                PersistentScheduleItem(
                    id = schedule.id,
                    name = schedule.name,
                    note = schedule.note,
                    startTime = schedule.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    endTime = schedule.endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = schedule.type.name,
                    repeatMode = schedule.repeatMode.name,
                    weekDays = schedule.weekDays.map { it.name },
                    cyclicTasks = schedule.cyclicTasks.map { task ->
                        PersistentCyclicTask(
                            id = task.id, name = task.name, duration = task.duration
                        )
                    },
                )
            }

            val jsonString = json.encodeToString(persistentSchedules)
            settings.putString(KEY_SCHEDULES, jsonString)
        } catch (e: Exception) {
            KLogger.de { "保存日程失败: ${e.message}" }
            e.printStackTrace()
        }
    }

    /**
     * 从本地存储加载日程
     */
    private fun loadSchedules() {
        try {
            val jsonString = settings.getStringOrNull(KEY_SCHEDULES)
            if (jsonString != null) {
                val persistentSchedules = json.decodeFromString<List<PersistentScheduleItem>>(jsonString)

                _schedules.clear()
                persistentSchedules.forEach { persistent ->
                    try {
                        val schedule = ScheduleItem(id = persistent.id,
                            name = persistent.name,
                            note = persistent.note,
                            startTime = LocalDateTime.parse(
                                persistent.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            ),
                            endTime = LocalDateTime.parse(persistent.endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            type = com.treevalue.beself.ui.ScheduleType.valueOf(persistent.type),
                            repeatMode = com.treevalue.beself.ui.RepeatMode.valueOf(persistent.repeatMode),
                            weekDays = persistent.weekDays.map { java.time.DayOfWeek.valueOf(it) }.toSet(),
                            cyclicTasks = persistent.cyclicTasks.map { task ->
                                com.treevalue.beself.ui.CyclicTask(
                                    id = task.id, name = task.name, duration = task.duration
                                )
                            })
                        _schedules.add(schedule)
                    } catch (e: Exception) {
                        KLogger.de { "解析日程失败: ${e.message}" }
                    }
                }


                // 加载后清理过期日程
                cleanExpiredSchedules()
            }
        } catch (e: Exception) {
            KLogger.de { "加载日程失败: ${e.message}" }
            e.printStackTrace()
        }
    }

    /**
     * 清理过期的日程（结束时间超过1天的一次性日程）
     */
    private fun cleanExpiredSchedules() {
        val now = LocalDateTime.now()
        val oneDayAgo = now.minusDays(1)

        val initialSize = _schedules.size

        // 只删除一次性日程中结束时间超过1天的
        _schedules.removeAll { schedule ->
            schedule.repeatMode == com.treevalue.beself.ui.RepeatMode.ONCE && schedule.endTime.isBefore(oneDayAgo)
        }

        val removedCount = initialSize - _schedules.size

        if (removedCount > 0) {
            saveSchedules()
        }
    }

    /**
     * 启动后台监控，检查日程开始和结束时间
     */
    private fun startBackgroundMonitor() {
        monitorJob?.cancel()

        // 计算初始检查间隔
        currentCheckInterval = calculateOptimalCheckInterval()

        monitorJob = scope.launch {
            try {
                while (true) {
                    checkScheduleNotifications()

                    // 每次检查后重新计算最优间隔
                    val newInterval = calculateOptimalCheckInterval()
                    if (newInterval != currentCheckInterval) {
                        currentCheckInterval = newInterval
                    }

                    delay(currentCheckInterval)
                }
            } catch (e: Exception) {
                KLogger.de { "协程停止: ${e.message}" }
            }
        }
    }

    /**
     * 检查是否需要播报日程
     */
    private fun checkScheduleNotifications() {
        val now = LocalDateTime.now()

        _schedules.forEach { schedule ->
            // 检查是否到达开始时间（前后5分钟内）
            val minutesToStart = java.time.Duration.between(now, schedule.startTime).toMinutes()
            if (minutesToStart in -5..5 && !notifiedSchedules.contains(schedule.id)) {
                notifyScheduleStart(schedule)
                notifiedSchedules.add(schedule.id)
            }

            // 检查是否到达结束时间（前后5分钟内）
            val minutesToEnd = java.time.Duration.between(now, schedule.endTime).toMinutes()
            if (minutesToEnd in -5..5 && !notifiedEndSchedules.contains(schedule.id)) {
                notifyScheduleEnd(schedule)
                notifiedEndSchedules.add(schedule.id)
            }

            // 如果日程已经完全过去超过1小时，从已播报列表中移除
            if (java.time.Duration.between(schedule.endTime, now).toHours() > 1) {
                notifiedSchedules.remove(schedule.id)
                notifiedEndSchedules.remove(schedule.id)
            }
        }
    }

    /**
     * 播报日程开始
     */
    private fun notifyScheduleStart(schedule: ScheduleItem) {
        if (isTtsReady && _isTtsEnabled) {
            val message = "${schedule.name}，开始"
            KLogger.de { "播报: $message" }
            ttsEngine.speak(message)
        }
    }

    /**
     * 播报日程结束
     */
    private fun notifyScheduleEnd(schedule: ScheduleItem) {
        if (isTtsReady && _isTtsEnabled) {
            val message = "${schedule.name}，结束"
            KLogger.dd { "播报: $message" }
            ttsEngine.speak(message)
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        monitorJob?.cancel()
        ttsEngine.shutdown()
        saveSchedules()
    }
}
