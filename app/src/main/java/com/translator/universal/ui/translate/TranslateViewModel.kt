package com.translator.universal.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.TextToSpeechService
import com.translator.universal.data.service.TranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslateViewModel @Inject constructor(
    private val translationService: TranslationService,
    private val textToSpeechService: TextToSpeechService
) : ViewModel() {

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    fun translate(text: String, sourceLang: String, targetLang: String) {
        viewModelScope.launch {
            translationService.translate(text, sourceLang, targetLang).collect { state ->
                _translationState.value = state
            }
        }
    }

    fun speak(text: String, languageCode: String) {
        textToSpeechService.speak(text, languageCode)
    }

    fun stopSpeaking() {
        textToSpeechService.stop()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeechService.stop()
    }
}