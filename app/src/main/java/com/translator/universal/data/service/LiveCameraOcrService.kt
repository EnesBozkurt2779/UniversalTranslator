package com.translator.universal.data.service

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveCameraOcrService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var isProcessing = false
    private var lastRecognizedText = ""

    sealed class OcrResult {
        data class TextFound(val text: String, val timestamp: Long = System.currentTimeMillis()) : OcrResult()
        data class Error(val message: String) : OcrResult()
        object NoText : OcrResult()
    }

    fun startLiveOcr(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onTextDetected: (String) -> Unit
    ): Flow<OcrResult> = callbackFlow {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isProcessing) {
                            isProcessing = true
                            processImage(imageProxy) { text ->
                                if (text.isNotEmpty() && text != lastRecognizedText) {
                                    lastRecognizedText = text
                                    trySend(OcrResult.TextFound(text))
                                    onTextDetected(text)
                                } else if (text.isEmpty()) {
                                    trySend(OcrResult.NoText)
                                }
                                isProcessing = false
                            }
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                trySend(OcrResult.Error("Kamera başlatılamadı: ${e.message}"))
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            cameraExecutor.shutdown()
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: androidx.camera.core.ImageProxy, callback: (String) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            textRecognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    callback(result.text)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    callback("")
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            callback("")
        }
    }

    fun stopOcr() {
        cameraExecutor.shutdown()
    }

    fun clearLastText() {
        lastRecognizedText = ""
    }
}