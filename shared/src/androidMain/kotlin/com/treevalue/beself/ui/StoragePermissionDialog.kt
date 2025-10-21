package com.treevalue.beself.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun StoragePermissionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestManageStorage: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "存储权限请求",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "应用需要存储权限来保存和读取浏览器数据。请选择权限级别：",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // 基础权限按钮
                    Button(
                        onClick = {
                            onRequestPermission()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("请求基础存储权限")
                    }

                    // 完全访问权限按钮（Android 11+）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        OutlinedButton(
                            onClick = {
                                onRequestManageStorage()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("请求完全文件访问权限")
                        }

                        Text(
                            text = "注意：完全文件访问权限需要在系统设置中手动开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(Pages.AddSitePage.Cancel.getLang())
                    }
                }
            }
        }
    }
}
