package com.translator.universal.service

import com.translator.universal.data.local.TranslationHistoryDao
import com.translator.universal.data.local.VocabularyDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationService @Inject constructor(
    private val historyDao: TranslationHistoryDao,
    private val vocabularyDao: VocabularyDao
) {
    private val achievements = mutableListOf<Achievement>()
    private var currentStreak = 0
    private var totalPoints = 0L
    
    init {
        initializeAchievements()
    }
    
    private fun initializeAchievements() {
        achievements.addAll(listOf(
            Achievement("first_translation", "İlk Çeviri", "İlk çevirini yap", "ic_translate", 1, 1),
            Achievement("ten_translations", "Başlangıç", "10 çeviri yap", "ic_translate", 10, 10),
            Achievement("fifty_translations", "Hızlanıyor", "50 çeviri yap", "ic_translate", 50, 50),
            Achievement("hundred_translations", "Çevirmen", "100 çeviri yap", "ic_translate", 100, 100),
            Achievement("five_hundred_translations", "Uzman Çevirmen", "500 çeviri yap", "ic_translate", 500, 500),
            Achievement("thousand_translations", "Efsane Çevirmen", "1000 çeviri yap", "ic_translate", 1000, 1000),
            
            Achievement("first_offline", "Offline Çevirmen", "Offline çeviri yap", "ic_offline", 1, 1),
            Achievement("ten_offline", "Offline Ustası", "10 offline çeviri yap", "ic_offline", 10, 10),
            
            Achievement("first_camera", "Fotoğrafçı", "Kamera ile çevir", "ic_camera", 1, 1),
            Achievement("ten_camera", "Gözlemci", "10 fotoğraf çevirisi", "ic_camera", 10, 10),
            
            Achievement("first_voice", "Sesli Çeviri", "Sesli çeviri yap", "ic_mic", 1, 1),
            Achievement("ten_voice", "Konuşmacı", "10 sesli çeviri", "ic_mic", 10, 10),
            
            Achievement("first_vocabulary", "Kelime Avcısı", "İlk kelime ekle", "ic_vocabulary", 1, 1),
            Achievement("ten_vocabulary", "Sözlük", "10 kelime ekle", "ic_vocabulary", 10, 10),
            Achievement("fifty_vocabulary", "Kelime Ustası", "50 kelime ekle", "ic_vocabulary", 50, 50),
            Achievement("hundred_vocabulary", "Söz ustası", "100 kelime ekle", "ic_vocabulary", 100, 100),
            
            Achievement("first_favorite", "Favori", "İlk favoriyi kaydet", "ic_favorite", 1, 1),
            Achievement("ten_favorites", "Koleksiyoncu", "10 favori ekle", "ic_favorite", 10, 10),
            
            Achievement("first_learned", "Öğrenci", "İlk kelimeyi öğren", "ic_check", 1, 1),
            Achievement("ten_learned", "Dil Öğrencisi", "10 kelime öğren", "ic_check", 10, 10),
            Achievement("fifty_learned", "Dil Uzmanı", "50 kelime öğren", "ic_check", 50, 50),
            
            Achievement("streak_3", "Üç Günlük", "3 gün üst üste kullan", "ic_streak", 3, 3),
            Achievement("streak_7", "Haftalık", "7 gün üst üste kullan", "ic_streak", 7, 7),
            Achievement("streak_30", "Aylık", "30 gün üst üste kullan", "ic_streak", 30, 30),
            Achievement("streak_100", "Yüz Gün", "100 gün üst üste kullan", "ic_streak", 100, 100),
            
            Achievement("all_languages", "Dünya Vatandaşı", "Tüm dilleri dene", "ic_languages", 0, 30),
            
            Achievement("night_owl", "Gece Kuşu", "Gece çeviri yap", "ic_moon", 1, 1),
            Achievement("early_bird", "Erken Kuş", "Sabah çeviri yap", "ic_sun", 1, 1),
            
            Achievement("share_app", "Paylaşımcı", "Uygulamayı paylaş", "ic_share", 1, 1),
            Achievement("rate_app", "Değerlendirmeci", "Uygulamayı değerlendir", "ic_star", 1, 1)
        ))
    }
    
    suspend fun checkAndUpdateAchievements(): List<Achievement> = withContext(Dispatchers.IO) {
        val history = historyDao.getHistoryCount().first()
        val vocabularyCount = vocabularyDao.getWordCount().first()
        val learnedCount = vocabularyDao.getLearnedWordCount().first()
        
        val unlockedAchievements = mutableListOf<Achievement>()
        
        achievements.forEach { achievement ->
            val progress = when (achievement.id) {
                "first_translation", "ten_translations", "fifty_translations", 
                "hundred_translations", "five_hundred_translations", "thousand_translations" -> history
                
                "first_vocabulary", "ten_vocabulary", "fifty_vocabulary", "hundred_vocabulary" -> vocabularyCount
                
                "first_learned", "ten_learned", "fifty_learned" -> learnedCount
                
                else -> 0
            }
            
            if (!achievement.isUnlocked && progress >= achievement.targetProgress) {
                achievement.isUnlocked = true
                achievement.unlockedAt = System.currentTimeMillis()
                totalPoints += 100
                unlockedAchievements.add(achievement)
            }
            
            achievement.progress = minOf(progress, achievement.targetProgress)
        }
        
        unlockedAchievements
    }
    
    fun getAllAchievements() = achievements.toList()
    
    fun getUnlockedCount() = achievements.count { it.isUnlocked }
    
    fun getTotalAchievementCount() = achievements.size
    
    fun getPoints() = totalPoints
    
    fun getStreak() = currentStreak
    
    fun updateStreak(days: Int) {
        currentStreak = days
    }
    
    suspend fun calculateLevel(): Int = withContext(Dispatchers.IO) {
        val historyCount = historyDao.getHistoryCount().first()
        val vocabularyCount = vocabularyDao.getWordCount().first()
        val learnedCount = vocabularyDao.getLearnedWordCount().first()
        
        // Points calculation
        val points = (historyCount * 1) + (vocabularyCount * 2) + (learnedCount * 5) + totalPoints
        
        // Level calculation (every 1000 points = 1 level)
        (points / 1000) + 1
    }
    
    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val iconName: String,
        var progress: Int = 0,
        val targetProgress: Int,
        var isUnlocked: Boolean = false,
        var unlockedAt: Long? = null
    )
}

