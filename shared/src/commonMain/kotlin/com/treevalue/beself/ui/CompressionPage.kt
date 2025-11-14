package com.treevalue.beself.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

interface ImageVideoOps {
    /** 选择多个图片（路径或 content:// Uri 字符串） */
    suspend fun pickImages(): List<String>

    /** 选择多个视频（路径或 content:// Uri 字符串） */
    suspend fun pickVideos(): List<String>

    /** 选择输出目录；Android 返回 Tree Uri 字符串，桌面返回绝对路径 */
    suspend fun pickDirectory(): String

    /** 选择单个文件用于分割 */
    suspend fun pickFileForSplit(): String

    /** 选择目录用于合并（包含分割块的目录） */
    suspend fun pickDirectoryForMerge(): String

    /** 分割文件 */
    suspend fun splitFile(
        inputFile: String,
        outDir: String,
        splitSizeMB: Int,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Boolean

    /** 合并文件 */
    suspend fun mergeFile(
        inputDir: String,
        outDir: String,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Boolean

    /** 压缩图片，qualityPercent: 1..100（就是“压缩到：xx%”） */
    suspend fun compressImages(
        inputs: List<String>,
        outDir: String,
        qualityPercent: Int,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Pair<Int, Int>

    /** 去除视频声音，仅保留视频轨 */
    suspend fun stripAudio(
        inputs: List<String>,
        outDir: String,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
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
    fun append(msg: String) {
        log += if (log.isEmpty()) msg else "\n$msg"
    }

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
                    Text(
                        "压缩",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    )
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
                            }, enabled = !running) { Text("选择") }
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
                            Text(
                                if (imageOutDir.isBlank()) "输出目录" else "输出目录：$imageOutDir",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { imageOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择") }
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
                                    } finally {
                                        running = false
                                    }
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
                            Icon(
                                Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("视频去除声音", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("已选 ${videoPaths.size} 个视频", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch { videoPaths = ops.pickVideos() }
                            }, enabled = !running) { Text("选择") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (videoOutDir.isBlank()) "输出目录" else "输出目录：$videoOutDir",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { videoOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择") }
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
                                    } finally {
                                        running = false
                                    }
                                }
                            },
                            enabled = !running && videoPaths.isNotEmpty() && videoOutDir.isNotBlank()
                        ) { Text("开始处理") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 文件分割卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("文件分割", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        var splitFilePath by remember { mutableStateOf("") }
                        var splitOutDir by remember { mutableStateOf("") }
                        var splitSizeMB by remember { mutableStateOf(100f) }
                        var splitDone by remember { mutableStateOf(0L) }
                        var splitTotal by remember { mutableStateOf(0L) }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (splitFilePath.isBlank()) "文件" else "文件：${
                                    splitFilePath.substringAfterLast(
                                        '/'
                                    )
                                }",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { splitFilePath = ops.pickFileForSplit() }
                            }, enabled = !running) { Text("选择") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("分割大小：${splitSizeMB.toInt()} MB")
                        Slider(
                            value = splitSizeMB,
                            onValueChange = { splitSizeMB = it.coerceIn(1f, 1000f) },
                            valueRange = 1f..1000f,
                            steps = 998,
                            enabled = !running
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (splitOutDir.isBlank()) "输出目录" else "输出：$splitOutDir",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { splitOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择") }
                        }

                        if (running && splitTotal > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = (splitDone / splitTotal.toFloat()).coerceIn(0f, 1f))
                            Spacer(Modifier.height(4.dp))
                            Text("进度：${splitDone / 1024 / 1024} / ${splitTotal / 1024 / 1024} MB")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    running = true
                                    splitDone = 0L; splitTotal = 0L
                                    log = "开始分割文件..."
                                    try {
                                        val success = ops.splitFile(
                                            inputFile = splitFilePath,
                                            outDir = splitOutDir,
                                            splitSizeMB = splitSizeMB.toInt(),
                                            logger = ::append
                                        ) { d, t ->
                                            splitDone = d.toLong()
                                            splitTotal = t.toLong()
                                        }
                                        if (success) {
                                            append("✅ 文件分割成功！")
                                            snackbar.showSnackbar("文件分割成功！")
                                        }
                                    } finally {
                                        running = false
                                    }
                                }
                            },
                            enabled = !running && splitFilePath.isNotBlank() && splitOutDir.isNotBlank()
                        ) { Text("开始分割") }
                    }
                }

                Spacer(Modifier.height(16.dp))

// 文件合并卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.MergeType,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("文件合并", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        var mergeInputDir by remember { mutableStateOf("") }
                        var mergeOutDir by remember { mutableStateOf("") }
                        var mergeDone by remember { mutableStateOf(0L) }
                        var mergeTotal by remember { mutableStateOf(0L) }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (mergeInputDir.isBlank()) "输入目录" else "输入：$mergeInputDir",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { mergeInputDir = ops.pickDirectoryForMerge() }
                            }, enabled = !running) { Text("选择") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (mergeOutDir.isBlank()) "输出目录" else "输出：$mergeOutDir",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                scope.launch { mergeOutDir = ops.pickDirectory() }
                            }, enabled = !running) { Text("选择") }
                        }

                        if (running && mergeTotal > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = (mergeDone / mergeTotal.toFloat()).coerceIn(0f, 1f))
                            Spacer(Modifier.height(4.dp))
                            Text("进度：${mergeDone / 1024 / 1024} / ${mergeTotal / 1024 / 1024} MB")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    running = true
                                    mergeDone = 0L; mergeTotal = 0L
                                    log = "开始合并文件..."
                                    try {
                                        val success = ops.mergeFile(
                                            inputDir = mergeInputDir,
                                            outDir = mergeOutDir,
                                            logger = ::append
                                        ) { d, t ->
                                            mergeDone = d.toLong(); mergeTotal = t.toLong()
                                        }
                                        if (success) {
                                            append("✅ 文件合并成功！")
                                            snackbar.showSnackbar("文件合并成功！")
                                        }
                                    } finally {
                                        running = false
                                    }
                                }
                            },
                            enabled = !running && mergeInputDir.isNotBlank() && mergeOutDir.isNotBlank()
                        ) { Text("开始合并") }
                    }
                }

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
