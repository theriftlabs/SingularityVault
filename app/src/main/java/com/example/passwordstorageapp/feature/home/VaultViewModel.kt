package com.example.passwordstorageapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordstorageapp.data.VaultEntry
import data.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    val entries: StateFlow<List<VaultEntry>> =
        repository.getAllEntries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addEntry(
        service: String,
        username: String,
        password: String,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertEntry(
                VaultEntry(
                    serviceName = service,
                    username = username,
                    password = password,
                    notes = notes
                )
            )
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }
}