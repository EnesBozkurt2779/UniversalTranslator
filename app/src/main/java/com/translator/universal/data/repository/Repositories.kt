package com.translator.universal.data.repository

import com.translator.universal.data.local.*
import com.translator.universal.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationHistoryRepository @Inject constructor(
    private val historyDao: TranslationHistoryDao
) {
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>> = historyDao.getAllHistory()
    
    fun getRecentHistory(limit: Int = 50): Flow<List<TranslationHistoryEntity>> = 
        historyDao.getRecentHistory(limit)
    
    fun searchHistory(query: String): Flow<List<TranslationHistoryEntity>> = 
        historyDao.searchHistory(query)
    
    suspend fun addToHistory(
        sourceText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        isOffline: Boolean
    ): Long {
        return historyDao.insertHistory(
            TranslationHistoryEntity(
                sourceText = sourceText,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isOffline = isOffline
            )
        )
    }
    
    suspend fun deleteHistory(history: TranslationHistoryEntity) {
        historyDao.deleteHistory(history)
    }
    
    suspend fun clearAllHistory() {
        historyDao.clearAllHistory()
    }
    
    fun getHistoryCount(): Flow<Int> = historyDao.getHistoryCount()
}

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteTranslationDao
) {
    fun getAllFavorites(): Flow<List<FavoriteTranslationEntity>> = favoriteDao.getAllFavorites()
    
    fun searchFavorites(query: String): Flow<List<FavoriteTranslationEntity>> = 
        favoriteDao.searchFavorites(query)
    
    suspend fun addFavorite(
        sourceText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Long {
        return favoriteDao.insertFavorite(
            FavoriteTranslationEntity(
                sourceText = sourceText,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
        )
    }
    
    suspend fun removeFavorite(favorite: FavoriteTranslationEntity) {
        favoriteDao.deleteFavorite(favorite)
    }
    
    suspend fun clearAllFavorites() {
        favoriteDao.clearAllFavorites()
    }
    
    suspend fun isFavorite(sourceText: String, targetLanguage: String): Boolean {
        return favoriteDao.isFavorite(sourceText, targetLanguage)
    }
}

@Singleton
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao
) {
    fun getAllWords(): Flow<List<VocabularyEntity>> = vocabularyDao.getAllWords()
    
    fun getUnlearnedWords(): Flow<List<VocabularyEntity>> = vocabularyDao.getUnlearnedWords()
    
    fun getLearnedWords(): Flow<List<VocabularyEntity>> = vocabularyDao.getLearnedWords()
    
    fun getWordsByLanguages(sourceLang: String, targetLang: String): Flow<List<VocabularyEntity>> =
        vocabularyDao.getWordsByLanguages(sourceLang, targetLang)
    
    fun searchWords(query: String): Flow<List<VocabularyEntity>> = 
        vocabularyDao.searchWords(query)
    
    suspend fun addWord(
        word: String,
        translation: String,
        sourceLanguage: String,
        targetLanguage: String,
        example: String? = null,
        pronunciation: String? = null
    ): Long {
        return vocabularyDao.insertWord(
            VocabularyEntity(
                word = word,
                translation = translation,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                example = example,
                pronunciation = pronunciation
            )
        )
    }
    
    suspend fun updateWord(word: VocabularyEntity) {
        vocabularyDao.updateWord(word)
    }
    
    suspend fun deleteWord(word: VocabularyEntity) {
        vocabularyDao.deleteWord(word)
    }
    
    suspend fun clearAllWords() {
        vocabularyDao.clearAllWords()
    }
    
    fun getWordCount(): Flow<Int> = vocabularyDao.getWordCount()
    
    fun getLearnedWordCount(): Flow<Int> = vocabularyDao.getLearnedWordCount()
    
    suspend fun markAsLearned(wordId: Long) {
        vocabularyDao.setLearned(wordId, true)
    }
    
    suspend fun markAsUnlearned(wordId: Long) {
        vocabularyDao.setLearned(wordId, false)
    }
    
    suspend fun incrementReview(wordId: Long) {
        vocabularyDao.incrementReviewCount(wordId, System.currentTimeMillis())
    }
}

@Singleton
class DownloadedModelsRepository @Inject constructor(
    private val downloadedModelDao: DownloadedModelDao
) {
    fun getAllDownloadedModels(): Flow<List<DownloadedModelEntity>> = 
        downloadedModelDao.getAllDownloadedModels()
    
    suspend fun getModel(languageCode: String): DownloadedModelEntity? =
        downloadedModelDao.getModel(languageCode)
    
    suspend fun saveModel(
        languageCode: String,
        languageName: String,
        modelSize: Long
    ) {
        downloadedModelDao.insertModel(
            DownloadedModelEntity(
                languageCode = languageCode,
                languageName = languageName,
                modelSize = modelSize
            )
        )
    }
    
    suspend fun removeModel(model: DownloadedModelEntity) {
        downloadedModelDao.deleteModel(model)
    }
    
    suspend fun clearAllModels() {
        downloadedModelDao.clearAllModels()
    }
    
    fun getDownloadedCount(): Flow<Int> = downloadedModelDao.getDownloadedCount()
    
    fun getTotalSize(): Flow<Long?> = downloadedModelDao.getTotalSize()
}