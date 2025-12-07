package com.example.passwordstorageapp.encryption

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.nio.charset.StandardCharsets

class CryptoManager(
    private val key : ByteArray
) {

    init{
        require(key.size==32){
            "Expected 32-byte key for AES-256, got ${key.size}"
        }
    }

    fun encrypt(plainText : String) : EncryptedData{
        val secretKey = SecretKeySpec(key, "AES")
        val iv = ByteArray(12).also{
            SecureRandom().nextBytes(it)
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128,iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return EncryptedData(iv, cipherText)
    }

    fun decrypt(encryptedData: EncryptedData) : String{
        require(encryptedData.iv.size==12){
            "Invalid IV size: ${encryptedData.iv.size}"
        }
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val plainBytes = cipher.doFinal(encryptedData.cipherText)
        val plainText = String(plainBytes, StandardCharsets.UTF_8)
        return plainText
    }
}

data class EncryptedData(
    val iv : ByteArray,
    val cipherText : ByteArray
)