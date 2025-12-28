package com.riftlabs.singularityvault.feature.home

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ---------- DataStore ----------
private val Context.securityDataStore by preferencesDataStore(
    name = "security_settings"
)

// ---------- Keys ----------
private val KEY_IDLE_TIMEOUT_ENABLED =
    booleanPreferencesKey("idle_timeout_enabled")

private val KEY_IDLE_TIMEOUT_MS =
    longPreferencesKey("idle_timeout_ms")

private val KEY_CLIPBOARD_CLEAR_ENABLED =
    booleanPreferencesKey("clipboard_clear_enabled")

private val KEY_CLIPBOARD_CLEAR_MS =
    longPreferencesKey("clipboard_clear_ms")

private val KEY_LOCK_ON_BACKGROUND =
    booleanPreferencesKey("lock_on_background")

// ---------- Model ----------
data class SecuritySettings(
    val idleTimeoutEnabled: Boolean,
    val idleTimeoutMs: Long,

    val clipboardClearEnabled: Boolean,
    val clipboardClearMs: Long,

    val lockOnBackground: Boolean
)

// ---------- Repository ----------
class SecuritySettingsRepository(
    private val context: Context
) {

    val settingsFlow: Flow<SecuritySettings> =
        context.securityDataStore.data.map { prefs ->
            SecuritySettings(
                idleTimeoutEnabled =
                    prefs[KEY_IDLE_TIMEOUT_ENABLED] ?: true,

                idleTimeoutMs =
                    prefs[KEY_IDLE_TIMEOUT_MS] ?: 30_000L,

                clipboardClearEnabled =
                    prefs[KEY_CLIPBOARD_CLEAR_ENABLED] ?: true,

                clipboardClearMs =
                    prefs[KEY_CLIPBOARD_CLEAR_MS] ?: 20_000L,

                lockOnBackground =
                    prefs[KEY_LOCK_ON_BACKGROUND] ?: true
            )
        }

    // ---------- Update functions ----------

    suspend fun setIdleTimeout(
        enabled: Boolean,
        timeoutMs: Long
    ) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_IDLE_TIMEOUT_ENABLED] = enabled
            prefs[KEY_IDLE_TIMEOUT_MS] = timeoutMs
        }
    }

    suspend fun setClipboardClear(
        enabled: Boolean,
        timeoutMs: Long
    ) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_CLIPBOARD_CLEAR_ENABLED] = enabled
            prefs[KEY_CLIPBOARD_CLEAR_MS] = timeoutMs
        }
    }

    suspend fun setLockOnBackground(
        enabled: Boolean
    ) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_LOCK_ON_BACKGROUND] = enabled
        }
    }
}