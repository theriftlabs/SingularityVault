package com.example.passwordstorageapp.feature.auth

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class MasterPasswordRepository(context : Context) {
    private val prefs = context.getSharedPreferences("master_password_prefs", Context.MODE_PRIVATE)

    companion object{
        private const val KEY_PASSWORD_HASH  = "master_password_hash"
        private const val KEY_SALT = "key_salt"
    }

    fun isMasterPasswordSet(): Boolean{
        return prefs.contains(KEY_PASSWORD_HASH) && prefs.contains(KEY_SALT)
    }

    fun setMasterPassword(password : String){
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            100_000,
            256
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedKey = factory.generateSecret(spec).encoded
        spec.clearPassword()

        val saltString = Base64.encodeToString(salt, Base64.NO_WRAP)
        val keyString = Base64.encodeToString(derivedKey, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_SALT, saltString)
            .putString(KEY_PASSWORD_HASH, keyString)
            .apply()
    }

    fun verifyPassword(password : String): ByteArray?{
        val storedKeyString = prefs.getString(KEY_PASSWORD_HASH, null) ?: return null
        val storedSaltString = prefs.getString(KEY_SALT, null) ?: return null
        val storedSaltBytes = Base64.decode(storedSaltString, Base64.NO_WRAP)
        val storedKeyBytes = Base64.decode(storedKeyString, Base64.NO_WRAP)

        val spec = PBEKeySpec(
            password.toCharArray(),
            storedSaltBytes,
            100_000,
            256
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val inputKey = factory.generateSecret(spec).encoded
        spec.clearPassword()

        if(storedKeyBytes.contentEquals(inputKey)){
            return inputKey
        }
        return null
    }
}