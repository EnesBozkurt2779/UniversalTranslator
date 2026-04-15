package com.translator.universal.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.translator.universal.data.local.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: TranslationHistoryDao,
    private val favoriteDao: FavoriteTranslationDao,
    private val vocabularyDao: VocabularyDao,
    private val modelDao: DownloadedModelDao
) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    data class BackupData(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val history: List<TranslationHistoryEntity> = emptyList(),
        val favorites: List<FavoriteTranslationEntity> = emptyList(),
        val vocabulary: List<VocabularyEntity> = emptyList()
    )

    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val backup = BackupData(
            history = historyDao.getAllHistory().first(),
            favorites = favoriteDao.getAllFavorites().first(),
            vocabulary = vocabularyDao.getAllWords().first()
        )

        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, "backup_${dateFormat.format(Date())}.json")
        backupFile.writeText(gson.toJson(backup))
        backupFile
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val json = inputStream.bufferedReader().use { it.readText() }
            val backup = gson.fromJson(json, BackupData::class.java)

            // Clear existing data
            historyDao.clearAllHistory()
            favoriteDao.clearAllFavorites()
            vocabularyDao.clearAllWords()

            // Restore data
            backup.history.forEach { historyDao.insertHistory(it) }
            backup.favorites.forEach { favoriteDao.insertFavorite(it) }
            backup.vocabulary.forEach { vocabularyDao.insertWord(it) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportToCsv(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val history = historyDao.getAllHistory().first()
            val csv = buildString {
                appendLine("Source,Translation,Source Language,Target Language,Timestamp")
                history.forEach { item ->
                    appendLine("\"${item.sourceText.replace("\"", "\"\"")}\",\"${item.translatedText.replace("\"", "\"\"")}\",${item.sourceLanguage},${item.targetLanguage},${item.timestamp}")
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csv.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportToJson(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val history = historyDao.getAllHistory().first()
            val json = gson.toJson(history)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getBackupList(): List<File> = withContext(Dispatchers.IO) {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (backupDir.exists()) {
            backupDir.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun deleteBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        file.delete()
    }
}

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir = context.cacheDir
    private val filesDir = context.filesDir
    private val externalFilesDir = context.getExternalFilesDir(null)

    fun getCacheSize(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun getModelSize(): Long {
        val modelDir = File(filesDir, "ml_models")
        return if (modelDir.exists()) {
            modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0
    }

    fun getTotalAppSize(): Long {
        return getCacheSize() + getModelSize() + getDatabaseSize()
    }

    fun getDatabaseSize(): Long {
        val dbDir = context.getDatabasePath("translator_database").parentFile
        return if (dbDir != null && dbDir.exists()) {
            dbDir.walkTopDown().filter { it.isFile && it.extension == "sqlite" }.sumOf { it.length() }
        } else 0
    }

    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    fun clearModels() {
        val modelDir = File(filesDir, "ml_models")
        modelDir.deleteRecursively()
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getAvailableSpace(): Long {
        return externalFilesDir?.freeSpace ?: 0
    }
}