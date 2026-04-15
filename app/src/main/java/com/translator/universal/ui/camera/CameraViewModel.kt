package com.translator.universal.ui.camera

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognizer
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.ImageTextRecognitionService
import com.translator.universal.data.service.TranslationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageTextRecognitionService: ImageTextRecognitionService,
    private val translationService: TranslationService
) : ViewModel() {

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            imageTextRecognitionService.recognizeTextFromUri(uri).collect { state ->
                when (state) {
                    is TranslationState.Success -> {
                        _recognizedText.value = state.result.sourceText
                    }
                    is TranslationState.Error -> {
                        _recognizedText.value = "Hata: ${state.message}"
                    }
                    else -> {}
                }
            }
        }
    }

    fun captureAndTranslate(imageCapture: ImageCapture, executor: Executor) {
        viewModelScope.launch {
            try {
                imageCapture.takePicture(executor, object : androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val mediaImage = image.image
                        if (mediaImage != null) {
                            viewModelScope.launch {
                                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                val result = textRecognizer.process(inputImage).await()
                                _recognizedText.value = result.text
                                
                                if (result.text.isNotEmpty()) {
                                    translateText(result.text, "tr")
                                }
                                image.close()
                            }
                        }
                    }

                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                        viewModelScope.launch {
                            _translationState.value = TranslationState.Error(exception.message ?: "Fotoğraf çekme hatası")
                        }
                    }
                })
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(e.message ?: "Fotoğraf çekme hatası")
            }
        }
    }

    fun translateText(text: String, targetLang: String) {
        viewModelScope.launch {
            translationService.translate(text, "auto", targetLang).collect { state ->
                _translationState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}