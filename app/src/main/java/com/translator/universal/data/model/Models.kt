package com.translator.universal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_corrections")
data class UserCorrection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val suggestedTranslation: String,
    val userCorrection: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "analytics")
data class TranslationAnalytics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLanguage: String,
    val targetLanguage: String,
    val translationType: String, // text, camera, voice, dialog
    val isOffline: Boolean,
    val responseTime: Long, // milliseconds
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_access")
data class QuickAccessItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_progress")
data class LearningProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val languageCode: String,
    val wordsLearned: Int = 0,
    val wordsToLearn: Int = 0,
    val accuracy: Float = 0f,
    val streakDays: Int = 0,
    val lastActivityAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val targetProgress: Int = 1
)

// API Response Models
data class TranslationAlternative(
    val text: String,
    val confidence: Float,
    val style: TranslationStyle,
    val region: String? = null
)

enum class TranslationStyle {
    FORMAL,
    INFORMAL,
    LITERAL,
    ADAPTIVE,
    TECHNICAL,
    CREATIVE
}

data class DictionaryResult(
    val word: String,
    val phonetic: String?,
    val definitions: List<Definition>,
    val examples: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>
)

data class Definition(
    val partOfSpeech: String,
    val meaning: String,
    val examples: List<String>
)

data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String,
    val isSupported: Boolean,
    val modelSize: Long?,
    val isDownloaded: Boolean,
    val downloadProgress: Int = 0
)

data class TranslationStats(
    val totalTranslations: Int,
    val todayTranslations: Int,
    val favoriteLanguage: String,
    val averageResponseTime: Long,
    val offlineUsage: Int,
    val streakDays: Int
)

data class UserPreferences(
    val defaultSourceLanguage: String = "auto",
    val defaultTargetLanguage: String = "tr",
    val autoDetectLanguage: Boolean = true,
    val themeMode: Int = 3, // 0: light, 1: dark, 2: amoled, 3: system
    val hapticFeedback: Boolean = true,
    val soundEffects: Boolean = false,
    val autoSaveHistory: Boolean = true,
    val offlineByDefault: Boolean = false,
    val showAlternatives: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val preferredEngine: String = "mlkit"
)