package com.example.passwordstorageapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. Tell the database which tables to create
@Database(entities = [VaultEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 2. Expose the DAO so the app can use it
    abstract fun vaultDao(): VaultDao

    // 3. Singleton Boilerplate (Just copy-paste this, it prevents crashes)
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nano_vault_database" // <--- The actual file name on the phone
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}