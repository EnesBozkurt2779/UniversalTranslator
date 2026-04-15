package com.translator.universal.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translator.universal.data.model.ConversationMessage
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.SpeechRecognitionService
import com.translator.universal.data.service.TranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DialogViewModel @Inject constructor(
    private val translationService: TranslationService,
    private val speechRecognitionService: SpeechRecognitionService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    init {
        viewModelScope.launch {
            speechRecognitionService.speechText.collect { text ->
                _speechText.value = text
            }
        }
        viewModelScope.launch {
            speechRecognitionService.isListeningState.collect { listening ->
                _isListening.value = listening
            }
        }
    }

    fun startListening(languageCode: String) {
        speechRecognitionService.startListening(languageCode)
    }

    fun stopListening() {
        speechRecognitionService.stopListening()
    }

    fun addMessage(text: String, sourceLang: String, targetLang: String) {
        val userMessage = ConversationMessage(
            text = text,
            isFromUser = true
        )
        
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(userMessage)
        _messages.value = currentMessages

        viewModelScope.launch {
            translationService.translate(text, sourceLang, targetLang).collect { state ->
                _translationState.value = state
                
                if (state is TranslationState.Success) {
                    val translatedMessage = ConversationMessage(
                        text = state.result.translatedText,
                        isFromUser = false,
                        translatedText = text
                    )
                    val updatedMessages = _messages.value.toMutableList()
                    updatedMessages.add(translatedMessage)
                    _messages.value = updatedMessages
                }
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.destroy()
    }
}