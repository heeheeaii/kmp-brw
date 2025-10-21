package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.FunctionBackend
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import com.treevalue.beself.config.LangType

@Composable
fun SystemSettingsPage(
    onBackClicked: () -> Unit,
    backend: FunctionBackend = FunctionBackend
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    // 使用 backend 的状态，当语言改变时 UI 会自动更新
    val currentLanguage by backend.language

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp)
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Pages.FunctionPage.Back.getLang(),
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Pages.SystemSettingsPage.SystemSettings.getLang(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground
                )
            }

            // 语言设置项
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true },
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = Pages.SystemSettingsPage.LanguageSettings.getLang(),
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = Pages.SystemSettingsPage.LanguageSettings.getLang(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Pages.SystemSettingsPage.CurrentLanguage.getLang()}: ${
                                    when (currentLanguage) {
                                        LangType.CN -> Pages.SystemSettingsPage.Chinese.getLang()
                                        LangType.EN -> Pages.SystemSettingsPage.English.getLang()
                                        else -> Pages.SystemSettingsPage.Chinese.getLang()
                                    }
                                }",
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { newLanguage ->
                backend.setLanguage(newLanguage)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: LangType,
    onLanguageSelected: (LangType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = Pages.SystemSettingsPage.LanguageSettings.getLang(),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Pages.SystemSettingsPage.SelectLanguage.getLang())
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 中文选项
                LanguageOptionCard(
                    languageName = "中文",
                    isSelected = currentLanguage == LangType.CN,
                    onClick = { onLanguageSelected(LangType.CN) }
                )

                // 英文选项
                LanguageOptionCard(
                    languageName = "English",
                    isSelected = currentLanguage == LangType.EN,
                    onClick = { onLanguageSelected(LangType.EN) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Pages.AddSitePage.Cancel.getLang())
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LanguageOptionCard(
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected)
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        else
            MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = languageName,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colors.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
