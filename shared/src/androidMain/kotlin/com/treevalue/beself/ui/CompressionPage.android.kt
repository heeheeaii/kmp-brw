package com.treevalue.beself.ui

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

@Composable
fun rememberAwaitableLauncher(
    contract: ActivityResultContracts.OpenMultipleDocuments,
): Pair<(Array<String>) -> Unit, suspend () -> List<Uri>> {
    var waiter by remember { mutableStateOf<CompletableDeferred<List<Uri>>?>(null) }

    val launcher = rememberLauncherForActivityResult(contract) { uris ->
        waiter?.complete(uris ?: emptyList())
        waiter = null
    }

    val launch: (Array<String>) -> Unit = { mimes ->
        waiter = CompletableDeferred()
        launcher.launch(mimes)
    }
    val await: suspend () -> List<Uri> = {
        (waiter ?: CompletableDeferred<List<Uri>>().also { it.complete(emptyList()) }).await()
    }
    return launch to await
}

@Composable
fun rememberAwaitableTree(context: Context): Pair<(Uri?) -> Unit, suspend () -> Uri?> {
    var waiter by remember { mutableStateOf<CompletableDeferred<Uri?>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // 持久化权限
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        waiter?.complete(uri)
        waiter = null
    }
    val launch: (Uri?) -> Unit = { _ ->
        waiter = CompletableDeferred()
        launcher.launch(null)
    }
    val await: suspend () -> Uri? = {
        (waiter ?: CompletableDeferred<Uri?>().also { it.complete(null) }).await()
    }
    return launch to await
}

@Composable
actual fun rememberPlatformImageVideoOps(): ImageVideoOps {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // 图片/视频 chooser
    val (launchImgs, awaitImgs) = rememberAwaitableLauncher(ActivityResultContracts.OpenMultipleDocuments())
    val (launchVids, awaitVids) = rememberAwaitableLauncher(ActivityResultContracts.OpenMultipleDocuments())
    val (launchTree, awaitTree) = rememberAwaitableTree(context)

    return remember(activity) {
        object : ImageVideoOps {

            override suspend fun pickImages(): List<String> {
                launchImgs(arrayOf("image/*"))
                return awaitImgs().map { it.toString() }
            }

            override suspend fun pickVideos(): List<String> {
                launchVids(arrayOf("video/*"))
                return awaitVids().map { it.toString() }
            }

            override suspend fun pickDirectory(): String {
                launchTree(null)
                return awaitTree()?.toString() ?: ""
            }

            override suspend fun compressImages(
                inputs: List<String>,
                outDir: String,
                qualityPercent: Int,
                logger: (String) -> Unit,
                onProgress: (done: Int, total: Int) -> Unit,
            ): Pair<Int, Int> = withContext(Dispatchers.IO) {
                val cr = context.contentResolver
                val outTree = DocumentFile.fromTreeUri(context, Uri.parse(outDir))
                    ?: throw IllegalStateException("输出目录不可用")

                var ok = 0
                var fail = 0
                val total = inputs.size
                var done = 0
                for (p in inputs) {
                    try {
                        val uri = Uri.parse(p)
                        val name = guessDisplayName(cr, uri) ?: "image"
                        val outName = avoidOverwriteDoc(outTree, "$name.jpg")
                        val outDoc = outTree.createFile("image/jpeg", outName)
                            ?: throw IllegalStateException("无法创建文件：$outName")

                        cr.openInputStream(uri).use { input ->
                            if (input == null) throw IllegalStateException("无法读取：$p")
                            val bmp = BitmapFactory.decodeStream(input) ?: throw IllegalStateException("无法解码：$p")
                            cr.openOutputStream(outDoc.uri, "w").use { os ->
                                if (os == null) throw IllegalStateException("无法写入：$outName")
                                bmp.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(1, 100), os)
                            }
                        }
                        logger("✅ $name -> $outName")
                        ok++
                    } catch (e: Throwable) {
                        logger("❌ $p 失败：${e.message}")
                        fail++
                    } finally {
                        done++
                        onProgress(done, total)
                    }
                }
                ok to fail
            }

            override suspend fun stripAudio(
                inputs: List<String>,
                outDir: String,
                logger: (String) -> Unit,
                onProgress: (done: Int, total: Int) -> Unit,
            ): Pair<Int, Int> = withContext(Dispatchers.IO) {
                val cr = context.contentResolver
                val outTree = DocumentFile.fromTreeUri(context, Uri.parse(outDir))
                    ?: throw IllegalStateException("输出目录不可用")

                var ok = 0
                var fail = 0
                val total = inputs.size
                var done = 0

                for (p in inputs) {
                    try {
                        val inUri = Uri.parse(p)
                        val display = guessDisplayName(cr, inUri) ?: "video"

                        // —— 先用临时文件做中转 (minSdk 21 友好) —— //
                        val tempOut = File(context.cacheDir, "strip_${System.nanoTime()}.mp4")

                        val extractor = MediaExtractor().apply {
                            cr.openFileDescriptor(inUri, "r")?.use { pfd ->
                                setDataSource(pfd.fileDescriptor)
                            } ?: setDataSource(context, inUri, null)
                        }

// 找出视频轨
                        val videoTracks = (0 until extractor.trackCount).filter { i ->
                            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                        }
                        if (videoTracks.isEmpty()) throw IllegalStateException("无视频轨")

// 🔧 读取旋转角度（优先从 MediaFormat，兜底用 MediaMetadataRetriever）
                        var rotation = 0
                        val vf0 = extractor.getTrackFormat(videoTracks.first())
                        if (vf0.containsKey(MediaFormat.KEY_ROTATION)) {
                            rotation = try {
                                vf0.getInteger(MediaFormat.KEY_ROTATION)
                            } catch (_: Throwable) {
                                0
                            }
                        }
                        if (rotation == 0) {
                            // 有些文件 MediaFormat 没写旋转，用 retriever 更稳
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                cr.openFileDescriptor(inUri, "r")?.use { pfd ->
                                    retriever.setDataSource(pfd.fileDescriptor)
                                } ?: retriever.setDataSource(context, inUri)
                                rotation = retriever.extractMetadata(
                                    android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                                )?.toIntOrNull() ?: 0
                            } finally {
                                retriever.release()
                            }
                        }

                        val muxer = MediaMuxer(
                            tempOut.absolutePath,
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                        ).apply {
                            // 只接受 0/90/180/270，其它值归一化一下
                            val r = ((rotation % 360) + 360) % 360
                            val norm = when (r) {
                                90, 180, 270 -> r; else -> 0
                            }
                            setOrientationHint(norm) // <-- 关键：写回旋转元数据
                        }
                        val trackMap = mutableMapOf<Int, Int>()
                        for (vt in videoTracks) {
                            val fmt = extractor.getTrackFormat(vt)
                            trackMap[vt] = muxer.addTrack(fmt)
                        }
                        muxer.start()

                        val buf = ByteBuffer.allocate(1 * 1024 * 1024)
                        val info = MediaCodec.BufferInfo()
                        for (vt in videoTracks) {
                            extractor.selectTrack(vt)
                            while (true) {
                                info.offset = 0
                                info.size = extractor.readSampleData(buf, 0)
                                if (info.size < 0) {
                                    extractor.unselectTrack(vt); break
                                }
                                info.presentationTimeUs = extractor.sampleTime
                                // ✅ 不要强行改成 SYNC，保留原始 flags 更安全
                                info.flags = mapExtractorFlagsToCodecFlags(extractor.sampleFlags)
                                muxer.writeSampleData(trackMap.getValue(vt), buf, info)
                                extractor.advance()
                            }
                        }
                        muxer.stop()
                        muxer.release()
                        extractor.release()

                        // —— 把临时文件拷贝进 SAF 目录 —— //
                        val outName = avoidOverwriteDoc(outTree, "$display.mp4")
                        val outDoc = outTree.createFile("video/mp4", outName)
                            ?: throw IllegalStateException("无法创建文件：$outName")
                        cr.openOutputStream(outDoc.uri, "w").use { os ->
                            if (os == null) throw IllegalStateException("无法写入：$outName")
                            FileInputStream(tempOut).use { fis ->
                                fis.copyTo(os)
                            }
                        }
                        tempOut.delete()

                        logger("✅ $display -> $outName")
                        ok++
                    } catch (e: Throwable) {
                        logger("❌ $p 失败：${e.message}")
                        fail++
                    } finally {
                        done++
                        onProgress(done, total)
                    }
                }
                ok to fail
            }
        }
    }
}

