/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.treevalue.beself

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.treevalue.beself.persistence.PersistenceManager
import com.treevalue.beself.persistence.StoragePermissionManager
import com.treevalue.beself.platform.AndroidContextProvider
import com.treevalue.beself.ui.StoragePermissionDialog

class MainActivity : AppCompatActivity() {
    private lateinit var permissionManager: StoragePermissionManager
    private lateinit var persistenceManager: PersistenceManager

    private var permissionGranted = mutableStateOf<Boolean?>(null)
    private var showExitDialog = mutableStateOf(false)
    private var userDeniedPermission = mutableStateOf(false)
    private var isNavigatingToSettings = mutableStateOf(false) // 跟踪是否正在跳转到设置

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformInit.initialize(applicationContext)
        permissionManager = StoragePermissionManager(this)
        persistenceManager = PersistenceManager()
        AndroidContextProvider.init(application)

        handleIntent(intent)

        setContent {
            LaunchedEffect(Unit) {
                checkStoragePermission()
            }

            when {
                showExitDialog.value -> {
                    ExitConfirmDialog()
                }

                permissionGranted.value == false -> {
                    StoragePermissionDialog(
                        isVisible = true,
                        onDismiss = {
                            if (isNavigatingToSettings.value) {
                            } else {
                                userDeniedPermission.value = true
                                showExitDialog.value = true
                            }
                        },
                        onRequestPermission = {
                            userDeniedPermission.value = false
                            isNavigatingToSettings.value = true // 标记即将跳转
                            requestBasicPermission()
                        },
                        onRequestManageStorage = {
                            userDeniedPermission.value = false
                            isNavigatingToSettings.value = true // 标记即将跳转
                            requestManageStoragePermission()
                        }
                    )
                }

                permissionGranted.value == true -> {
                    webViewApp()
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("检查权限中...")
                    }
                }
            }
        }
    }

    @Composable
    private fun ExitConfirmDialog() {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("权限必需") },
            text = {
                Text("此应用需要存储权限才能正常运行。没有存储权限将无法保存数据和文件。\n\n是否退出应用？")
            },
            confirmButton = {
                TextButton(onClick = { exitApplication() }) {
                    Text("退出应用")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog.value = false
                        userDeniedPermission.value = false
                        isNavigatingToSettings.value = false // 重置跳转状态
                        checkStoragePermission()
                    }
                ) {
                    Text("重新申请")
                }
            }
        )
    }

    private fun checkStoragePermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            permissionManager.hasStoragePermission()
        }

        permissionGranted.value = hasPermission

        if (hasPermission) {
            userDeniedPermission.value = false
            persistenceManager.resetStorageStrategy()
        } else {

        }
    }

    private fun requestBasicPermission() {

        permissionManager.requestStoragePermission(requestManageStorage = false) { granted ->
            runOnUiThread {

                if (!granted) {
                    userDeniedPermission.value = true
                }
            }
        }
    }

    private fun requestManageStoragePermission() {

        permissionManager.requestStoragePermission(requestManageStorage = true) { granted ->
            runOnUiThread {

                if (!granted) {
                    userDeniedPermission.value = true
                }
                // 不在这里直接更新permissionGranted，让onResume处理
            }
        }
    }

    private fun exitApplication() {
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {

        }
        finish()
        finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onResume() {
        super.onResume()
        if (isNavigatingToSettings.value) {
            isNavigatingToSettings.value = false
        }
        checkStoragePermission()
    }

    override fun onBackPressed() {
        if (permissionGranted.value != true) {
            userDeniedPermission.value = true
            showExitDialog.value = true
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            uri?.let {
                val url = it.toString()
                UrlReceiver.receiveUrl(url)
            }
        }
    }
}
