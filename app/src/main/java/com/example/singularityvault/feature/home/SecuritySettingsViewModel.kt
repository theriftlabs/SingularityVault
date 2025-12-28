package com.riftlabs.singularityvault.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val repository: SecuritySettingsRepository
) : ViewModel() {

    private var clipboardClearJob: Job? = null

    val settings: StateFlow<SecuritySettings> =
        repository.settingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SecuritySettings(
                    idleTimeoutEnabled = true,
                    idleTimeoutMs = 30_000L,
                    clipboardClearEnabled = true,
                    clipboardClearMs = 20_000L,
                    lockOnBackground = true
                )
            )

    // ---------- Mutations ----------

    fun setIdleTimeout(
        enabled: Boolean,
        timeoutMs: Long
    ) {
        viewModelScope.launch {
            repository.setIdleTimeout(
                enabled = enabled,
                timeoutMs = timeoutMs
            )
        }
    }

    fun onClipboardCopied(
        clipboard: ClipboardManager,
        label: String,
        value: String
    ) {
        val currentSettings = settings.value

        clipboard.setPrimaryClip(
            ClipData.newPlainText(label, value)
        )

        if (!currentSettings.clipboardClearEnabled) return

        clipboardClearJob?.cancel()
        clipboardClearJob = viewModelScope.launch {
            delay(currentSettings.clipboardClearMs)
            clipboard.setPrimaryClip(
                ClipData.newPlainText("Cleared", "")
            )
        }
    }

    fun setClipboardClear(
        enabled: Boolean,
        timeoutMs: Long
    ) {
        viewModelScope.launch {
            repository.setClipboardClear(
                enabled = enabled,
                timeoutMs = timeoutMs
            )
        }
    }

    fun setLockOnBackground(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            repository.setLockOnBackground(enabled)
        }
    }
}

/**
 * Factory because this ViewModel has a constructor parameter.
 * No DI framework required.
 */
class SecuritySettingsViewModelFactory(
    private val repository: SecuritySettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecuritySettingsViewModel::class.java)) {
            return SecuritySettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}