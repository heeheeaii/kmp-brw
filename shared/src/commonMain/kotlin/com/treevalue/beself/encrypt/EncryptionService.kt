package com.treevalue.beself.encrypt

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

object EncryptionService {
    private val cryptoHelper = CryptoHelper()

    fun generateEncryptedString(
        deviceId: String,
        startTime: Long = kotlin.time.measureTime { }.toLong(DurationUnit.MILLISECONDS),
        validTime: Duration = 1.hours,
        decryptionPassword: String,
        obfuscationChars: String
    ): String {
        val data = EncryptedData(
            deviceId = deviceId,
            startTime = startTime,
            validTime = validTime.inWholeMilliseconds,
            decryptionPassword = decryptionPassword,
            obfuscationChars = obfuscationChars
        )
        val dataString = data.toString()
        val key = cryptoHelper.generateKey()
        val encryptedData = cryptoHelper.encrypt(dataString, key)
        return "$encryptedData:$key"
    }

    fun decryptEncryptedString(encryptedString: String): EncryptedData {
        val (encryptedData, key) = encryptedString.split(":")
        val decryptedString = cryptoHelper.decrypt(encryptedData, key)

        val regex = Regex("deviceId=(\\w+), startTime=(\\d+), validTime=(\\d+), decryptionPassword=(\\w+), obfuscationChars=(\\w+)")
        val matchResult = regex.find(decryptedString)

        if (matchResult != null) {
            val deviceId = matchResult.groupValues[1]
            val startTime = matchResult.groupValues[2].toLong()
            val validTime = matchResult.groupValues[3].toLong()
            val decryptionPassword = matchResult.groupValues[4]
            val obfuscationChars = matchResult.groupValues[5]

            return EncryptedData(
                deviceId = deviceId,
                startTime = startTime,
                validTime = validTime,
                decryptionPassword = decryptionPassword,
                obfuscationChars = obfuscationChars
            )
        } else {
            throw IllegalArgumentException("Invalid decrypted string format: $decryptedString")
        }
    }}
