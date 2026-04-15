package com.translator.universal.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

@Singleton
class ThemeManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_AMOLED = 2
        const val THEME_SYSTEM = 3

        private val KEY_THEME = intPreferencesKey("theme_mode")
    }

    val currentTheme: Flow<Int> = context.themeDataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: THEME_SYSTEM
    }

    suspend fun setTheme(themeMode: Int) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME] = themeMode
        }
        applyTheme(themeMode)
    }

    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_AMOLED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    fun getThemeName(themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> "Açık"
            THEME_DARK -> "Koyu"
            THEME_AMOLED -> "AMOLED"
            THEME_SYSTEM -> "Sistem"
            else -> "Sistem"
        }
    }
}