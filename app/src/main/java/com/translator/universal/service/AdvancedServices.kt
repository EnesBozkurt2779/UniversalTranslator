package com.translator.universal.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperAPIService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // API for external developers
    interface TranslationCallback {
        fun onSuccess(result: String)
        fun onError(error: String)
    }

    // API Key management would go here
    fun validateApiKey(key: String): Boolean {
        return key.isNotEmpty() && key.length > 10
    }

    // Rate limiting
    private val apiCalls = mutableMapOf<String, MutableList<Long>>()

    fun checkRateLimit(apiKey: String, maxCalls: Int = 100, windowMs: Long = 60000): Boolean {
        val now = System.currentTimeMillis()
        val calls = apiCalls.getOrPut(apiKey) { mutableListOf() }
        
        // Remove old calls
        calls.removeAll { it < now - windowMs }
        
        return if (calls.size < maxCalls) {
            calls.add(now)
            true
        } else {
            false
        }
    }

    data class APIResponse(
        val success: Boolean,
        val data: Any? = null,
        val error: String? = null,
        val quotaRemaining: Int? = null
    )

    // Documentation endpoint
    fun getAPIDocumentation(): String = """
        Universal Translator API v1.0
        =============================
        
        Endpoints:
        - POST /translate - Translate text
        - GET /languages - Get supported languages
        - POST /detect - Detect language
        - GET /history - Get translation history
        
        Authentication:
        Pass API key in header: X-API-Key: your_key
        
        Rate Limits:
        - Free: 100 calls/minute
        - Pro: 1000 calls/minute
        
        Example:
        curl -X POST https://api.translator.com/translate \
          -H "Content-Type: application/json" \
          -H "X-API-Key: your_key" \
          -d '{"text": "Hello", "source": "en", "target": "tr"}'
    """.trimIndent()
}

@Singleton
class EmojiService @Inject constructor() {
    data class EmojiMeaning(
        val emoji: String,
        val name: String,
        val meaning: String,
        val examples: List<String>
    )

    private val emojiMeanings = mapOf(
        "😊" to EmojiMeaning("😊", "Smiling Face with Smiling Eyes", "Mutlu ve gülümseyen", listOf("Çok mutluyum!", "Teşekkür ederim 😊")),
        "🙏" to EmojiMeaning("🙏", "Folded Hands", "Teşekkür ve saygı", listOf("Çok teşekkürler 🙏", "Saygılarımla 🙏")),
        "👍" to EmojiMeaning("👍", "Thumbs Up", "Onay ve beğeni", listOf("Çok iyi!", "Tamam 👍")),
        "❤️" to EmojiMeaning("❤️", "Red Heart", "Aşk ve sevgi", listOf("Seni seviyorum ❤️", "Harika ❤️")),
        "😂" to EmojiMeaning("😂", "Face with Tears of Joy", "Çok gülme", listOf("Çok komik!", "Kahkaha atıyorum 😂")),
        "🔥" to EmojiMeaning("🔥", "Fire", "Harika veya trend", listOf("Çok iyi!", "Bu konu trend 🔥")),
        "💪" to EmojiMeaning("💪", "Flexed Biceps", "Güç ve başarı", listOf("Başarabilirim!", "Güçlü 💪")),
        "🎉" to EmojiMeaning("🎉", "Party Popper", "Kutlama", listOf("Tebrikler!", "Kutlama zamanı 🎉"))
    )

    fun translateEmoji(emoji: String, targetLang: String): EmojiMeaning? {
        return emojiMeanings[emoji]
    }

    fun searchEmojis(query: String): List<EmojiMeaning> {
        return emojiMeanings.values.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.meaning.contains(query, ignoreCase = true)
        }
    }

    // Convert emoji to text description
    fun emojiToText(text: String, targetLang: String): String {
        var result = text
        emojiMeanings.forEach { (emoji, meaning) ->
            result = result.replace(emoji, "[${meaning.name}]")
        }
        return result
    }
}

@Singleton
class MemeService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class MemeTemplate(
        val id: String,
        val name: String,
        val imageUrl: String,
        val textPositions: List<TextPosition>
    )

    data class TextPosition(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    fun getTemplates(): List<MemeTemplate> = listOf(
        MemeTemplate("drake", "Drake Hotline Bling", "drake.jpg", listOf(
            TextPosition(0.1f, 0.1f, 0.4f, 0.4f),
            TextPosition(0.1f, 0.55f, 0.4f, 0.4f)
        )),
        MemeTemplate("distracted", "Distracted Boyfriend", "distracted.jpg", listOf(
            TextPosition(0.1f, 0.2f, 0.25f, 0.3f),
            TextPosition(0.4f, 0.3f, 0.25f, 0.3f),
            TextPosition(0.7f, 0.25f, 0.25f, 0.3f)
        )),
        MemeTemplate("two_buttons", "Two Buttons", "two_buttons.jpg", listOf(
            TextPosition(0.1f, 0.1f, 0.4f, 0.3f),
            TextPosition(0.5f, 0.1f, 0.4f, 0.3f)
        ))
    )

    fun createMeme(templateId: String, texts: List<String>): ByteArray? {
        // Would use Canvas or GPU to create meme
        return null
    }
}

@Singleton
class TextSummaryService @Inject constructor() {
    // Summarization using extractive method
    fun summarize(text: String, maxLength: Int = 100): String {
        val sentences = text.split(Regex("[.!?]")).filter { it.trim().length > 10 }
        
        if (sentences.isEmpty()) return text.take(maxLength)
        
        // Score sentences by word frequency
        val words = text.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
        val wordFreq = words.groupingBy { it }.eachCount()
        
        val scoredSentences = sentences.mapIndexed { index, sentence ->
            val sentenceWords = sentence.lowercase().split(Regex("\\W+"))
            val score = sentenceWords.sumOf { wordFreq[it] ?: 0 }
            Pair(index, score)
        }.sortedByDescending { it.second }
        
        val topSentences = scoredSentences.take(3).map { it.first }.sorted()
        
        return topSentences.mapNotNull { sentences.getOrNull(it) }.joinToString(". ") + "."
    }

    // Keywords extraction
    fun extractKeywords(text: String, count: Int = 10): List<String> {
        val words = text.lowercase().split(Regex("\\W+"))
            .filter { it.length > 4 }
            .groupingBy { it }
            .eachCount()
        
        return words.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }
    }
}

@Singleton
class NotesService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class TranslationNote(
        val id: String,
        val originalText: String,
        val translatedText: String,
        val note: String,
        val createdAt: Long,
        val tags: List<String>
    )

    private val notes = mutableListOf<TranslationNote>()

    fun addNote(original: String, translated: String, note: String, tags: List<String> = emptyList()): TranslationNote {
        val newNote = TranslationNote(
            id = System.currentTimeMillis().toString(),
            originalText = original,
            translatedText = translated,
            note = note,
            createdAt = System.currentTimeMillis(),
            tags = tags
        )
        notes.add(newNote)
        return newNote
    }

    fun getNotes(): List<TranslationNote> = notes.toList()

    fun searchNotes(query: String): List<TranslationNote> {
        return notes.filter { 
            it.originalText.contains(query, ignoreCase = true) ||
            it.translatedText.contains(query, ignoreCase = true) ||
            it.note.contains(query, ignoreCase = true)
        }
    }

    fun deleteNote(id: String): Boolean {
        return notes.removeIf { it.id == id }
    }

    fun getNotesByTag(tag: String): List<TranslationNote> {
        return notes.filter { it.tags.contains(tag) }
    }
}