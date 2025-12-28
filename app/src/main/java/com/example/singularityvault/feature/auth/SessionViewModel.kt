package com.riftlabs.singularityvault.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    var vaultKey: ByteArray? = null

    var isUnlocked: Boolean = false
        private set

    private var lastInteractionTime = android.os.SystemClock.elapsedRealtime()
    private var idleJob: Job? = null

    fun markUnlocked() {
        isUnlocked = true
        touch()
    }

    fun markLocked() {
        isUnlocked = false
        vaultKey = null
        stopIdleWatcher()
    }

    fun touch() {
        lastInteractionTime = android.os.SystemClock.elapsedRealtime()
    }

    fun startIdleWatcher(
        idleTimeoutEnabled: Boolean,
        idleTimeoutMs: Long,
        onLock: () -> Unit
    ) {
        // Cancel any existing watcher first
        idleJob?.cancel()
        idleJob = null

        // Don't start if disabled or not unlocked
        if (!idleTimeoutEnabled || !isUnlocked) return

        idleJob = viewModelScope.launch {
            while (true) {
                val now = android.os.SystemClock.elapsedRealtime()
                val timeSinceInteraction = now - lastInteractionTime
                val remaining = idleTimeoutMs - timeSinceInteraction

                if (remaining <= 0) {
                    onLock()
                    break
                }

                // Wait for remaining time, but check every second
                delay(remaining.coerceAtMost(1_000L))
            }
        }
    }

    fun stopIdleWatcher() {
        idleJob?.cancel()
        idleJob = null
    }
}
