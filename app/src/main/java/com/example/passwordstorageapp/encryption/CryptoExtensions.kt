package com.example.passwordstorageapp.encryption

import android.util.Base64

fun CryptoManager.encryptToBase64(plainText : String) : String{
    val encrypted = encrypt(plainText)
    val iv = encrypted.iv
    val cipher = encrypted.cipherText

    val combined = ByteArray(iv.size + cipher.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(cipher, 0, combined, iv.size, cipher.size)

    return Base64.encodeToString(combined, Base64.NO_WRAP)
}

fun CryptoManager.decryptFromBase64(stored : String) : String{
    val combined = Base64.decode(stored, Base64.NO_WRAP)
    if(combined.size < 13){
        throw IllegalArgumentException("Encrypted data too short")
    }

    val ivSize = 12
    val iv = combined.copyOfRange(0, ivSize)
    val cipherText = combined.copyOfRange(ivSize, combined.size)

    val encryptedData = EncryptedData(iv = iv, cipherText = cipherText)
    return decrypt(encryptedData)
}