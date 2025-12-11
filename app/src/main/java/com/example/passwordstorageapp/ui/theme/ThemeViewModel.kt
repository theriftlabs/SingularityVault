package com.example.passwordstorageapp.ui.theme

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.content.Context

// Extension DataStore on Context
private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.themeDataStore

    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_enabled")

    // Expose a Flow<Boolean> to the UI
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DARK_THEME_KEY] ?: true   // default: dark enabled
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DARK_THEME_KEY] = enabled
            }
        }
    }
}
