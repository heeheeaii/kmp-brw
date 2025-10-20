package com.treevalue.beself.persistence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class StoragePermissionManager(private val activity: ComponentActivity) {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // 权限请求启动器
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            handlePermissionResult(allGranted)
        }

    // Android 11+ 管理外部存储权限启动器
    private val manageStorageLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
            handlePermissionResult(hasPermission)
        }

    fun hasStoragePermission(includeManageStorage: Boolean = true): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 访问公共目录必须有 MANAGE_EXTERNAL_STORAGE 权限
                Environment.isExternalStorageManager()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    // 修改请求权限方法，Android 11+ 强制要求管理权限
    fun requestStoragePermission(
        requestManageStorage: Boolean = true, // 默认请求完整权限
        onResult: (Boolean) -> Unit
    ) {
        this.onPermissionResult = onResult

        when {
            hasStoragePermission(requestManageStorage) -> {
                onResult(true)
            }

            // Android 11+ 直接请求管理权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                requestManageExternalStoragePermission()
            }

            else -> {
                requestTraditionalPermissions()
            }
        }
    }

    /**
     * 请求传统存储权限
     */
    private fun requestTraditionalPermissions() {
        val permissions = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ 使用细粒度媒体权限
                permissions.addAll(
                    listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6.0-12 使用传统权限
                permissions.addAll(
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            handlePermissionResult(true)
        }
    }

    /**
     * 请求管理外部存储权限 (Android 11+)
     */
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                // 如果无法打开应用特定设置，打开通用设置
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            handlePermissionResult(true)
        }
    }

    /**
     * 处理权限请求结果
     */
    private fun handlePermissionResult(granted: Boolean) {
        onPermissionResult?.invoke(granted)
        onPermissionResult = null
    }

    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            else -> false
        }
    }

    /**
     * 获取权限状态描述
     */
    fun getPermissionStatusDescription(): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    "✅ 已获得完全文件访问权限"
                } else {
                    "⚠️ 仅有限文件访问权限"
                }
            }

            hasStoragePermission() -> "✅ 已获得存储权限"

            else -> "❌ 未获得存储权限"
        }
    }
}
