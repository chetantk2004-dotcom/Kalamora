package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kalamoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "kalamora_prefs")

class PreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val interfaceLanguageKey = stringPreferencesKey("interface_language")

    val themeModeFlow: Flow<String> = context.applicationContext.kalamoraDataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "system"
    }

    val interfaceLanguageFlow: Flow<String> = context.applicationContext.kalamoraDataStore.data.map { prefs ->
        prefs[interfaceLanguageKey] ?: "en"
    }

    suspend fun setThemeMode(mode: String) {
        val safeMode = if (mode in listOf("system", "light", "dark")) mode else "system"
        context.applicationContext.kalamoraDataStore.edit { prefs ->
            prefs[themeModeKey] = safeMode
        }
    }

    suspend fun setInterfaceLanguage(lang: String) {
        val safeLang = if (lang in listOf("en", "hi")) lang else "en"
        context.applicationContext.kalamoraDataStore.edit { prefs ->
            prefs[interfaceLanguageKey] = safeLang
        }
    }
}
