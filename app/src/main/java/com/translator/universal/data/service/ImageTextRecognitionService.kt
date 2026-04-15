package com.translator.universal.data.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.translator.universal.data.model.TranslationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ImageTextRecognitionService @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun recognizeTextFromBitmap(bitmap: Bitmap): Flow<TranslationState> = flow {
        emit(TranslationState.Loading)
        
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage).await()
            emit(TranslationState.Success(
                com.translator.universal.data.model.TranslationResult(
                    sourceText = result.text,
                    translatedText = "",
                    sourceLanguage = "auto",
                    targetLanguage = "",
                    isOffline = true
                )
            ))
        } catch (e: Exception) {
            emit(TranslationState.Error(e.message ?: "Metin tanıma hatası"))
        }
    }.flowOn(Dispatchers.IO)

    fun recognizeTextFromUri(uri: Uri): Flow<TranslationState> = flow {
        emit(TranslationState.Loading)
        
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val result = textRecognizer.process(inputImage).await()
            emit(TranslationState.Success(
                com.translator.universal.data.model.TranslationResult(
                    sourceText = result.text,
                    translatedText = "",
                    sourceLanguage = "auto",
                    targetLanguage = "",
                    isOffline = true
                )
            ))
        } catch (e: Exception) {
            emit(TranslationState.Error(e.message ?: "Metin tanıma hatası"))
        }
    }.flowOn(Dispatchers.IO)

    fun recognizeTextFromPath(path: String): Flow<TranslationState> = flow {
        emit(TranslationState.Loading)
        
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val result = textRecognizer.process(inputImage).await()
                emit(TranslationState.Success(
                    com.translator.universal.data.model.TranslationResult(
                        sourceText = result.text,
                        translatedText = "",
                        sourceLanguage = "auto",
                        targetLanguage = "",
                        isOffline = true
                    )
                ))
            } else {
                emit(TranslationState.Error("Görüntü okunamadı"))
            }
        } catch (e: Exception) {
            emit(TranslationState.Error(e.message ?: "Metin tanıma hatası"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun captureAndRecognize(
        imageCapture: ImageCapture,
        executor: ExecutorService
    ): String = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            override fun onCaptureSuccess(image: ImageProxy) {
                val mediaImage = image.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { result ->
                            continuation.resume(result.text)
                            image.close()
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                            image.close()
                        }
                } else {
                    continuation.resumeWithException(Exception("No image available"))
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    fun close() {
        textRecognizer.close()
        cameraExecutor.shutdown()
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = 
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result)
            }
            addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
}