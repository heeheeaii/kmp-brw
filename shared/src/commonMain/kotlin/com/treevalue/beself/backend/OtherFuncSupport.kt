package com.treevalue.beself.backend

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class FeatureSettings(
    val videoEnabled: Boolean = false,
    val videoUsageToday: Long = 0L, // 今天已使用的毫秒数
    val lastUsageDate: String = "", // 最后使用日期，用于重置每日计时
    val videoSessionStartTime: Long? = null, // 当前会话开始时间，null表示未开启
    val videoUsageResetTime: Long = 0L, // 上次时间重置时间
)

@Serializable
data class FeatureLimits(
    val videoLimit: Long = 20 * 60 * 1000L, // 20分钟
    val longVideoTimeSites: List<String> = listOf(
        // 可以添加允许20分钟的网站
        "youtube",
        "bilibili",
    ),
)

// 工具函数
fun getCurrentDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date())
}

fun isNewDay(lastDate: String): Boolean {
    return lastDate != getCurrentDateString()
}
