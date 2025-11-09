package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 平台无关接口（新增 onProgress 回调 & 语义更清晰的质量百分比）
 */
interface ImageVideoOps {
    /** 选择多个图片（路径或 content:// Uri 字符串） */
    suspend fun pickImages(): List<String>
    /** 选择多个视频（路径或 content:// Uri 字符串） */
    suspend fun pickVideos(): List<String>
    /** 选择输出目录；Android 返回 Tree Uri 字符串，桌面返回绝对路径 */
    suspend fun pickDirectory(): String

    /** 压缩图片，qualityPercent: 1..100（就是“压缩到：xx%”） */
    suspend fun compressImages(
        inputs: List<String>,
        outDir: String,
        qualityPercent: Int,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit
    ): Pair<Int, Int>

    /** 去除视频声音，仅保留视频轨 */
    suspend fun stripAudio(
        inputs: List<String>,
        outDir: String,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit
    ): Pair<Int, Int>
}

@Composable
expect fun rememberPlatformImageVideoOps(): ImageVideoOps

@Composable
fun CompressionPage(
    onBackClicked: () -> Unit,
) {
    val ops = rememberPlatformImageVideoOps()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var running by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    fun append(msg: String) { log += if (log.isEmpty()) msg else "\n$msg" }

    // 图片
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageQuality by remember { mutableStateOf(80f) } // “压缩到：xx%”
    var imageOutDir by remember { mutableStateOf("") }
    var imgDone by remember { mutableStateOf(0) }
    var imgTotal by remember { mutableStateOf(0) }

    // 视频
    var videoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var videoOutDir by remember { mutableStateOf("") }
    var vidDone by remember { mutableStateOf(0) }
    var vidTotal by remember { mutableStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClicked, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("压缩", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
                }

                // 图片压缩卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("图片压缩", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("已选 ${imagePaths.size} 张", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { imagePaths = ops.pickImages() }
                            }, enabled = !running) { Text("选择图片") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("压缩到：${imageQuality.toInt()}%")
                        Slider(
                            value = imageQuality,
                            onValueChange = { imageQuality = it.coerceIn(1f, 100f) },
                            valueRange = 1f..100f,
                            steps = 98,
                            enabled = !running
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (imageOutDir.isBlank()) "未选择输出目录" else "输出目录：$imageOutDir", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { imageOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择输出目录") }
                        }

                        if (running && imgTotal > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = imgDone / imgTotal.toFloat())
                            Spacer(Modifier.height(4.dp))
                            Text("进度：$imgDone / $imgTotal")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    running = true
                                    imgDone = 0; imgTotal = imagePaths.size
                                    log = "开始压缩图片..."
                                    try {
                                        val (ok, fail) = ops.compressImages(
                                            inputs = imagePaths,
                                            outDir = imageOutDir,
                                            qualityPercent = imageQuality.toInt(),
                                            logger = ::append
                                        ) { d, t ->
                                            imgDone = d; imgTotal = t
                                        }
                                        append("图片压缩完成：成功 $ok，失败 $fail")
                                        snackbar.showSnackbar("图片压缩完成：成功 $ok，失败 $fail")
                                    } finally { running = false }
                                }
                            },
                            enabled = !running && imagePaths.isNotEmpty() && imageOutDir.isNotBlank()
                        ) { Text("开始压缩") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 视频去声卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoFile, contentDescription = null, tint = MaterialTheme.colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("视频去除声音", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("已选 ${videoPaths.size} 个视频", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { videoPaths = ops.pickVideos() }
                            }, enabled = !running) { Text("选择视频") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (videoOutDir.isBlank()) "未选择输出目录" else "输出目录：$videoOutDir", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { videoOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择输出目录") }
                        }

                        if (running && vidTotal > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = vidDone / vidTotal.toFloat())
                            Spacer(Modifier.height(4.dp))
                            Text("进度：$vidDone / $vidTotal")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    running = true
                                    vidDone = 0; vidTotal = videoPaths.size
                                    log = "开始去除视频声音..."
                                    try {
                                        val (ok, fail) = ops.stripAudio(
                                            inputs = videoPaths,
                                            outDir = videoOutDir,
                                            logger = ::append
                                        ) { d, t ->
                                            vidDone = d; vidTotal = t
                                        }
                                        append("视频处理完成：成功 $ok，失败 $fail")
                                        snackbar.showSnackbar("视频处理完成：成功 $ok，失败 $fail")
                                    } finally { running = false }
                                }
                            },
                            enabled = !running && videoPaths.isNotEmpty() && videoOutDir.isNotBlank()
                        ) { Text("开始处理") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (log.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        backgroundColor = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("日志", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(log, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
