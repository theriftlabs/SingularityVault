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

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private var cryptoManager : CryptoManager? = null

    fun setKey(key : ByteArray){
        cryptoManager = CryptoManager(key)
    }

    fun clearKey(){
        cryptoManager = null
    }

    val entries: StateFlow<List<VaultEntry>> =
        repository.getAllEntries()
            .map{ list ->
                val crypto = cryptoManager
                if(crypto == null){
                    emptyList()
                }
                else{
                    list.map{
                        decryptEntry(it, crypto)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private fun decryptEntry(vaultEntry: VaultEntry, cryptoManager: CryptoManager) : VaultEntry{
        return vaultEntry.copy(
            username = cryptoManager.decryptFromBase64(vaultEntry.username),
            password = cryptoManager.decryptFromBase64(vaultEntry.password),
            notes = vaultEntry.notes?.let{
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
        }?.let{
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

    fun updateEntry(vaultEntry: VaultEntry){
        val crypto = cryptoManager
            ?: throw IllegalArgumentException("Vault is locked - no key set in VaultViewModel")

        val encryptedUsername = crypto.encryptToBase64(vaultEntry.username)
        val encryptedPassword = crypto.encryptToBase64(vaultEntry.password)
        val encryptedNotes = vaultEntry.notes?.takeIf {
            it.isNotBlank()
        }?.let{
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
}