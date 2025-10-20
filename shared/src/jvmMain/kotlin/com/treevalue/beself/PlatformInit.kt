package com.treevalue.beself

import com.treevalue.beself.persistence.PersistentDeleteStorage

object PlatformInit {
    /**
     * 初始化平台特定的组件
     * @param context 在 Android 中传入 Context，在桌面中传入 null
     */
    fun initialize(context: Any? = null) {
        if (context != null) {
            // Android 环境
            PersistentDeleteStorage.initializeAndroidContext(context)
        } else {
            // 桌面环境
        }
    }

    fun isInitialized(): Boolean {
        return try {
            PersistentDeleteStorage.isAndroidEnvironment()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEnvironmentInfo(): String {
        return buildString {
            appendLine("平台信息:")
            appendLine("操作系统: ${System.getProperty("os.name")}")
            appendLine("Java 版本: ${System.getProperty("java.version")}")
            appendLine("Java 供应商: ${System.getProperty("java.vendor")}")

            val isAndroid = try {
                PersistentDeleteStorage.isAndroidEnvironment()
            } catch (e: Exception) {
                false
            }

            appendLine("运行环境: ${if (isAndroid) "Android" else "桌面"}")
            appendLine("初始化状态: ${if (isInitialized()) "已初始化" else "未初始化"}")
        }
    }
}
