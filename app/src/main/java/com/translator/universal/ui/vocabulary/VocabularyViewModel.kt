package com.translator.universal.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translator.universal.data.local.VocabularyEntity
import com.translator.universal.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) : ViewModel() {

    private val _wordList = MutableStateFlow<List<VocabularyEntity>>(emptyList())
    val wordList: StateFlow<List<VocabularyEntity>> = _wordList.asStateFlow()

    private val _wordCount = MutableStateFlow(0)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _learnedCount = MutableStateFlow(0)
    val learnedCount: StateFlow<Int> = _learnedCount.asStateFlow()

    private val _flashcardMode = MutableStateFlow(false)
    val flashcardMode: StateFlow<Boolean> = _flashcardMode.asStateFlow()

    private val _currentFlashcard = MutableStateFlow<VocabularyEntity?>(null)
    val currentFlashcard: StateFlow<VocabularyEntity?> = _currentFlashcard.asStateFlow()

    init {
        loadAllWords()
        loadCounts()
    }

    private fun loadCounts() {
        viewModelScope.launch {
            vocabularyRepository.getWordCount().collectLatest { count ->
                _wordCount.value = count
            }
        }
        viewModelScope.launch {
            vocabularyRepository.getLearnedWordCount().collectLatest { count ->
                _learnedCount.value = count
            }
        }
    }

    fun loadAllWords() {
        viewModelScope.launch {
            vocabularyRepository.getAllWords().collectLatest { list ->
                _wordList.value = list
            }
        }
    }

    fun loadUnlearnedWords() {
        viewModelScope.launch {
            vocabularyRepository.getUnlearnedWords().collectLatest { list ->
                _wordList.value = list
            }
        }
    }

    fun loadLearnedWords() {
        viewModelScope.launch {
            vocabularyRepository.getLearnedWords().collectLatest { list ->
                _wordList.value = list
            }
        }
    }

    fun addWord(
        word: String,
        translation: String,
        sourceLanguage: String,
        targetLanguage: String,
        example: String? = null,
        pronunciation: String? = null
    ) {
        viewModelScope.launch {
            vocabularyRepository.addWord(
                word = word,
                translation = translation,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                example = example,
                pronunciation = pronunciation
            )
        }
    }

    fun deleteWord(word: VocabularyEntity) {
        viewModelScope.launch {
            vocabularyRepository.deleteWord(word)
        }
    }

    fun toggleLearned(word: VocabularyEntity) {
        viewModelScope.launch {
            if (word.isLearned) {
                vocabularyRepository.markAsUnlearned(word.id)
            } else {
                vocabularyRepository.markAsLearned(word.id)
            }
        }
    }

    fun startFlashcardMode() {
        _flashcardMode.value = true
        viewModelScope.launch {
            vocabularyRepository.getUnlearnedWords().collectLatest { words ->
                if (words.isNotEmpty()) {
                    _currentFlashcard.value = words.random()
                }
            }
        }
    }

    fun nextFlashcard() {
        viewModelScope.launch {
            vocabularyRepository.getUnlearnedWords().collectLatest { words ->
                if (words.isNotEmpty()) {
                    _currentFlashcard.value = words.random()
                } else {
                    _currentFlashcard.value = null
                }
            }
        }
    }

    fun markFlashcardAsLearned() {
        _currentFlashcard.value?.let { word ->
            viewModelScope.launch {
                vocabularyRepository.markAsLearned(word.id)
                nextFlashcard()
            }
        }
    }

    fun exitFlashcardMode() {
        _flashcardMode.value = false
        _currentFlashcard.value = null
    }

    fun searchWords(query: String) {
        if (query.isEmpty()) {
            loadAllWords()
        } else {
            viewModelScope.launch {
                vocabularyRepository.searchWords(query).collectLatest { list ->
                    _wordList.value = list
                }
            }
        }
    }
}