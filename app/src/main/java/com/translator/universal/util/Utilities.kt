package com.translator.universal.util

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class TranslationStats(
        val totalTranslations: Int = 0,
        val todayTranslations: Int = 0,
        val weekTranslations: Int = 0,
        val monthTranslations: Int = 0,
        val favoriteLanguage: String = "tr",
        val averageResponseTime: Long = 0,
        val offlineUsage: Int = 0,
        val onlineUsage: Int = 0,
        val cameraUsage: Int = 0,
        val voiceUsage: Int = 0,
        val textUsage: Int = 0
    )
    
    suspend fun getStats(): TranslationStats = withContext(Dispatchers.IO) {
        TranslationStats(
            totalTranslations = prefs.getInt("total_translations", 0),
            todayTranslations = prefs.getInt("today_translations", 0),
            weekTranslations = prefs.getInt("week_translations", 0),
            monthTranslations = prefs.getInt("month_translations", 0),
            favoriteLanguage = prefs.getString("favorite_language", "tr") ?: "tr",
            averageResponseTime = prefs.getLong("avg_response_time", 0),
            offlineUsage = prefs.getInt("offline_usage", 0),
            onlineUsage = prefs.getInt("online_usage", 0),
            cameraUsage = prefs.getInt("camera_usage", 0),
            voiceUsage = prefs.getInt("voice_usage", 0),
            textUsage = prefs.getInt("text_usage", 0)
        )
    }
    
    suspend fun recordTranslation(
        isOffline: Boolean,
        translationType: String,
        responseTime: Long
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putInt("total_translations", getInt("total_translations", 0) + 1)
            putInt("today_translations", getInt("today_translations", 0) + 1)
            putInt("week_translations", getInt("week_translations", 0) + 1)
            putInt("month_translations", getInt("month_translations", 0) + 1)
            
            if (isOffline) {
                putInt("offline_usage", getInt("offline_usage", 0) + 1)
            } else {
                putInt("online_usage", getInt("online_usage", 0) + 1)
            }
            
            when (translationType) {
                "camera" -> putInt("camera_usage", getInt("camera_usage", 0) + 1)
                "voice" -> putInt("voice_usage", getInt("voice_usage", 0) + 1)
                "text" -> putInt("text_usage", getInt("text_usage", 0) + 1)
            }
            
            val currentAvg = getLong("avg_response_time", 0)
            val total = getInt("total_translations", 1)
            val newAvg = ((currentAvg * (total - 1)) + responseTime) / total
            putLong("avg_response_time", newAvg)
            
            apply()
        }
    }
    
    suspend fun resetDailyStats() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putInt("today_translations", 0)
            apply()
        }
    }
    
    fun getUsageByHour(): Map<Int, Int> {
        // Returns usage count by hour (0-23)
        val map = mutableMapOf<Int, Int>()
        val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
        map[hour] = prefs.getInt("today_translations", 0)
        return map
    }
    
    fun getUsageByLanguage(): Map<String, Int> {
        val json = prefs.getString("language_usage", "{}") ?: "{}"
        return try {
            gson.fromJson(json, Map::class.java) as? Map<String, Int> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

@Singleton
class FeatureFlags @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)
    
    fun isEnabled(feature: String): Boolean {
        return prefs.getBoolean(feature, true)
    }
    
    fun setEnabled(feature: String, enabled: Boolean) {
        prefs.edit().putBoolean(feature, enabled).apply()
    }
    
    companion object {
        const val FEATURE_ONBOARDING = "onboarding_enabled"
        const val FEATURE_WIDGET = "widget_enabled"
        const val FEATURE_GAMIFICATION = "gamification_enabled"
        const val FEATURE_ANALYTICS = "analytics_enabled"
        const val FEATURE_CLOUD_SYNC = "cloud_sync_enabled"
        const val FEATURE_BIOMETRIC = "biometric_enabled"
        const val FEATURE_LIVE_CAMERA = "live_camera_enabled"
        const val FEATURE_VOICE_CLONING = "voice_cloning_enabled"
    }
}

@Singleton
class AppConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    
    var appVersion: String
        get() = prefs.getString("app_version", "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit().putString("app_version", value).apply()
    
    var lastUpdateCheck: Long
        get() = prefs.getLong("last_update_check", 0)
        set(value) = prefs.edit().putLong("last_update_check", value).apply()
    
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()
    
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()
    
    var lastLanguageUpdate: Long
        get() = prefs.getLong("last_language_update", 0)
        set(value) = prefs.edit().putLong("last_language_update", value).apply()
    
    fun getMinRequiredVersion(): String = "1.0.0"
    
    fun shouldShowUpdateDialog(): Boolean {
        // Would compare versions and show update dialog
        return false
    }
    
    fun isVersionObsolete(): Boolean {
        val current = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val required = getMinRequiredVersion().split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in current.indices) {
            if (current[i] < required[i]) return true
            if (current[i] > required[i]) return false
        }
        return false
    }
}

@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
    
    private val prefs = context.getSharedPreferences("logs", Context.MODE_PRIVATE)
    private val maxLogSize = 1000
    
    fun log(level: LogLevel, tag: String, message: String) {
        if (!isLoggingEnabled()) return
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] ${level.name} [$tag]: $message"
        
        val currentLogs = getLogs().toMutableList()
        currentLogs.add(logEntry)
        
        if (currentLogs.size > maxLogSize) {
            currentLogs.removeAt(0)
        }
        
        prefs.edit().putString("log_data", currentLogs.joinToString("\n")).apply()
    }
    
    fun getLogs(): List<String> {
        val data = prefs.getString("log_data", "") ?: ""
        return if (data.isEmpty()) emptyList() else data.split("\n")
    }
    
    fun clearLogs() {
        prefs.edit().remove("log_data").apply()
    }
    
    private fun isLoggingEnabled(): Boolean {
        return context.packageManager.getApplicationInfo(context.packageName, 0)
            .let { it.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    }
    
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
}