package data

import com.example.passwordstorageapp.data.VaultDao
import com.example.passwordstorageapp.data.VaultEntry

class VaultRepository(
    private val dao: VaultDao
) {
    fun getAllEntries() = dao.getAllEntries()

    suspend fun getEntryById(id: Int) = dao.getEntryById(id)

    suspend fun insertEntry(entry: VaultEntry) = dao.insertEntry(entry)

    suspend fun deleteEntry(entry: VaultEntry) = dao.deleteEntry(entry)
}