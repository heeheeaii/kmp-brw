package com.treevalue.beself.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object FileSplitMergeUtil {
    private const val BUFFER_SIZE = 8192

    data class SplitRecord(
        val originalFileName: String,
        val totalSha256: String,
        val totalSize: Long,
        val splitSize: Long,
        val parts: List<PartInfo>,
    )

    data class PartInfo(
        val index: Int,
        val fileName: String,
        val sha256: String,
        val size: Long,
    )

    /**
     * 计算文件的 SHA-256
     */
    fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 分割文件
     */
    fun splitFile(
        inputFile: File,
        outDir: File,
        splitSizeMB: Int,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Boolean {
        try {
            if (!inputFile.exists() || !inputFile.isFile) {
                return false
            }

            outDir.mkdirs()

            val totalSha = calculateSHA256(inputFile)
            val totalSize = inputFile.length()
            val splitSizeBytes = splitSizeMB * 1024L * 1024L

            val baseName = inputFile.nameWithoutExtension
            val ext = inputFile.extension
            val parts = mutableListOf<PartInfo>()

            FileInputStream(inputFile).use { fis ->
                var partIndex = 0
                var totalRead = 0L
                var buffer = ByteArray(BUFFER_SIZE)

                while (totalRead < totalSize) {
                    val partNum = String.format("%03d", partIndex)
                    val partFileName = "${baseName}_${partNum}.${ext}"
                    val partFile = File(outDir, partFileName)

                    FileOutputStream(partFile).use { fos ->
                        var partSize = 0L
                        val partDigest = MessageDigest.getInstance("SHA-256")

                        while (partSize < splitSizeBytes && totalRead < totalSize) {
                            val toRead =
                                minOf(buffer.size.toLong(), splitSizeBytes - partSize, totalSize - totalRead).toInt()
                            val bytesRead = fis.read(buffer, 0, toRead)
                            if (bytesRead == -1) break

                            fos.write(buffer, 0, bytesRead)
                            partDigest.update(buffer, 0, bytesRead)
                            partSize += bytesRead
                            totalRead += bytesRead

                            onProgress(totalRead.toInt(), totalSize.toInt())
                        }

                        val partSha = partDigest.digest().joinToString("") { "%02x".format(it) }
                        parts.add(PartInfo(partIndex, partFileName, partSha, partSize))
                    }

                    partIndex++
                }
            }

            // 生成分割记录文件
            val record = SplitRecord(
                originalFileName = inputFile.name,
                totalSha256 = totalSha,
                totalSize = totalSize,
                splitSize = splitSizeBytes,
                parts = parts
            )

            val recordFile = File(outDir, "${baseName}_split_record.txt")
            recordFile.writeText(buildRecordText(record))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 合并文件
     */
    fun mergeFile(
        inputDir: File,
        outDir: File,
        logger: (String) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Boolean {
        try {
            if (!inputDir.exists() || !inputDir.isDirectory) {
                return false
            }

            // 查找分割记录文件
            val recordFile = inputDir.listFiles()?.firstOrNull {
                it.name.endsWith("_split_record.txt")
            }

            if (recordFile == null) {
                logger("❌ 未找到分割记录文件")
                return false
            }

            logger("📝 读取分割记录: ${recordFile.name}")
            val record = parseRecordText(recordFile.readText())


            for (part in record.parts) {
                val partFile = File(inputDir, part.fileName)
                if (!partFile.exists()) {
                    logger("❌ 缺失分割块: ${part.fileName}")
                    return false
                }

                logger("🔍 校验: ${part.fileName}...")
                val actualSha = calculateSHA256(partFile)
                if (actualSha != part.sha256) {
                    logger("❌ 分割块 ${part.fileName} SHA-256 校验失败")
                    logger("   预期: ${part.sha256}")
                    logger("   实际: $actualSha")
                    return false
                }
                logger("✅ ${part.fileName} 校验通过")
            }

            logger("🔗 开始合并...")
            outDir.mkdirs()
            val outputFile = File(outDir, record.originalFileName)

            // 合并文件
            val totalDigest = MessageDigest.getInstance("SHA-256")
            var totalMerged = 0L

            FileOutputStream(outputFile).use { fos ->
                for (part in record.parts.sortedBy { it.index }) {
                    val partFile = File(inputDir, part.fileName)
                    FileInputStream(partFile).use { fis ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            totalDigest.update(buffer, 0, bytesRead)
                            totalMerged += bytesRead
                            onProgress(totalMerged.toInt(), record.totalSize.toInt())
                        }
                    }
                    logger("✅ 已合并: ${part.fileName}")
                }
            }

            // 校验合并后的文件
            logger("🔍 校验合并后的文件...")
            val finalSha = totalDigest.digest().joinToString("") { "%02x".format(it) }

            if (finalSha != record.totalSha256) {
                logger("❌ 合并后的文件 SHA-256 校验失败")
                logger("   预期: ${record.totalSha256}")
                logger("   实际: $finalSha")
                outputFile.delete()
                return false
            }

            logger("✅ SHA-256 校验通过！")
            logger("🎉 合并完成: ${outputFile.name}")
            logger("📊 文件大小: ${outputFile.length() / 1024 / 1024} MB")

            return true
        } catch (e: Exception) {
            logger("❌ 合并失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun buildRecordText(record: SplitRecord): String {
        return buildString {
            appendLine("=== 文件分割记录 ===")
            appendLine("原文件名: ${record.originalFileName}")
            appendLine("总大小: ${record.totalSize} bytes (${record.totalSize / 1024 / 1024} MB)")
            appendLine("分割大小: ${record.splitSize} bytes (${record.splitSize / 1024 / 1024} MB)")
            appendLine("总SHA-256: ${record.totalSha256}")
            appendLine()
            appendLine("=== 分割块信息 ===")
            for (part in record.parts) {
                appendLine("Part ${part.index}: ${part.fileName}")
                appendLine("  大小: ${part.size} bytes")
                appendLine("  SHA-256: ${part.sha256}")
                appendLine()
            }
        }
    }

    private fun parseRecordText(text: String): SplitRecord {
        val lines = text.lines()
        var originalFileName = ""
        var totalSize = 0L
        var splitSize = 0L
        var totalSha = ""
        val parts = mutableListOf<PartInfo>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            when {
                line.startsWith("原文件名:") -> originalFileName = line.substringAfter(":").trim()
                line.startsWith("总大小:") -> totalSize = line.substringAfter(":").trim().split(" ")[0].toLong()
                line.startsWith("分割大小:") -> splitSize = line.substringAfter(":").trim().split(" ")[0].toLong()
                line.startsWith("总SHA-256:") -> totalSha = line.substringAfter(":").trim()
                line.startsWith("Part ") -> {
                    val index = line.substringAfter("Part ").substringBefore(":").trim().toInt()
                    val fileName = line.substringAfter(":").trim()
                    i++
                    val size = lines[i].trim().substringAfter(":").trim().split(" ")[0].toLong()
                    i++
                    val sha = lines[i].trim().substringAfter(":").trim()
                    parts.add(PartInfo(index, fileName, sha, size))
                }
            }
            i++
        }

        return SplitRecord(originalFileName, totalSha, totalSize, splitSize, parts)
    }
}
