package com.example.passwordstorageapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,             // Unique ID, handled by Room
    val serviceName: String,     // e.g. "Google"
    val username: String,        // e.g. "me@gmail.com"
    val password: String,        // Plain text for Phase 1
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)