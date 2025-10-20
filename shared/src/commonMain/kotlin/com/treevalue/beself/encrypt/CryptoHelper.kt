package com.treevalue.beself.encrypt

expect class CryptoHelper() {
    fun generateKey(): String
    fun encrypt(data: String, key: String): String
    fun decrypt(encryptedData: String, key: String): String
}
