package com.translator.universal.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "translation_history")
data class TranslationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isOffline: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_translations")
data class FavoriteTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "word_vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val translation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val example: String? = null,
    val pronunciation: String? = null,
    val isLearned: Boolean = false,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_models")
data class DownloadedModelEntity(
    @PrimaryKey
    val languageCode: String,
    val languageName: String,
    val modelSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val isUpdated: Boolean = true
)

@Dao
interface TranslationHistoryDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<TranslationHistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<TranslationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TranslationHistoryEntity): Long

    @Delete
    suspend fun deleteHistory(history: TranslationHistoryEntity)

    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM translation_history")
    fun getHistoryCount(): Flow<Int>
}

@Dao
interface FavoriteTranslationDao {
    @Query("SELECT * FROM favorite_translations ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteTranslationEntity>>

    @Query("SELECT * FROM favorite_translations WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchFavorites(query: String): Flow<List<FavoriteTranslationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteTranslationEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteTranslationEntity)

    @Query("DELETE FROM favorite_translations")
    suspend fun clearAllFavorites()

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_translations WHERE sourceText = :sourceText AND targetLanguage = :targetLanguage LIMIT 1)")
    suspend fun isFavorite(sourceText: String, targetLanguage: String): Boolean
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM word_vocabulary ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM word_vocabulary WHERE isLearned = 0 ORDER BY createdAt DESC")
    fun getUnlearnedWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM word_vocabulary WHERE isLearned = 1 ORDER BY lastReviewedAt DESC")
    fun getLearnedWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM word_vocabulary WHERE sourceLanguage = :sourceLang AND targetLanguage = :targetLang ORDER BY createdAt DESC")
    fun getWordsByLanguages(sourceLang: String, targetLang: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM word_vocabulary WHERE word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchWords(query: String): Flow<List<VocabularyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabularyEntity): Long

    @Update
    suspend fun updateWord(word: VocabularyEntity)

    @Delete
    suspend fun deleteWord(word: VocabularyEntity)

    @Query("DELETE FROM word_vocabulary")
    suspend fun clearAllWords()

    @Query("SELECT COUNT(*) FROM word_vocabulary")
    fun getWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_vocabulary WHERE isLearned = 1")
    fun getLearnedWordCount(): Flow<Int>

    @Query("UPDATE word_vocabulary SET reviewCount = reviewCount + 1, lastReviewedAt = :timestamp WHERE id = :wordId")
    suspend fun incrementReviewCount(wordId: Long, timestamp: Long)

    @Query("UPDATE word_vocabulary SET isLearned = :isLearned WHERE id = :wordId")
    suspend fun setLearned(wordId: Long, isLearned: Boolean)
}

@Dao
interface DownloadedModelDao {
    @Query("SELECT * FROM downloaded_models ORDER BY languageName ASC")
    fun getAllDownloadedModels(): Flow<List<DownloadedModelEntity>>

    @Query("SELECT * FROM downloaded_models WHERE languageCode = :languageCode")
    suspend fun getModel(languageCode: String): DownloadedModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: DownloadedModelEntity)

    @Delete
    suspend fun deleteModel(model: DownloadedModelEntity)

    @Query("DELETE FROM downloaded_models")
    suspend fun clearAllModels()

    @Query("SELECT COUNT(*) FROM downloaded_models")
    fun getDownloadedCount(): Flow<Int>

    @Query("SELECT SUM(modelSize) FROM downloaded_models")
    fun getTotalSize(): Flow<Long?>
}

@Database(
    entities = [
        TranslationHistoryEntity::class,
        FavoriteTranslationEntity::class,
        VocabularyEntity::class,
        DownloadedModelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TranslatorDatabase : RoomDatabase() {
    abstract fun translationHistoryDao(): TranslationHistoryDao
    abstract fun favoriteTranslationDao(): FavoriteTranslationDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun downloadedModelDao(): DownloadedModelDao
}