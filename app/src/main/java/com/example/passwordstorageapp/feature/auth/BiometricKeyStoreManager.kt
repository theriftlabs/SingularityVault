package com.example.passwordstorageapp.feature.auth

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricKeyStoreManager(private val context : Context) {

    val prefs = context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
    private val KEY_ALIAS = "nano_vault_biometric_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    private fun getOrCreateSecretKey(): SecretKey {
        // 1. Load the Android Keystore
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        // 2. If key already exists, return it
        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey is SecretKey) {
            return existingKey
        }

        // 3. Otherwise, generate a new AES key and store it in the keystore
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val params = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            // You *can* tie this to biometric auth with user authentication flags later
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(params)

        return keyGenerator.generateKey()
    }

    fun saveDerivedKey(derivedKey : ByteArray){
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(derivedKey)
        val combined = ByteArray(iv.size+cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        val keyString = Base64.encodeToString(combined, Base64.NO_WRAP)
        prefs.edit()
            .putString("encrypted_derived_key", keyString)
            .apply()
    }

    fun loadDerivedKey(): ByteArray? {
        val keyString = prefs.getString("encrypted_derived_key", null) ?: return null
        val combined = Base64.decode(keyString, Base64.NO_WRAP)
        val ivSize = 12
        val iv = combined.copyOfRange(0, ivSize)
        val cipherText = combined.copyOfRange(ivSize, combined.size)
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val derivedKey = cipher.doFinal(cipherText)
        return derivedKey
    }
}