package com.translator.universal.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private var currentLanguage: Locale = Locale("tr", "TR")
    
    private val languageCodeMap = mapOf(
        "tr" to Locale("tr", "TR"),
        "en" to Locale("en", "US"),
        "de" to Locale("german"),
        "fr" to Locale("fr", "FR"),
        "es" to Locale("es", "ES"),
        "it" to Locale("it", "IT"),
        "ru" to Locale("ru", "RU"),
        "ar" to Locale("ar"),
        "zh" to Locale("chinese"),
        "ja" to Locale("japanese"),
        "ko" to Locale("korean"),
        "pt" to Locale("pt", "BR"),
        "nl" to Locale("nl"),
        "pl" to Locale("pl"),
        "hi" to Locale("hi"),
        "th" to Locale("th"),
        "vi" to Locale("vi"),
        "id" to Locale("in"),
        "ms" to Locale("ms"),
        "uk" to Locale("uk"),
        "el" to Locale("el"),
        "sv" to Locale("sv"),
        "da" to Locale("da"),
        "fi" to Locale("fi"),
        "no" to Locale("no"),
        "cs" to Locale("cs"),
        "ro" to Locale("ro"),
        "hu" to Locale("hu"),
        "he" to Locale("he")
    )

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguage(currentLanguage)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun setLanguage(languageCode: String): Boolean {
        if (!isInitialized) return false
        
        val locale = languageCodeMap[languageCode] ?: Locale.getDefault()
        currentLanguage = locale
        
        val result = textToSpeech?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, languageCode: String = "tr") {
        if (!isInitialized) return
        
        setLanguage(languageCode)
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun isLanguageSupported(languageCode: String): Boolean {
        val locale = languageCodeMap[languageCode] ?: return false
        val result = textToSpeech?.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}