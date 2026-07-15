package com.example.vantaread.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.model.AppAccent
import com.example.vantaread.data.prefs.ReaderPreferencesManager
import com.example.vantaread.data.prefs.SourcePreferencesManager
import com.example.vantaread.data.repository.AuthRepository
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.SourceCatalog
import com.example.vantaread.data.util.VantaStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: ReaderPreferencesManager,
    private val sourcePreferencesManager: SourcePreferencesManager,
    private val novelRepository: NovelRepository,
    private val storageManager: VantaStorageManager,
    authRepository: AuthRepository
) : ViewModel() {

    val currentTheme: StateFlow<String> = preferencesManager.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val currentAccent: StateFlow<AppAccent> = preferencesManager.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppAccent.VANTA_PURPLE)
        
    val defaultSource: StateFlow<String> = preferencesManager.defaultSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourceCatalog.DEFAULT_SOURCE_ID)

    val currentUser = authRepository.currentUser
    val isFirebaseConfigured = authRepository.isConfigured

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    fun setAccent(accent: AppAccent) {
        viewModelScope.launch {
            preferencesManager.setAccent(accent)
        }
    }

    fun setDefaultSource(sourceId: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultSource(sourceId)
            sourcePreferencesManager.setActiveSource(sourceId)
        }
    }

    val storageUri: StateFlow<String?> = preferencesManager.storageUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isMigrating = MutableStateFlow(false)
    val isMigrating: StateFlow<Boolean> = _isMigrating.asStateFlow()

    fun updateStorageUri(newUri: String?) {
        val oldUri = preferencesManager.storageUri.value
        if (oldUri == newUri) return

        viewModelScope.launch {
            _isMigrating.value = true
            _message.value = "Migrating data to new location..."
            try {
                storageManager.migrate(oldUri, newUri)
                preferencesManager.setStorageUri(newUri)
                storageManager.setBaseUri(newUri)
                novelRepository.setStorageUri(newUri)
                _message.value = "Storage location updated and data migrated."
            } catch (e: Exception) {
                _message.value = "Migration failed: ${e.message}"
            } finally {
                _isMigrating.value = false
            }
        }
    }

    val batchDownloadAmount: StateFlow<Int> = preferencesManager.batchDownloadAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setBatchDownloadAmount(amount: Int) {
        viewModelScope.launch {
            preferencesManager.setBatchDownloadAmount(amount)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            novelRepository.clearHistory()
            _message.value = "Reading history cleared"
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            novelRepository.removeAllDownloadedChapters()
            _message.value = "Offline downloads cleared"
        }
    }
}
