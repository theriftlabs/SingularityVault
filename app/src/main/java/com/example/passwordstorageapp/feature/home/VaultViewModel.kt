package com.example.passwordstorageapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordstorageapp.data.VaultEntry
import com.example.passwordstorageapp.encryption.CryptoManager
import com.example.passwordstorageapp.encryption.decryptFromBase64
import com.example.passwordstorageapp.encryption.encryptToBase64
import data.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private var cryptoManager: CryptoManager? = null

    fun setKey(key: ByteArray) {
        cryptoManager = CryptoManager(key)
    }

    fun clearKey() {
        cryptoManager = null
    }

    val entries: StateFlow<List<VaultEntry>?> =
        repository.getAllEntries()
            .map { list ->
                val crypto = cryptoManager
                if (crypto == null) {
                    emptyList()
                } else {
                    // defensive: if a single entry fails to decrypt, skip it instead of crashing
                    list.mapNotNull { stored ->
                        try {
                            decryptEntry(stored, crypto)
                        } catch (e: Exception) {
                            // skip corrupted / wrong-key items
                            null
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private fun decryptEntry(vaultEntry: VaultEntry, cryptoManager: CryptoManager): VaultEntry {
        return vaultEntry.copy(
            username = cryptoManager.decryptFromBase64(vaultEntry.username),
            password = cryptoManager.decryptFromBase64(vaultEntry.password),
            notes = vaultEntry.notes?.let {
                cryptoManager.decryptFromBase64(it)
            }
        )
    }

    fun addEntry(
        service: String,
        username: String,
        password: String,
        notes: String?
    ) {
        val crypto = cryptoManager
            ?: throw IllegalArgumentException("Vault is locked - no key set in VaultViewModel")

        val encryptedUsername = crypto.encryptToBase64(username)
        val encryptedPassword = crypto.encryptToBase64(password)
        val encryptedNotes = notes?.takeIf {
            it.isNotBlank()
        }?.let {
            crypto.encryptToBase64(it)
        }

        val entry = VaultEntry(
            serviceName = service,
            username = encryptedUsername,
            password = encryptedPassword,
            notes = encryptedNotes
        )

        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }

    fun updateEntry(vaultEntry: VaultEntry) {
        val crypto = cryptoManager
            ?: throw IllegalArgumentException("Vault is locked - no key set in VaultViewModel")

        val encryptedUsername = crypto.encryptToBase64(vaultEntry.username)
        val encryptedPassword = crypto.encryptToBase64(vaultEntry.password)
        val encryptedNotes = vaultEntry.notes?.takeIf {
            it.isNotBlank()
        }?.let {
            crypto.encryptToBase64(it)
        }

        val encryptedEntry = vaultEntry.copy(
            username = encryptedUsername,
            password = encryptedPassword,
            notes = encryptedNotes
        )

        viewModelScope.launch {
            repository.insertEntry(encryptedEntry)
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    // Re-encrypt all entries safely using oldKey -> newKey
    // onComplete is invoked when finished (success/failure not reported separately here;
    // caller may verify outcomes by catching errors or checking logs)
    fun reencryptAll(oldKey: ByteArray, newKey: ByteArray, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val oldCrypto = CryptoManager(oldKey)
                val newCrypto = CryptoManager(newKey)

                // get raw stored entries once (these are still encrypted with the old key)
                val storedList = repository.getAllEntries().first()

                storedList.forEach { stored ->
                    // decrypt with old key
                    val decryptedUsername = oldCrypto.decryptFromBase64(stored.username)
                    val decryptedPassword = oldCrypto.decryptFromBase64(stored.password)
                    val decryptedNotes = stored.notes?.let { oldCrypto.decryptFromBase64(it) }

                    // encrypt with new key
                    val reEncryptedUsername = newCrypto.encryptToBase64(decryptedUsername)
                    val reEncryptedPassword = newCrypto.encryptToBase64(decryptedPassword)
                    val reEncryptedNotes = decryptedNotes?.let { newCrypto.encryptToBase64(it) }

                    val replaced = stored.copy(
                        username = reEncryptedUsername,
                        password = reEncryptedPassword,
                        notes = reEncryptedNotes
                    )

                    repository.insertEntry(replaced)
                }
            } finally {
                // wipe sensitive buffers at caller level if needed; invoke completion
                onComplete?.invoke()
            }
        }
    }
}
