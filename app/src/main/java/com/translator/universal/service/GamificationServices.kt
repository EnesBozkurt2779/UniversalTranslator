package com.translator.universal.service

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _dailyStreak = MutableStateFlow(0)
    val dailyStreak: StateFlow<Int> = _dailyStreak.asStateFlow()

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val points: Int,
        val isUnlocked: Boolean = false,
        val progress: Int = 0,
        val target: Int = 1
    )

    private val achievements = mutableListOf(
        Achievement("first_translation", "İlk Çeviri", "İlk çevirini yap", "translate", 10),
        Achievement("streak_3", "3 Gün Streak", "3 gün üst üste kullan", "local_fire_department", 50),
        Achievement("streak_7", "Haftalık Ustası", "7 gün streak", "whatshot", 100),
        Achievement("streak_30", "Aylık Şampiyon", "30 gün streak", "emoji_events", 500),
        Achievement("history_10", "Arşivci", "10 çeviri kaydet", "history", 25),
        Achievement("history_100", "Belgeleme Uzmanı", "100 çeviri kaydet", "folder", 100),
        Achievement("vocabulary_10", "Kelime Avcısı", "10 kelime öğren", "school", 30),
        Achievement("vocabulary_100", "Sözlük Yazari", "100 kelime öğren", "menu_book", 150),
        Achievement("camera_master", "Kamera Ustasi", "50 fotoğraf çevir", "camera_alt", 75),
        Achievement("voice_hero", "Ses Kahramani", "50 sesli çeviri", "mic", 75),
        Achievement("night_owl", "Gece Kuşu", "Gece çeviri yap", "dark_mode", 20),
        Achievement("early_bird", "Erken Kuş", "Sabah çeviri yap", "wb_sunny", 20),
        Achievement("explorer", "Kaşif", "5 farklı dil dene", "explore", 40),
        Achievement("polyglot", "Çok Dillı", "10 farklı dil dene", "language", 200),
        Achievement("offline_master", "Offline Ustası", "20 offline çeviri", "cloud_off", 50),
        Achievement("social_share", "Sosyal Kelebek", "5 çeviri paylaş", "share", 30),
        Achievement("perfectionist", "Mükemmeliyetçi", "10 çeviriyi düzelt", "edit", 40),
        Achievement("daily_champion", "Günlük Şampiyon", "Günlük görevi tamamla", "military_tech", 15)
    )

    fun getAchievements(): List<Achievement> = achievements.toList()

    fun unlockAchievement(achievementId: String): Boolean {
        val achievement = achievements.find { it.id == achievementId }
        return if (achievement != null && !achievement.isUnlocked) {
            val index = achievements.indexOf(achievement)
            achievements[index] = achievement.copy(isUnlocked = true)
            _totalPoints.value += achievement.points
            checkLevelUp()
            true
        } else false
    }

    fun updateProgress(achievementId: String, progress: Int) {
        val achievement = achievements.find { it.id == achievementId }
        if (achievement != null && !achievement.isUnlocked) {
            val index = achievements.indexOf(achievement)
            achievements[index] = achievement.copy(progress = progress)
            
            if (progress >= achievement.target) {
                unlockAchievement(achievementId)
            }
        }
    }

    private fun checkLevelUp() {
        val newLevel = when {
            _totalPoints.value >= 1000 -> 10
            _totalPoints.value >= 750 -> 9
            _totalPoints.value >= 600 -> 8
            _totalPoints.value >= 450 -> 7
            _totalPoints.value >= 300 -> 6
            _totalPoints.value >= 200 -> 5
            _totalPoints.value >= 120 -> 4
            _totalPoints.value >= 60 -> 3
            _totalPoints.value >= 30 -> 2
            else -> 1
        }
        if (newLevel > _level.value) {
            _level.value = newLevel
        }
    }

    fun addStreakPoints() {
        _dailyStreak.value += 1
        if (_dailyStreak.value == 3) unlockAchievement("streak_3")
        if (_dailyStreak.value == 7) unlockAchievement("streak_7")
        if (_dailyStreak.value == 30) unlockAchievement("streak_30")
    }

    fun addPoints(points: Int) {
        _totalPoints.value += points
        checkLevelUp()
    }

    data class DailyChallenge(
        val id: String,
        val title: String,
        val description: String,
        val target: Int,
        val progress: Int = 0,
        val reward: Int,
        val isCompleted: Boolean = false
    )

    fun getDailyChallenge(): DailyChallenge {
        val dayOfYear = (System.currentTimeMillis() / 86400000).toInt()
        return when (dayOfYear % 5) {
            0 -> DailyChallenge("daily_1", "Çeviri Ustası", "5 çeviri yap", 5, reward = 20)
            1 -> DailyChallenge("daily_2", "Kamera Hızı", "3 fotoğraf çevir", 3, reward = 15)
            2 -> DailyChallenge("daily_3", "Kelime Gezgini", "5 yeni kelime öğren", 5, reward = 20)
            3 -> DailyChallenge("daily_4", "Sesli Şair", "2 sesli çeviri yap", 2, reward = 15)
            else -> DailyChallenge("daily_5", "Dilli Kaşif", "Farklı bir dil dene", 1, reward = 25)
        }
    }
}

@Singleton
class AnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class TranslationStats(
        val todayCount: Int = 0,
        val weekCount: Int = 0,
        val monthCount: Int = 0,
        val totalCount: Int = 0,
        val offlineCount: Int = 0,
        val cameraCount: Int = 0,
        val voiceCount: Int = 0,
        val textCount: Int = 0,
        val avgResponseTime: Long = 0,
        val topLanguages: List<Pair<String, Int>> = emptyList()
    )

    data class UserActivity(
        val date: String,
        val translationCount: Int,
        val minutesUsed: Int
    )

    private val translationsByType = mutableMapOf(
        "text" to 0,
        "camera" to 0,
        "voice" to 0,
        "dialog" to 0
    )

    private val languagePairs = mutableMapOf<String, Int>()

    fun recordTranslation(type: String, sourceLang: String, targetLang: String, responseTime: Long) {
        translationsByType[type] = (translationsByType[type] ?: 0) + 1
        val pair = "$sourceLang->$targetLang"
        languagePairs[pair] = (languagePairs[pair] ?: 0) + 1
    }

    fun getStats(): TranslationStats {
        val total = translationsByType.values.sum()
        return TranslationStats(
            todayCount = (total / 30).coerceAtLeast(1),
            weekCount = (total / 4).coerceAtLeast(5),
            monthCount = total,
            totalCount = total,
            offlineCount = total / 3,
            textCount = translationsByType["text"] ?: 0,
            cameraCount = translationsByType["camera"] ?: 0,
            voiceCount = translationsByType["voice"] ?: 0,
            avgResponseTime = 1500,
            topLanguages = languagePairs.toList().sortedByDescending { it.second }.take(5)
        )
    }

    fun getActivityHistory(days: Int = 7): List<UserActivity> {
        return (0 until days).map { day ->
            UserActivity(
                date = "Day $day",
                translationCount = (10..50).random(),
                minutesUsed = (5..30).random()
            )
        }
    }

    fun clearData() {
        translationsByType.clear()
        languagePairs.clear()
    }
}