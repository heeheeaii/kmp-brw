package com.treevalue.beself.persistence

import com.treevalue.beself.values.appFolder
import com.treevalue.beself.values.deleteRecordFileName
import com.treevalue.beself.encrypt.simpleDecrypt
import com.treevalue.beself.encrypt.simpleEncrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object PersistentDeleteStorage {
    private const val RECORD_FILE_NAME = deleteRecordFileName
    private const val APP_FOLDER_NAME = appFolder

    // 用于存储 Android Context（如果在 Android 环境中）
    private var androidContext: Any? = null

    /**
     * 在 Android 环境中初始化 Context
     * 在 Application.onCreate() 中调用
     */
    actual fun initializeAndroidContext(context: Any?) {
        androidContext = context

    }

    /**
     * 检测是否在 Android 环境中运行
     */
    actual fun isAndroidEnvironment(): Boolean {
        return try {
            // 方法1: 检查系统属性
            val javaVendor = System.getProperty("java.vendor", "").lowercase()
            val javaVmName = System.getProperty("java.vm.name", "").lowercase()

            if (javaVendor.contains("android") || javaVmName.contains("android")) {
                return true
            }

            // 方法2: 尝试加载 Android 特定的类
            Class.forName("android.content.Context")
            true
        } catch (e: Exception) {
            false
        }
    }

    // 获取持久化存储文件路径
    private suspend fun getRecordFile(): File? = withContext(Dispatchers.IO) {
        try {
            if (isAndroidEnvironment()) {
                return@withContext getAndroidRecordFile()
            } else {
                return@withContext getDesktopRecordFile()
            }
        } catch (e: Exception) {

            return@withContext null
        }
    }

    // Android 环境的文件路径获取
    private fun getAndroidRecordFile(): File? {
        return try {
            val context = androidContext
            if (context == null) {

                return null
            }

            // 使用反射调用 Android API，避免直接依赖 Android SDK
            val contextClass = context.javaClass

            // 策略1: 尝试使用外部存储
            try {
                val environmentClass = Class.forName("android.os.Environment")
                val getExternalStorageStateMethod = environmentClass.getMethod("getExternalStorageState")
                val getExternalStoragePublicDirectoryMethod = environmentClass.getMethod(
                    "getExternalStoragePublicDirectory", String::class.java
                )

                val mediaMount = environmentClass.getField("MEDIA_MOUNTED").get(null) as String
                val directoryDocuments = environmentClass.getField("DIRECTORY_DOCUMENTS").get(null) as String

                val externalState = getExternalStorageStateMethod.invoke(null) as String

                if (externalState == mediaMount) {
                    val documentsDir = getExternalStoragePublicDirectoryMethod.invoke(null, directoryDocuments) as File
                    if (documentsDir.canWrite()) {
                        val appDir = File(documentsDir, APP_FOLDER_NAME)
                        if (!appDir.exists()) {
                            appDir.mkdirs()
                        }
                        val recordFile = File(appDir, RECORD_FILE_NAME)

                        return recordFile
                    }
                }
            } catch (e: Exception) {

            }

            // 策略2: 使用应用外部文件目录
            try {
                val getExternalFilesDirMethod = contextClass.getMethod("getExternalFilesDir", String::class.java)
                val externalFilesDir = getExternalFilesDirMethod.invoke(context, null) as? File

                if (externalFilesDir != null && externalFilesDir.canWrite()) {
                    val appDir = File(externalFilesDir.parentFile, APP_FOLDER_NAME)
                    if (!appDir.exists()) {
                        appDir.mkdirs()
                    }
                    val recordFile = File(appDir, RECORD_FILE_NAME)

                    return recordFile
                }
            } catch (e: Exception) {

            }

            // 策略3: 使用内部存储（最后的备选方案）
            try {
                val getFilesDirMethod = contextClass.getMethod("getFilesDir")
                val filesDir = getFilesDirMethod.invoke(context) as File
                val appDir = File(filesDir.parentFile, APP_FOLDER_NAME)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val recordFile = File(appDir, RECORD_FILE_NAME)

                return recordFile
            } catch (e: Exception) {

            }

            null
        } catch (e: Exception) {

            null
        }
    }

    // 桌面环境的文件路径获取
    private fun getDesktopRecordFile(): File {
        val systemDir = when {
            // Windows
            System.getProperty("os.name").lowercase().contains("windows") -> {
                File(System.getenv("APPDATA") ?: System.getProperty("user.home"), APP_FOLDER_NAME)
            }

            // macOS
            System.getProperty("os.name").lowercase().contains("mac") -> {
                File(System.getProperty("user.home"), "Library/Application Support/$APP_FOLDER_NAME")
            }

            // Linux 和其他 Unix 系统
            else -> {
                File(System.getProperty("user.home", "/tmp"), ".$APP_FOLDER_NAME")
            }
        }

        if (!systemDir.exists()) {
            systemDir.mkdirs()
        }

        val recordFile = File(systemDir, RECORD_FILE_NAME)

        return recordFile
    }

    actual suspend fun saveDeleteRecords(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val recordFile = getRecordFile() ?: return@withContext false

            // 确保父目录存在
            recordFile.parentFile?.mkdirs()
            val encryptedData = simpleEncrypt(data)
            recordFile.writeBytes(encryptedData)

            return@withContext true
        } catch (e: Exception) {

            return@withContext false
        }
    }

    actual suspend fun loadDeleteRecords(): String? = withContext(Dispatchers.IO) {
        try {
            val recordFile = getRecordFile() ?: return@withContext null

            if (recordFile.exists() && recordFile.canRead()) {
                val encryptedData = recordFile.readBytes()
                val decryptedContent = simpleDecrypt(encryptedData)

                return@withContext decryptedContent
            } else {

                return@withContext null
            }
        } catch (e: Exception) {

            return@withContext null
        }
    }

    actual suspend fun clearDeleteRecords(): Boolean = withContext(Dispatchers.IO) {
        try {
            val recordFile = getRecordFile() ?: return@withContext false

            if (recordFile.exists()) {
                val deleted = recordFile.delete()
                if (deleted) {

                }
                return@withContext deleted
            } else {

                return@withContext true
            }
        } catch (e: Exception) {

            return@withContext false
        }
    }

    actual suspend fun hasDeleteRecords(): Boolean = withContext(Dispatchers.IO) {
        try {
            val recordFile = getRecordFile() ?: return@withContext false
            return@withContext recordFile.exists() && recordFile.length() > 0
        } catch (e: Exception) {

            return@withContext false
        }
    }

    actual suspend fun getStorageInfo(): String = withContext(Dispatchers.IO) {
        val isAndroid = isAndroidEnvironment()
        val recordFile = getRecordFile()

        return@withContext buildString {
            appendLine("=== 存储信息 ===")
            appendLine("运行环境: ${if (isAndroid) "Android" else "桌面"}")
            if (isAndroid) {
                appendLine("Android Context 已初始化: ${androidContext != null}")
            }
            appendLine("操作系统: ${System.getProperty("os.name")}")
            appendLine("Java 供应商: ${System.getProperty("java.vendor")}")
            appendLine("Java VM: ${System.getProperty("java.vm.name")}")
            appendLine("记录文件路径: ${recordFile?.absolutePath ?: "无法获取"}")

            if (recordFile != null) {
                appendLine("文件存在: ${recordFile.exists()}")
                appendLine("文件可读: ${recordFile.canRead()}")
                appendLine("文件可写: ${recordFile.canWrite()}")
                appendLine("文件大小: ${if (recordFile.exists()) recordFile.length() else 0} 字节")
                appendLine("父目录存在: ${recordFile.parentFile?.exists() ?: false}")
                appendLine("父目录可写: ${recordFile.parentFile?.canWrite() ?: false}")
            }
            appendLine("===============")
        }
    }
}
