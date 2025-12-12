package com.example.passwordstorageapp.feature.auth

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small helper for AndroidKeyStore AES/GCM usage tied to biometric auth.
 *
 * Exposes:
 *  - getEncryptCipher() : Cipher  (init for ENCRYPT_MODE; caller will use BiometricPrompt.CryptoObject)
 *  - getDecryptCipher(iv) : Cipher (init for DECRYPT_MODE with provided iv)
 *  - persistEncryptedDerivedKey(iv, ciphertext) : store combined blob in prefs
 *  - loadEncryptedBlob() : ByteArray? (returns combined iv+ciphertext)
 *  - clearStoredDerivedKey(), deleteKeystoreKey()
 *
 * Notes:
 *  - Key is created with setUserAuthenticationRequired(true) and validity -1 so each auth must occur.
 *  - This code is minimal and designed to be compatible with BiometricPrompt crypto flows.
 */
class BiometricKeyStoreManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)
    private val KEY_ALIAS = "nano_vault_biometric_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val PREF_KEY_BLOB = "encrypted_derived_key"

    private fun ensureKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) {
            return existing
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            // Require biometric each time; validitySeconds = -1 means passport-style (auth per op)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGen.init(spec)
        return keyGen.generateKey()
    }

    /**
     * Returns a Cipher initialized for AES/GCM/NoPadding ENCRYPT_MODE.
     * Caller will wrap it in BiometricPrompt.CryptoObject and authenticate.
     */
    fun getEncryptCipher(): Cipher {
        val secret = ensureKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        return cipher
    }

    /**
     * Returns a Cipher initialized for DECRYPT_MODE using provided iv.
     * Throws if key is missing or cipher init fails (caller should handle).
     */
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) ?: throw IllegalStateException("Keystore key missing")
        if (existing !is SecretKey) throw IllegalStateException("Keystore key not secret")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, existing, GCMParameterSpec(128, iv))
        return cipher
    }

    /**
     * Persist combined iv + ciphertext into prefs (Base64).
     * iv and ciphertext must be raw bytes (iv typically 12 bytes for GCM).
     */
    fun persistEncryptedDerivedKey(iv: ByteArray, ciphertext: ByteArray) {
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        val b64 = Base64.encodeToString(combined, Base64.NO_WRAP)
        prefs.edit().putString(PREF_KEY_BLOB, b64).apply()
    }

    /**
     * Returns raw combined blob (iv + ciphertext) or null.
     */
    fun loadEncryptedBlob(): ByteArray? {
        val s = prefs.getString(PREF_KEY_BLOB, null) ?: return null
        return Base64.decode(s, Base64.NO_WRAP)
    }

    /**
     * Clear stored blob from prefs (does not touch keystore key).
     */
    fun clearStoredDerivedKey() {
        prefs.edit().remove(PREF_KEY_BLOB).apply()
    }

    /**
     * Delete the keystore key entry if present.
     */
    fun deleteKeystoreKey() {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) {
                ks.deleteEntry(KEY_ALIAS)
            }
        } catch (ignored: Exception) {
            // best-effort; don't crash
        }
    }
}
