package com.translator.universal.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "translator_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_SOURCE_LANGUAGE = stringPreferencesKey("source_language")
        private val KEY_TARGET_LANGUAGE = stringPreferencesKey("target_language")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_AUTO_DETECT = booleanPreferencesKey("auto_detect")
        private val KEY_OFFLINE_MODE = booleanPreferencesKey("offline_mode")
    }

    val sourceLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SOURCE_LANGUAGE] ?: "auto"
    }

    val targetLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TARGET_LANGUAGE] ?: "tr"
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    val autoDetect: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_DETECT] ?: true
    }

    val offlineMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_OFFLINE_MODE] ?: false
    }

    suspend fun setSourceLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SOURCE_LANGUAGE] = languageCode
        }
    }

    suspend fun setTargetLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TARGET_LANGUAGE] = languageCode
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setAutoDetect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_DETECT] = enabled
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OFFLINE_MODE] = enabled
        }
    }
}