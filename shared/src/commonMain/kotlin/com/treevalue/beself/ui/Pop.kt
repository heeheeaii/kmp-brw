import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.treevalue.beself.backend.InterceptRequestBackend
import com.treevalue.beself.bus.Event
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.EventId
import com.treevalue.beself.bus.PopEvent
import com.treevalue.beself.ui.FunctionPage
import com.treevalue.beself.ui.FunctionPageType
import com.treevalue.beself.ui.HelpPage
import com.treevalue.beself.ui.SearchPage

/**
 * 通用的内容弹窗 Composable。
 *
 * @param onDismissRequest 当用户请求关闭弹窗时调用（例如点击外部区域或关闭按钮）。
 * @param modifier 应用于弹窗根部 Surface 的 Modifier。
 * @param width 弹窗的宽度。如果为 null，则根据内容自适应。
 * @param height 弹窗的高度。如果为 null，则根据内容自适应。
 * @param title 弹窗的标题。如果为 null，则不显示标题区域。
 * @param text 如果只需要显示一段简单的居中文本，请使用此参数。它将被包裹在一个带居中对齐的 Box 中。
 * @param content 如果需要更复杂的自定义布局，请使用此 lambda。
 */
@Composable
fun ContentPop(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 320.dp,
    height: Dp? = null,
    title: String? = null,
    text: String? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
    dismissOnClickOutside: Boolean = true,
) {
    val thenModifier = modifier.then(
        if (width != null && height != null) Modifier.size(width, height)
        else if (width != null) Modifier.width(width)
        else if (height != null) Modifier.height(height)
        else Modifier
    ).border(1.dp, Color.White, RoundedCornerShape(10.dp))

    Dialog(
        onDismissRequest = if (dismissOnClickOutside) onDismissRequest else {
            {}
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnClickOutside,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        val popColors = if (MaterialTheme.colors.isLight) {
            lightColors(
                primary = Color(0xFF6200EE),
                primaryVariant = Color(0xFF3700B3),
                secondary = Color(0xFF03DAC6),
                surface = Color.White,
                background = Color.White,
                onSurface = Color.Black,
                onBackground = Color.Black
            )
        } else {
            MaterialTheme.colors
        }

        MaterialTheme(colors = popColors) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colors.surface,
                modifier = thenModifier
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 根据内容类型调整布局策略
                    when {
                        content != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = 4.dp,
                                        start = 4.dp,
                                        end = 4.dp,
                                        bottom = 4.dp
                                    )
                            ) {
                                if (title != null) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colors.onSurface,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 12.dp
                                        )
                                    )
                                }

                                // 内容区域占用剩余所有空间
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        content()
                                    }
                                }
                            }
                        }

                        // 对于简单文本内容，保持原有的居中布局
                        text != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = if (title != null) 24.dp else 48.dp,
                                        start = 24.dp,
                                        end = 24.dp,
                                        bottom = 24.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (title != null) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }

                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // 默认情况（只有标题）
                        else -> {
                            if (title != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 24.dp,
                                            start = 24.dp,
                                            end = 24.dp,
                                            bottom = 24.dp
                                        )
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 关闭按钮始终在右上角
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(48.dp)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭弹窗",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun popControl(
    backend: InterceptRequestBackend? = null,
    onShowPopChange: (Boolean) -> Unit = {},
) {
    var showPop = remember { mutableStateOf(false) } // control self display
    var currentPage = remember { mutableStateOf<FunctionPageType?>(null) }

    val showEvent = { pageType: FunctionPageType ->
        currentPage.value = pageType
        showPop.value = true
        onShowPopChange(true)
    }

    val closeEvent: () -> Unit = {
        showPop.value = false
        currentPage.value = null
        onShowPopChange(false)
    }

    LaunchedEffect(Unit) {
        EventBus.registerHandler<Event>(EventId.Pop) { event ->
            when (event) {
                is PopEvent.AddSite -> showEvent(FunctionPageType.ADD_SITE)
                is PopEvent.SearchSite -> showEvent(FunctionPageType.SearchSite)
                is PopEvent.HelpPop -> showEvent(FunctionPageType.Help)
                is PopEvent.FunctionMenu -> showEvent(FunctionPageType.HOME)
                is PopEvent.SystemSettings -> showEvent(FunctionPageType.SYSTEM_SETTINGS)
                is PopEvent.Calculator -> showEvent(FunctionPageType.CALCULATOR)
                is PopEvent.Schedule -> showEvent(FunctionPageType.SCHEDULE)
                is PopEvent.Compression -> showEvent(FunctionPageType.COMPRESSION)
                is PopEvent.HideSite -> showEvent(FunctionPageType.HIDE_SITE)
                is PopEvent.BlockSite -> showEvent(FunctionPageType.BLOCK_SITE)
                is PopEvent.GrabSite -> showEvent(FunctionPageType.GRAB_SITE)
                is PopEvent.StartPageSetting -> showEvent(FunctionPageType.START_PAGE_SETTING)
                is PopEvent.OpenUrl -> showEvent(FunctionPageType.OPEN_URL)
                is PopEvent.OtherFunctions -> showEvent(FunctionPageType.OTHER_FUNCTIONS)
                is PopEvent.HidePop -> closeEvent()
            }
        }
    }

    if (showPop.value) {
        ContentPop(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            onDismissRequest = closeEvent,
            dismissOnClickOutside = false,
            content = {
                when (currentPage.value) {
                    FunctionPageType.Help -> {
                        HelpPage()
                    }

                    FunctionPageType.SearchSite -> {
                        backend?.let {
                            SearchPage(backend = it)
                        } ?: Text(
                            text = "错误：无法访问后端服务",
                            color = MaterialTheme.colors.error
                        )
                    }

                    FunctionPageType.HOME -> {
                        FunctionPage(
                            backend = backend,
                            onBackClicked = closeEvent
                        )
                    }

                    else -> {
                        if (currentPage.value != null) {
                            FunctionPage(
                                initialPage = currentPage.value!!,
                                backend = backend,
                                onBackClicked = closeEvent
                            )
                        } else {
                            closeEvent()
                        }
                    }
                }
            }
        )
    }
}
