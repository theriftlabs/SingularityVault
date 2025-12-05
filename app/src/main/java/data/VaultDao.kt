package com.example.passwordstorageapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    // Insert a new password
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntry)

    // Delete a password
    @Delete
    suspend fun deleteEntry(entry: VaultEntry)

    // Get all passwords (updates automatically)
    @Query("SELECT * FROM vault_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<VaultEntry>>

    // Get one password by ID
    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): VaultEntry?
}