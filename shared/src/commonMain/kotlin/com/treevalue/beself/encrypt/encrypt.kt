package com.treevalue.beself.encrypt

private const val ENCRYPTION_KEY = "BrowserApp2025Key!" // 16字节密钥
private const val XOR_SEED = 0x5A

/**
 * 简单的XOR加密/解密（高性能）
 */
fun simpleEncrypt(data: String): ByteArray {
    val bytes = data.toByteArray(Charsets.UTF_8)
    val key = ENCRYPTION_KEY.toByteArray(Charsets.UTF_8)

    for (i in bytes.indices) {
        bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt() xor XOR_SEED).toByte()
    }

    // 添加简单的混淆头部
    val header = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
    return header + bytes
}

/**
 * 简单的XOR解密（高性能）
 */
fun simpleDecrypt(encryptedData: ByteArray): String? {
    return try {
        // 检查头部
        if (encryptedData.size < 4) return null
        val expectedHeader = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        for (i in 0..3) {
            if (encryptedData[i] != expectedHeader[i]) return null
        }

        // 提取实际数据
        val actualData = encryptedData.sliceArray(4 until encryptedData.size)
        val key = ENCRYPTION_KEY.toByteArray(Charsets.UTF_8)

        for (i in actualData.indices) {
            actualData[i] = (actualData[i].toInt() xor key[i % key.size].toInt() xor XOR_SEED).toByte()
        }

        actualData.toString(Charsets.UTF_8)
    } catch (e: Exception) {
        
        null
    }
}
