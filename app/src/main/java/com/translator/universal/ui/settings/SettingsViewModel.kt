package com.translator.universal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translator.universal.data.service.NetworkService
import com.translator.universal.data.service.TranslationService
import com.translator.universal.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val networkService: NetworkService,
    private val translationService: TranslationService
) : ViewModel() {

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _autoDetect = MutableStateFlow(true)
    val autoDetect: StateFlow<Boolean> = _autoDetect.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _networkStatus = MutableStateFlow(false)
    val networkStatus: StateFlow<Boolean> = _networkStatus.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.darkMode.collect { isDark ->
                _darkMode.value = isDark
            }
        }
        viewModelScope.launch {
            preferencesManager.autoDetect.collect { enabled ->
                _autoDetect.value = enabled
            }
        }
        _networkStatus.value = networkService.isOnline()
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun setAutoDetect(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoDetect(enabled)
        }
    }

    fun downloadLanguage(languageCode: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(languageCode)
            try {
                translationService.downloadLanguageModel(languageCode).collect { state ->
                    when (state) {
                        is com.translator.universal.data.model.TranslationState.Success -> {
                            _downloadState.value = DownloadState.Success
                        }
                        is com.translator.universal.data.model.TranslationState.Error -> {
                            _downloadState.value = DownloadState.Error(state.message)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "İndirme hatası")
            }
        }
    }

    fun deleteLanguage(languageCode: String) {
        viewModelScope.launch {
            val success = translationService.deleteLanguageModel(languageCode)
            if (success) {
                _downloadState.value = DownloadState.Success
            } else {
                _downloadState.value = DownloadState.Error("Silme hatası")
            }
        }
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val languageCode: String) : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}