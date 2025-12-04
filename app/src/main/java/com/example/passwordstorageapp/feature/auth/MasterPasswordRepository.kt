package com.example.passwordstorageapp.feature.auth

import android.content.Context
import java.security.MessageDigest

class MasterPasswordRepository(context : Context) {
    private val prefs = context.getSharedPreferences("master_password_prefs", Context.MODE_PRIVATE)

    companion object{
        private const val KEY_PASSWORD_HASH = "master_password_hash"
    }

    fun isMasterPasswordSet(): Boolean{
        return prefs.contains(KEY_PASSWORD_HASH)
    }

    fun setMasterPassword(password : String){
        val hash = hashPassword(password)
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .apply()
    }

    fun verifyPassword(password : String): Boolean{
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }

    private fun hashPassword(password : String): String{
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}