@Singleton
class ChallengeService @Inject constructor() {
    
    private var todayChallenge: DailyChallenge? = null
    
    fun getTodayChallenge(): DailyChallenge {
        todayChallenge?.let { return it }
        
        val challenge = when ((System.currentTimeMillis() / 86400000).toInt() % 7) {
            0 -> DailyChallenge(
                id = "translate_10",
                title = "Günlük Çeviri",
                description = "Bugün 10 çeviri yap",
                target = 10,
                type = ChallengeType.TRANSLATION_COUNT,
                reward = 50
            )
            1 -> DailyChallenge(
                id = "camera_5",
                title = "Fotoğrafçı",
                description = "5 fotoğraf çevirisi yap",
                target = 5,
                type = ChallengeType.CAMERA_COUNT,
                reward = 50
            )
            2 -> DailyChallenge(
                id = "vocabulary_5",
                title = "Kelime Öğren",
                description = "5 yeni kelime ekle",
                target = 5,
                type = ChallengeType.VOCABULARY_COUNT,
                reward = 50
            )
            3 -> DailyChallenge(
                id = "voice_3",
                title = "Sesli Çeviri",
                description = "3 sesli çeviri yap",
                target = 3,
                type = ChallengeType.VOICE_COUNT,
                reward = 50
            )
            4 -> DailyChallenge(
                id = "offline_2",
                title = "Offline Çevirmen",
                description = "2 offline çeviri yap",
                target = 2,
                type = ChallengeType.OFFLINE_COUNT,
                reward = 75
            )
            5 -> DailyChallenge(
                id = "favorite_3",
                title = "Koleksiyoncu",
                description = "3 çeviriyi favorilere ekle",
                target = 3,
                type = ChallengeType.FAVORITE_COUNT,
                reward = 50
            )
            else -> DailyChallenge(
                id = "learn_5",
                title = "Öğrenme Zamanı",
                description = "5 kelimeyi öğrenilmiş olarak işaretle",
                target = 5,
                type = ChallengeType.LEARNED_COUNT,
                reward = 75
            )
        }
        
        todayChallenge = challenge
        return challenge
    }
    
    fun completeChallenge() {
        todayChallenge = null
    }
    
    data class DailyChallenge(
        val id: String,
        val title: String,
        val description: String,
        val target: Int,
        val type: ChallengeType,
        val reward: Int
    )
    
    enum class ChallengeType {
        TRANSLATION_COUNT, CAMERA_COUNT, VOICE_COUNT, VOCABULARY_COUNT,
        OFFLINE_COUNT, FAVORITE_COUNT, LEARNED_COUNT
    }
}