/** 推测显示名（最好看的默认名） */
private fun guessDisplayName(cr: ContentResolver, uri: Uri): String? {
    return try {
        cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val raw = if (idx >= 0) c.getString(idx) else null
                raw?.substringBeforeLast('.', raw)
            } else null
        }
    } catch (_: Throwable) {
        null
    }
}

/** 在 DocumentTree 中避免覆盖，返回最终文件名 */
private fun avoidOverwriteDoc(dir: DocumentFile, wanted: String): String {
    val dot = wanted.lastIndexOf('.')
    val stem = if (dot >= 0) wanted.substring(0, dot) else wanted
    val ext = if (dot >= 0) wanted.substring(dot) else ""
    fun exists(name: String) = dir.findFile(name) != null
    var idx = 0
    while (true) {
        val name = if (idx == 0) "$stem$ext" else "$stem($idx)$ext"
        if (!exists(name)) return name
        idx++
    }
}

private fun mapExtractorFlagsToCodecFlags(sampleFlags: Int): Int {
    var flags = 0
    // 同步帧（关键帧）
    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
        flags = flags or MediaCodec.BUFFER_FLAG_SYNC_FRAME
        // 某些版本也支持 KEY_FRAME 标记（更语义化），加上也没问题
        if (Build.VERSION.SDK_INT >= 21) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
    }
    // 部分帧（Android 8.0+ 才有）
    if (Build.VERSION.SDK_INT >= 26 && (sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
        flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
    }
    // ENCRYPTED 标志不用直接映射到 MediaCodec 的 flags；如果有 DRM/加密流，这里通常不做直通复用
    return flags
}
