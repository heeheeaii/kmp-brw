package com.treevalue.beself.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treevalue.beself.backend.Pages
import com.treevalue.beself.backend.getLang
import compose_webview_multiplatform.shared.generated.resources.Res
import compose_webview_multiplatform.shared.generated.resources.help_img
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun HelpPage() {
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showHelpText = remember { mutableStateOf(false) }


    val emailText = "heeheeaii@163.com"

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎉",
                    fontSize = 32.sp
                )
                Text(
                    text = Pages.HelpPage.HelpCenter.getLang(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.h4
                )
                Text(
                    text = Pages.HelpPage.HelpLang.getLang(),
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Card(
                modifier = Modifier.size(200.dp, 150.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.help_img),
                            contentDescription = Pages.HelpPage.HelpImg.getLang(),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showHelpText.value) {
                    Text(
                        text = Pages.HelpPage.HelpDescription.getLang(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { showHelpText.value = !showHelpText.value }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Help,
                            contentDescription = Pages.HelpPage.HelpInfo.getLang(),
                            tint = MaterialTheme.colors.primary
                        )
                    }
//                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📧 ${Pages.HelpPage.FeedbackEmail.getLang()}:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
                Text(
                    text = emailText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.fillMaxWidth().clickable {
                        clipboardManager.setText(AnnotatedString(emailText))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(Pages.HelpPage.CopyTip.getLang())
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
