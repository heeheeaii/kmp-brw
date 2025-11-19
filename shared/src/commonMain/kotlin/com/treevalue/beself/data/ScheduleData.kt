package com.treevalue.beself.data

import kotlinx.serialization.Serializable

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

@Serializable
data class PersistentProgressItem(
    val id: String,
    val content: String,
    val createdAt: Long,
    val pinned: Boolean,
)

