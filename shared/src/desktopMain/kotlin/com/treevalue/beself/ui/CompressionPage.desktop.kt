package com.treevalue.beself.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.humble.video.Demuxer
import io.humble.video.DemuxerStream
import io.humble.video.MediaDescriptor
import io.humble.video.MediaPacket
import io.humble.video.Muxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberPlatformImageVideoOps(): ImageVideoOps {
    return remember {
        object : ImageVideoOps {
            override suspend fun pickImages(): List<String> = withContext(Dispatchers.IO) {
                fileChooser(
                    title = "选择图片",
                    multi = true,
                    filters = listOf(
                        FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp", "webp")
                    )
                )
            }

            override suspend fun pickVideos(): List<String> = withContext(Dispatchers.IO) {
                fileChooser(
                    title = "选择视频",
                    multi = true,
                    filters = listOf(
                        FileNameExtensionFilter("Videos", "mp4", "mov", "m4v", "mkv", "avi", "webm")
                    )
                )
            }

            override suspend fun pickDirectory(): String = withContext(Dispatchers.IO) {
                dirChooser("选择输出目录") ?: ""
            }

            override suspend fun compressImages(
                inputs: List<String>,
                outDir: String,
                qualityPercent: Int,
                logger: (String) -> Unit,
                onProgress: (done: Int, total: Int) -> Unit,
            ): Pair<Int, Int> = withContext(Dispatchers.IO) {
                val base = File(outDir).apply { mkdirs() }
                var ok = 0
                var fail = 0
                val q = qualityPercent.coerceIn(1, 100) / 100f
                val total = inputs.size
                var done = 0

                for (p in inputs) {
                    try {
                        val src = File(p)
                        val img = ImageIO.read(src) ?: throw IllegalStateException("无法读取图片")
                        val outFile = avoidOverwrite(base, src.nameWithoutExtension + ".jpg")

                        val writers: Iterator<ImageWriter> = ImageIO.getImageWritersByFormatName("jpg")
                        val writer = if (writers.hasNext()) writers.next()
                        else throw IllegalStateException("无 JPEG 编码器")

                        val ios: ImageOutputStream = ImageIO.createImageOutputStream(outFile)
                        writer.output = ios
                        val param = writer.defaultWriteParam
                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        param.compressionQuality = q
                        writer.write(null, IIOImage(img, null, null), param)
                        ios.close()
                        writer.dispose()

                        logger("✅ ${src.name} -> ${outFile.name}")
                        ok++
                    } catch (e: Throwable) {
                        logger("❌ $p 失败: ${e.message}")
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
                val base = File(outDir).apply { mkdirs() }
                var ok = 0
                var fail = 0
                val total = inputs.size
                var done = 0

                for (p in inputs) {
                    var demuxer: Demuxer? = null
                    var muxer: Muxer? = null

                    try {
                        val src = File(p)
                        if (!src.exists() || !src.isFile) {
                            logger("❌ $p 失败: 文件不存在或不可读")
                            fail++
                            continue
                        }

                        val outFile = avoidOverwrite(base, src.nameWithoutExtension + ".mp4")

                        demuxer = Demuxer.make()
                        demuxer.open(src.absolutePath, null, false, true, null, null)

                        var videoStreamId = -1
                        var videoStream: DemuxerStream? = null

                        // 查找视频流
                        for (i in 0 until demuxer.numStreams) {
                            val stream = demuxer.getStream(i)
                            val streamDecoder = stream.decoder
                            if (streamDecoder?.codecType == MediaDescriptor.Type.MEDIA_VIDEO) {
                                videoStreamId = i
                                videoStream = stream
                                break
                            }
                        }

                        if (videoStreamId == -1 || videoStream == null) {
                            logger("❌ $p 失败: 未找到视频流")
                            fail++
                            continue
                        }

                        // 使用流复制模式 - 完整保留元数据
                        muxer = Muxer.make(outFile.absolutePath, null, "mp4")

                        val decoder = videoStream.decoder ?: throw IllegalStateException("无法获取解码器")
                        val outStream = muxer.addNewStream(decoder)

                        // 完整复制视频流的所有元数据
                        try {
                            val metadata = videoStream.metaData
                            if (metadata != null) {
                                val keys = metadata.keys
                                for (key in keys) {
                                    try {
                                        val value = metadata.getValue(key)
                                        outStream.metaData?.setValue(key, value)
                                    } catch (e: Exception) {
                                        // 某些元数据可能无法设置，继续处理其他的
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger("⚠️ 复制元数据时出现警告: ${e.message}")
                        }

                        muxer.open(null, null)

                        // 直接复制视频流数据包（跳过音频流）
                        val packet = MediaPacket.make()
                        while (demuxer.read(packet) >= 0) {
                            if (packet.streamIndex == videoStreamId) {
                                packet.streamIndex = 0  // 输出流索引
                                muxer.write(packet, false)
                            }
                            // 其他流（音频等）直接跳过
                        }

                        muxer.close()

                        val sizeBefore = src.length() / 1024 / 1024
                        val sizeAfter = outFile.length() / 1024 / 1024
                        logger("✅ ${src.name} -> ${outFile.name} (${sizeBefore}MB -> ${sizeAfter}MB) [流复制-保留所有元数据]")

                        ok++

                    } catch (e: Throwable) {
                        logger("❌ $p 失败: ${e.message}")
                        fail++
                    } finally {
                        try {
                            muxer?.delete()
                            demuxer?.delete()
                        } catch (_: Exception) {
                            // 忽略清理错误
                        }
                        done++
                        onProgress(done, total)
                    }
                }

                ok to fail
            }

            /**
             * 流复制模式 - 直接复制视频流,不重新编码
             * 速度极快,文件大小通常小于或等于原始大小,无质量损失
             * 会自动保留所有元数据，包括旋转信息
             */
            private fun streamCopyMode(
                src: File,
                outFile: File,
                demuxer: Demuxer,
                videoStreamId: Int,
                videoStream: DemuxerStream,
                logger: (String) -> Unit,
            ) {
                val muxer = Muxer.make(outFile.absolutePath, null, "mp4")

                // 直接添加视频流,使用原始编码器参数
                val decoder = videoStream.decoder ?: throw IllegalStateException("无法获取解码器")
                val outStream = muxer.addNewStream(decoder)

                // 复制所有元数据（包括旋转信息）
                try {
                    val metadata = videoStream.metaData
                    if (metadata != null) {
                        val keys = metadata.keys
                        for (key in keys) {
                            val value = metadata.getValue(key)
                            try {
                                outStream.metaData?.setValue(key, value)
                            } catch (e: Exception) {
                                // 某些元数据可能无法设置，忽略
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger("⚠️ 复制元数据失败: ${e.message}")
                }

                muxer.open(null, null)

                // 直接复制数据包
                val packet = MediaPacket.make()
                while (demuxer.read(packet) >= 0) {
                    if (packet.streamIndex == videoStreamId) {
                        packet.streamIndex = 0
                        muxer.write(packet, false)
                    }
                }

                muxer.close()
                muxer.delete()
            }
        }
    }
}

private fun fileChooser(
    title: String,
    multi: Boolean,
    filters: List<FileNameExtensionFilter>,
): List<String> {
    val fc = JFileChooser().apply {
        dialogTitle = title
        isMultiSelectionEnabled = multi
        fileSelectionMode = JFileChooser.FILES_ONLY
        resetChoosableFileFilters()
        for (f in filters) addChoosableFileFilter(f)
        isAcceptAllFileFilterUsed = true
    }
    val result = fc.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        return if (multi) fc.selectedFiles?.map { it.absolutePath } ?: emptyList()
        else listOfNotNull(fc.selectedFile?.absolutePath)
    }
    return emptyList()
}

private fun dirChooser(title: String): String? {
    val fc = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
    }
    val result = fc.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) fc.selectedFile?.absolutePath else null
}

private fun avoidOverwrite(baseDir: File, name: String): File {
    val dot = name.lastIndexOf('.')
    val stem = if (dot >= 0) name.substring(0, dot) else name
    val ext = if (dot >= 0) name.substring(dot) else ""
    var idx = 0
    while (true) {
        val f = File(baseDir, if (idx == 0) "$stem$ext" else "$stem($idx)$ext")
        if (!f.exists()) return f
        idx++
    }
}
