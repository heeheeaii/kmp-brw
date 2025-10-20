package com.treevalue.beself.persistence

/**
 * 跨平台持久化删除记录存储接口
 * 用于在应用卸载后仍能保留删除限制记录
 */
expect object PersistentDeleteStorage {
    /**
     * 保存删除记录数据
     * @param data JSON 字符串格式的删除记录
     * @return 是否保存成功
     */
    suspend fun saveDeleteRecords(data: String): Boolean

    /**
     * 加载删除记录数据
     * @return JSON 字符串格式的删除记录，如果没有数据则返回 null
     */
    suspend fun loadDeleteRecords(): String?

    /**
     * 清除所有删除记录
     * @return 是否清除成功
     */
    suspend fun clearDeleteRecords(): Boolean

    /**
     * 检查是否有持久化的删除记录
     * @return 是否存在删除记录
     */
    suspend fun hasDeleteRecords(): Boolean

    /**
     * 获取存储状态信息（用于调试）
     * @return 存储状态描述
     */
    suspend fun getStorageInfo(): String

    /**
     * 检测是否在 Android 环境中运行
     * @return 是否为 Android 环境
     */
    fun isAndroidEnvironment(): Boolean

    /**
     * 在 Android 环境中初始化 Context
     * @param context Android Context 对象
     */
    fun initializeAndroidContext(context: Any?)
}
