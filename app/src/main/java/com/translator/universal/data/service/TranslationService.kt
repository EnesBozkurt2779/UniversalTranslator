package com.translator.universal.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.local.RemoteLanguageModelDownloader
import com.translator.universal.data.model.Language
import com.translator.universal.data.model.NetworkStatus
import com.translator.universal.data.model.TranslationResult
import com.translator.universal.data.model.TranslationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val translators = mutableMapOf<String, Translator>()
    private var lastSourceLang = ""
    private var lastTargetLang = ""

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            Language("auto", "Otomatik", "Auto"),
            Language("tr", "Türkçe", "Türkçe"),
            Language("en", "İngilizce", "English"),
            Language("de", "Almanca", "Deutsch"),
            Language("fr", "Fransızca", "Français"),
            Language("es", "İspanyolca", "Español"),
            Language("it", "İtalyanca", "Italiano"),
            Language("ru", "Rusça", "Русский"),
            Language("ar", "Arapça", "العربية"),
            Language("zh", "Çince", "中文"),
            Language("ja", "Japonca", "日本語"),
            Language("ko", "Korece", "한국어"),
            Language("pt", "Portekizce", "Português"),
            Language("nl", "Felemenkçe", "Nederlands"),
            Language("pl", "Lehçe", "Polski"),
            Language("hi", "Hintçe", "हिन्दी"),
            Language("th", "Tayca", "ไทย"),
            Language("vi", "Vietnamca", "Tiếng Việt"),
            Language("id", "Endonezya", "Bahasa Indonesia"),
            Language("ms", "Malezya", "Bahasa Melayu"),
            Language("uk", "Ukrayna", "Українська"),
            Language("el", "Yunanca", "Ελληνικά"),
            Language("sv", "İsveççe", "Svenska"),
            Language("da", "Danca", "Dansk"),
            Language("fi", "Fince", "Suomi"),
            Language("no", "Norveççe", "Norsk"),
            Language("cs", "Çekçe", "Čeština"),
            Language("ro", "Romence", "Română"),
            Language("hu", "Macarca", "Magyar"),
            Language("he", "İbranice", "עברית")
        )

        fun mapLanguageCode(code: String): String {
            return when (code) {
                "auto" -> TranslateLanguage.AUTO
                "tr" -> TranslateLanguage.TURKISH
                "en" -> TranslateLanguage.ENGLISH
                "de" -> TranslateLanguage.GERMAN
                "fr" -> TranslateLanguage.FRENCH
                "es" -> TranslateLanguage.SPANISH
                "it" -> TranslateLanguage.ITALIAN
                "ru" -> TranslateLanguage.RUSSIAN
                "ar" -> TranslateLanguage.ARABIC
                "zh" -> TranslateLanguage.CHINESE
                "ja" -> TranslateLanguage.JAPANESE
                "ko" -> TranslateLanguage.KOREAN
                "pt" -> TranslateLanguage.PORTUGUESE
                "nl" -> TranslateLanguage.DUTCH
                "pl" -> TranslateLanguage.POLISH
                "hi" -> TranslateLanguage.HINDI
                "th" -> TranslateLanguage.THAI
                "vi" -> TranslateLanguage.VIETNAMESE
                "id" -> TranslateLanguage.INDONESIAN
                "ms" -> TranslateLanguage.MALAY
                "uk" -> TranslateLanguage.UKRAINIAN
                "el" -> TranslateLanguage.GREEK
                "sv" -> TranslateLanguage.SWEDISH
                "da" -> TranslateLanguage.DANISH
                "fi" -> TranslateLanguage.FINNISH
                "no" -> TranslateLanguage.NORWEGIAN
                "cs" -> TranslateLanguage.CZECH
                "ro" -> TranslateLanguage.ROMANIAN
                "hu" -> TranslateLanguage.HUNGARIAN
                "he" -> TranslateLanguage.HEBREW
                else -> code
            }
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getNetworkStatus(): NetworkStatus {
        return if (isOnline()) NetworkStatus.ONLINE else NetworkStatus.OFFLINE
    }

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "$sourceLang-$targetLang"
        
        if (sourceLang == lastSourceLang && targetLang == lastTargetLang && translators.containsKey(key)) {
            return translators[key]!!
        }
        
        translators.values.forEach { it.close() }
        translators.clear()
        
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mapLanguageCode(sourceLang))
            .setTargetLanguage(mapLanguageCode(targetLang))
            .build()
        
        val translator = Translation.getClient(options)
        lastSourceLang = sourceLang
        lastTargetLang = targetLang
        translators[key] = translator
        return translator
    }

    fun translate(text: String, sourceLang: String, targetLang: String): Flow<TranslationState> = flow {
        emit(TranslationState.Loading)
        
        try {
            val translator = getTranslator(sourceLang, targetLang)
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            
            if (!isOnline()) {
                translator.downloadModelIfNeeded(conditions).await()
            }
            
            val result = translator.translate(text).await()
            val isOffline = !isOnline()
            
            emit(TranslationState.Success(
                TranslationResult(
                    sourceText = text,
                    translatedText = result,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    isOffline = isOffline
                )
            ))
        } catch (e: Exception) {
            emit(TranslationState.Error(e.message ?: "Çeviri hatası"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadLanguageModel(languageCode: String): Flow<TranslationState> = flow {
        emit(TranslationState.Downloading(0, languageCode))
        
        try {
            val model = TranslateRemoteModel.Builder(mapLanguageCode(languageCode)).build()
            val downloader = RemoteLanguageModelDownloader.getInstance()
            
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            
            downloader.download(model, conditions)
                .addOnSuccessListener {
                    // Downloaded
                }
                .addOnFailureListener { e ->
                    // Handle failure
                }
                .await()
            
            emit(TranslationState.Success(
                TranslationResult("", "", languageCode, "", true)
            ))
        } catch (e: Exception) {
            emit(TranslationState.Error(e.message ?: "İndirme hatası"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun isLanguageDownloaded(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(mapLanguageCode(languageCode)).build()
            val modelManager = Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TURKISH)
                .build()).modelManager
            
            modelManager.isModelDownloaded(model).await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteLanguageModel(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(mapLanguageCode(languageCode)).build()
            val modelManager = Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TURKISH)
                .build()).modelManager
            
            modelManager.deleteDownloadedModel(model).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun closeTranslators() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}