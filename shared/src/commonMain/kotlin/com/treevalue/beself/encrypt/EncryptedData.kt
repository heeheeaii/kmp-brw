package com.treevalue.beself.encrypt

data class EncryptedData(
    val deviceId: String,
    val startTime: Long,
    val validTime: Long,
    val decryptionPassword: String,
    val obfuscationChars: String
)
