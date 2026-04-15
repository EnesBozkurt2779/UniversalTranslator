package com.translator.universal.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translator.universal.data.local.FavoriteTranslationEntity
import com.translator.universal.data.local.TranslationHistoryEntity
import com.translator.universal.data.repository.FavoriteRepository
import com.translator.universal.data.repository.TranslationHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: TranslationHistoryRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _historyList = MutableStateFlow<List<TranslationHistoryEntity>>(emptyList())
    val historyList: StateFlow<List<TranslationHistoryEntity>> = _historyList.asStateFlow()

    private val _historyCount = MutableStateFlow(0)
    val historyCount: StateFlow<Int> = _historyCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            historyRepository.getRecentHistory(100).collectLatest { list ->
                _historyList.value = list
            }
        }
        viewModelScope.launch {
            historyRepository.getHistoryCount().collectLatest { count ->
                _historyCount.value = count
            }
        }
    }

    fun searchHistory(query: String) {
        if (query.isEmpty()) {
            loadHistory()
        } else {
            viewModelScope.launch {
                historyRepository.searchHistory(query).collectLatest { list ->
                    _historyList.value = list
                }
            }
        }
    }

    fun deleteHistory(history: TranslationHistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteHistory(history)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }

    fun toggleFavorite(history: TranslationHistoryEntity) {
        viewModelScope.launch {
            val isFav = favoriteRepository.isFavorite(history.sourceText, history.targetLanguage)
            if (isFav) {
                favoriteRepository.removeFavorite(
                    FavoriteTranslationEntity(
                        sourceText = history.sourceText,
                        translatedText = history.translatedText,
                        sourceLanguage = history.sourceLanguage,
                        targetLanguage = history.targetLanguage
                    )
                )
            } else {
                favoriteRepository.addFavorite(
                    history.sourceText,
                    history.translatedText,
                    history.sourceLanguage,
                    history.targetLanguage
                )
            }
        }
    }
}