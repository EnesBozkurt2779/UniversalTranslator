package com.translator.universal.data.cache

import android.content.Context
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val memoryCache = LruCache<String, CachedTranslation>(100)
    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "translation_cache")
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    data class CachedTranslation(
        val translatedText: String,
        val timestamp: Long,
        val isOffline: Boolean
    )

    private fun generateKey(sourceText: String, sourceLang: String, targetLang: String): String {
        return "${sourceLang}_${targetLang}_${sourceText.hashCode()}"
    }

    fun get(sourceText: String, sourceLang: String, targetLang: String): CachedTranslation? {
        val key = generateKey(sourceText, sourceLang, targetLang)
        
        // First check memory cache
        memoryCache.get(key)?.let { cached ->
            // Cache expires after 24 hours
            if (System.currentTimeMillis() - cached.timestamp < 24 * 60 * 60 * 1000) {
                return cached
            } else {
                memoryCache.remove(key)
            }
        }
        
        // Then check disk cache
        val file = File(cacheDir, "${key.hashCode()}.json")
        if (file.exists()) {
            try {
                val json = file.readText()
                val cached = gson.fromJson(json, CachedTranslation::class.java)
                if (System.currentTimeMillis() - cached.timestamp < 24 * 60 * 60 * 1000) {
                    memoryCache.put(key, cached)
                    return cached
                } else {
                    file.delete()
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
        
        return null
    }

    fun put(sourceText: String, sourceLang: String, targetLang: String, translatedText: String, isOffline: Boolean) {
        val key = generateKey(sourceText, sourceLang, targetLang)
        val cached = CachedTranslation(
            translatedText = translatedText,
            timestamp = System.currentTimeMillis(),
            isOffline = isOffline
        )
        
        // Save to memory
        memoryCache.put(key, cached)
        
        // Save to disk
        try {
            val file = File(cacheDir, "${key.hashCode()}.json")
            file.writeText(gson.toJson(cached))
        } catch (e: Exception) {
            // Ignore disk write errors
        }
    }

    fun invalidate(sourceLang: String, targetLang: String) {
        // Clear all cache for specific language pair
        memoryCache.snapshot().keys
            .filter { it.startsWith("${sourceLang}_${targetLang}_") }
            .forEach { memoryCache.remove(it) }
        
        // Clear disk cache
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    fun clearAll() {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    fun getCacheCount(): Int {
        return memoryCache.size()
    }
}

@Singleton
class LanguageModelCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir = File(context.filesDir, "ml_models")
    
    init {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
    }

    fun getModelPath(languageCode: String): File {
        return File(modelDir, "model_$languageCode")
    }

    fun modelExists(languageCode: String): Boolean {
        return getModelPath(languageCode).exists()
    }

    fun getModelSize(languageCode: String): Long {
        val path = getModelPath(languageCode)
        return if (path.exists()) path.length() else 0
    }

    fun deleteModel(languageCode: String): Boolean {
        return getModelPath(languageCode).deleteRecursively()
    }

    fun getTotalSize(): Long {
        return modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearAll() {
        modelDir.deleteRecursively()
        modelDir.mkdirs()
    }
}