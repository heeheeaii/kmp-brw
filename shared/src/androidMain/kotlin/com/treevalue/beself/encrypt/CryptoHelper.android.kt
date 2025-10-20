package com.treevalue.beself.encrypt
// androidMain
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

actual class CryptoHelper {
    actual fun generateKey(): String {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(128)
        val secretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    actual fun encrypt(data: String, key: String): String {
        val secretKey = SecretKeySpec(Base64.decode(key, Base64.NO_WRAP), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    actual fun decrypt(encryptedData: String, key: String): String {
        val secretKey = SecretKeySpec(Base64.decode(key, Base64.NO_WRAP), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
        return String(decryptedBytes)
    }
}
