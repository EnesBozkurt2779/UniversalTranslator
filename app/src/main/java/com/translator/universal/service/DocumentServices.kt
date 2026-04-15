package com.translator.universal.service

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.translator.universal.data.service.TranslationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentTranslationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationService: TranslationService
) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    enum class DocumentFormat {
        PDF, DOCX, TXT, RTF, EPUB, HTML, XML, JSON, CSV, SRT, VTT
    }

    data class DocumentTranslationResult(
        val originalFile: File,
        val translatedFile: File?,
        val translatedText: String,
        val pageCount: Int,
        val wordCount: Int,
        val success: Boolean,
        val errorMessage: String? = null
    )

    suspend fun translateDocument(
        file: File,
        sourceLang: String,
        targetLang: String,
        format: DocumentFormat
    ): DocumentTranslationResult = withContext(Dispatchers.IO) {
        try {
            val text = when (format) {
                DocumentFormat.PDF -> extractTextFromPDF(file)
                DocumentFormat.TXT, DocumentFormat.RTF -> file.readText()
                DocumentFormat.CSV -> extractFromCSV(file)
                DocumentFormat.SRT, DocumentFormat.VTT -> extractFromSubtitle(file)
                else -> file.readText()
            }

            val wordCount = text.split("\\s+".toRegex()).size
            
            // Translate in chunks for large documents
            val translatedText = translateInChunks(text, sourceLang, targetLang)
            
            val outputFile = saveTranslatedDocument(translatedText, file.name, format)
            
            DocumentTranslationResult(
                originalFile = file,
                translatedFile = outputFile,
                translatedText = translatedText,
                pageCount = estimatePageCount(wordCount),
                wordCount = wordCount,
                success = true
            )
        } catch (e: Exception) {
            DocumentTranslationResult(
                originalFile = file,
                translatedFile = null,
                translatedText = "",
                pageCount = 0,
                wordCount = 0,
                success = false,
                errorMessage = e.message
            )
        }
    }

    private suspend fun translateInChunks(text: String, sourceLang: String, targetLang: String): String {
        val chunks = text.chunked(5000)
        val translatedChunks = mutableListOf<String>()
        
        for (chunk in chunks) {
            var result = ""
            translationService.translate(chunk, sourceLang, targetLang).collect { state ->
                when (state) {
                    is com.translator.universal.data.model.TranslationState.Success -> {
                        result = state.result.translatedText
                    }
                    else -> {}
                }
            }
            translatedChunks.add(result)
        }
        
        return translatedChunks.joinToString("\n")
    }

    private fun extractTextFromPDF(file: File): String {
        // PDF text extraction - simplified for now
        return "PDF content would be extracted here"
    }

    private fun extractFromCSV(file: File): String {
        return file.readLines().joinToString("\n") { line ->
            line.split(",").joinToString(" | ")
        }
    }

    private fun extractFromSubtitle(file: File): String {
        return file.readLines().filter { 
            it.isNotBlank() && !it.startsWith("[") && !it.matches(Regex("^\\d+$"))
        }.joinToString("\n")
    }

    private fun estimatePageCount(wordCount: Int): Int {
        return (wordCount / 250).coerceAtLeast(1)
    }

    private fun saveTranslatedDocument(text: String, originalName: String, format: DocumentFormat): File {
        val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "translations")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val outputName = originalName.replaceBeforeLast(".", "_translated")
        val outputFile = File(outputDir, outputName)
        
        outputFile.writeText(text)
        return outputFile
    }

    suspend fun translateSubtitleFile(
        file: File,
        sourceLang: String,
        targetLang: String
    ): DocumentTranslationResult = withContext(Dispatchers.IO) {
        translateDocument(file, sourceLang, targetLang, DocumentFormat.SRT)
    }

    suspend fun batchTranslate(
        files: List<File>,
        sourceLang: String,
        targetLang: String
    ): List<DocumentTranslationResult> = withContext(Dispatchers.IO) {
        files.map { file ->
            val format = when (file.extension.lowercase()) {
                "pdf" -> DocumentFormat.PDF
                "txt" -> DocumentFormat.TXT
                "doc" -> DocumentFormat.TXT
                "srt" -> DocumentFormat.SRT
                "vtt" -> DocumentFormat.VTT
                "csv" -> DocumentFormat.CSV
                else -> DocumentFormat.TXT
            }
            translateDocument(file, sourceLang, targetLang, format)
        }
    }
}

@Singleton
class WebPageTranslationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationService: TranslationService
) {
    data class WebTranslationResult(
        val url: String,
        val title: String,
        val translatedContent: String,
        val originalContent: String,
        val links: List<String>,
        val images: List<String>
    )

    suspend fun translateWebPage(
        url: String,
        sourceLang: String,
        targetLang: String
    ): WebTranslationResult = withContext(Dispatchers.IO) {
        try {
            // Simplified - in production would use JSoup or similar
            val htmlContent = fetchHtml(url)
            val extractedText = extractTextFromHtml(htmlContent)
            val title = extractTitle(htmlContent)
            val links = extractLinks(htmlContent)
            val images = extractImages(htmlContent)
            
            var translatedText = ""
            translationService.translate(extractedText, sourceLang, targetLang).collect { state ->
                when (state) {
                    is com.translator.universal.data.model.TranslationState.Success -> {
                        translatedText = state.result.translatedText
                    }
                    else -> {}
                }
            }
            
            WebTranslationResult(
                url = url,
                title = title,
                translatedContent = translatedText,
                originalContent = extractedText,
                links = links,
                images = images
            )
        } catch (e: Exception) {
            WebTranslationResult(url, "", "", "", emptyList(), emptyList())
        }
    }

    private fun fetchHtml(url: String): String = ""
    private fun extractTextFromHtml(html: String): String = html.replace(Regex("<[^>]*>"), " ")
    private fun extractTitle(html: String): String = "Web Page"
    private fun extractLinks(html: String): List<String> = emptyList()
    private fun extractImages(html: String): List<String> = emptyList()
}

@Singleton
class BatchProcessingService @Inject constructor(
    private val translationService: TranslationService
) {
    data class BatchResult(
        val totalItems: Int,
        val successful: Int,
        val failed: Int,
        val results: List<BatchItemResult>
    )

    data class BatchItemResult(
        val index: Int,
        val originalText: String,
        val translatedText: String?,
        val success: Boolean,
        val errorMessage: String? = null
    )

    suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String,
        onProgress: (Int, Int) -> Unit
    ): BatchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<BatchItemResult>()
        var successful = 0
        var failed = 0

        texts.forEachIndexed { index, text ->
            try {
                var translated = ""
                translationService.translate(text, sourceLang, targetLang).collect { state ->
                    when (state) {
                        is com.translator.universal.data.model.TranslationState.Success -> {
                            translated = state.result.translatedText
                        }
                        else -> {}
                    }
                }

                results.add(BatchItemResult(index, text, translated, true))
                successful++
            } catch (e: Exception) {
                results.add(BatchItemResult(index, text, null, false, e.message))
                failed++
            }

            onProgress(index + 1, texts.size)
        }

        BatchResult(texts.size, successful, failed, results)
    }
}