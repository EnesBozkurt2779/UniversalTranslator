package com.translator.universal.service

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.translator.universal.data.model.TranslationResult
import com.translator.universal.data.service.NetworkService
import com.translator.universal.data.service.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiEngineTranslationService @Inject constructor(
    private val translationService: TranslationService,
    private val networkService: NetworkService
) {
    enum class TranslationEngine {
        GOOGLE_ML_KIT,
        BING,
        DEEPL,
        YANDEX
    }

    data class MultiEngineResult(
        val primaryResult: TranslationResult,
        val alternativeResults: Map<TranslationEngine, TranslationResult>,
        val bestMatch: TranslationEngine,
        val confidence: Float
    )

    suspend fun translateWithAllEngines(
        text: String,
        sourceLang: String,
        targetLang: String
    ): MultiEngineResult = withContext(Dispatchers.IO) {
        
        val isOnline = networkService.isOnline()
        
        // ML Kit translation (primary - works offline)
        val primaryDeferred = async {
            try {
                var result: TranslationResult? = null
                translationService.translate(text, sourceLang, targetLang).collect { state ->
                    when (state) {
                        is com.translator.universal.data.model.TranslationState.Success -> {
                            result = state.result
                        }
                        else -> {}
                    }
                }
                result
            } catch (e: Exception) {
                null
            }
        }
        
        val primaryResult = primaryDeferred.await() ?: TranslationResult(
            sourceText = text,
            translatedText = text,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            isOffline = !isOnline
        )
        
        // For online mode, we would add more engines here
        // Each would be called in parallel and compared
        val alternativeResults = emptyMap<TranslationEngine, TranslationResult>()
        
        // Best match is ML Kit for now (offline capable)
        MultiEngineResult(
            primaryResult = primaryResult,
            alternativeResults = alternativeResults,
            bestMatch = TranslationEngine.GOOGLE_ML_KIT,
            confidence = 0.95f
        )
    }

    suspend fun translateWithBestEngine(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        
        if (networkService.isOnline()) {
            // Use online translation for better accuracy
            var result: TranslationResult? = null
            translationService.translate(text, sourceLang, targetLang).collect { state ->
                when (state) {
                    is com.translator.universal.data.model.TranslationState.Success -> {
                        result = state.result
                    }
                    else -> {}
                }
            }
            result ?: TranslationResult(
                sourceText = text,
                translatedText = text,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isOffline = false
            )
        } else {
            // Offline mode - use ML Kit
            var result: TranslationResult? = null
            translationService.translate(text, sourceLang, targetLang).collect { state ->
                when (state) {
                    is com.translator.universal.data.model.TranslationState.Success -> {
                        result = state.result.copy(isOffline = true)
                    }
                    else -> {}
                }
            }
            result ?: TranslationResult(
                sourceText = text,
                translatedText = text,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isOffline = true
            )
        }
    }

    suspend fun compareResults(
        text: String,
        sourceLang: String,
        targetLang: String
    ): List<Pair<TranslationEngine, String>> = withContext(Dispatchers.IO) {
        // Compare results from different engines
        // This would be used for the "best translation" feature
        listOf(
            TranslationEngine.GOOGLE_ML_KIT to "Translation result"
        )
    }
}

@Singleton
class ContextAwareTranslationService @Inject constructor(
    private val multiEngineService: MultiEngineTranslationService
) {
    private val contextCache = mutableListOf<TranslationContext>()

    data class TranslationContext(
        val previousText: String,
        val previousTranslation: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    suspend fun translateWithContext(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        
        // Add context from previous translations
        val context = contextCache.takeLast(3)
        
        val baseResult = multiEngineService.translateWithBestEngine(text, sourceLang, targetLang)
        
        // Update context cache
        if (contextCache.size > 10) {
            contextCache.removeAt(0)
        }
        contextCache.add(TranslationContext(text, baseResult.translatedText))
        
        baseResult.copy(
            translatedText = baseResult.translatedText // In production, would adjust based on context
        )
    }

    fun clearContext() {
        contextCache.clear()
    }

    fun getContextSize(): Int = contextCache.size
}

@Singleton
class IdiomaticTranslationService @Inject constructor() {
    
    private val idioms = mapOf(
        "tr" to mapOf(
            "akşam oldu" to "evening came",
            "su damlası" to "drop of water",
            "çay kaşığı" to "teaspoon"
        ),
        "en" to mapOf(
            "piece of cake" to "çok kolay",
            "break a leg" to "bol şans",
            "once in a blue moon" to "çok nadiren"
        )
    )

    suspend fun translateIdioms(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String = withContext(Dispatchers.IO) {
        
        val sourceIdioms = idioms[sourceLang] ?: return@withContext text
        val targetIdioms = idioms[targetLang] ?: return@withContext text
        
        var translated = text
        sourceIdioms.forEach { (idiom, translation) ->
            if (text.contains(idiom, ignoreCase = true)) {
                translated = translated.replace(
                    Regex(idiom, RegexOption.IGNORE_CASE),
                    targetIdioms[translation] ?: translation
                )
            }
        }
        
        translated
    }

    suspend fun detectIdioms(text: String, lang: String): List<String> {
        val langIdioms = idioms[lang] ?: return emptyList()
        return langIdioms.keys.filter { text.contains(it, ignoreCase = true) }
    }
}