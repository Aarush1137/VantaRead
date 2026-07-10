package com.example.vantaread.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.prefs.ReaderPreferencesManager
import com.example.vantaread.data.prefs.SourcePreferencesManager
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.SourceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: ReaderPreferencesManager,
    private val sourcePreferencesManager: SourcePreferencesManager,
    private val novelRepository: NovelRepository
) : ViewModel() {

    val currentTheme: StateFlow<String> = preferencesManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
        
    val defaultSource: StateFlow<String> = preferencesManager.defaultSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourceCatalog.DEFAULT_SOURCE_ID)

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    fun setDefaultSource(sourceId: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultSource(sourceId)
            sourcePreferencesManager.setActiveSource(sourceId)
        }
    }

    val batchDownloadAmount: StateFlow<Int> = preferencesManager.batchDownloadAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setBatchDownloadAmount(amount: Int) {
        viewModelScope.launch {
            preferencesManager.setBatchDownloadAmount(amount)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            novelRepository.clearHistory()
        }
    }
}
