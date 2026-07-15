package com.example.vantaread.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.prefs.SourcePreferencesManager
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.SourceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ALL_SOURCES = "all"

data class SuggestionsUiState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSourceId: String = ALL_SOURCES,
    val successfulSourceIds: List<String> = emptyList(),
    val failedSourceIds: List<String> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    sourcePreferencesManager: SourcePreferencesManager
) : ViewModel() {

    val activeSourceId: StateFlow<String> = sourcePreferencesManager.activeSourceId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourceCatalog.DEFAULT_SOURCE_ID)

    private val _uiState = MutableStateFlow(SuggestionsUiState())
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectSource(sourceId: String) {
        _uiState.value = _uiState.value.copy(selectedSourceId = sourceId)
        refresh(forceRefresh = false)
    }

    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedSourceId
            val requestedSources = if (selected == ALL_SOURCES) {
                val active = activeSourceId.value
                listOf(active) + SourceCatalog.sources.map { it.id }.filterNot { it == active }
            } else {
                listOf(selected)
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            if (selected == ALL_SOURCES && requestedSources.isNotEmpty()) {
                val quickResult = novelRepository.getSuggestedNovels(
                    sourceIds = listOf(requestedSources.first()),
                    forceRefresh = false
                )
                if (quickResult.novels.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        novels = quickResult.novels,
                        successfulSourceIds = quickResult.successfulSourceIds,
                        failedSourceIds = quickResult.failedSourceIds,
                        errorMessage = "Showing quick suggestions while other sources load."
                    )
                }
            }

            val result = novelRepository.getSuggestedNovels(requestedSources, forceRefresh)
            val message = when {
                result.novels.isNotEmpty() && result.failedSourceIds.isNotEmpty() ->
                    "Loaded ${result.successfulSourceIds.size} sources. ${result.failedSourceIds.size} sources were slow or unavailable."
                result.novels.isEmpty() ->
                    "No suggestions loaded. Pull to retry or choose another source."
                else -> null
            }
            _uiState.value = _uiState.value.copy(
                novels = result.novels,
                isLoading = false,
                successfulSourceIds = result.successfulSourceIds,
                failedSourceIds = result.failedSourceIds,
                errorMessage = message
            )
        }
    }
}
