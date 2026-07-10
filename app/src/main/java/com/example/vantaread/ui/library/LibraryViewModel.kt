package com.example.vantaread.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.vantaread.data.db.ReadingHistoryEntity
import com.example.vantaread.data.prefs.SourcePreferencesManager
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val sourcePrefs: SourcePreferencesManager
) : ViewModel() {

    val recentReads: StateFlow<List<ReadingHistoryEntity>> = novelRepository.getRecentNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedNovels: StateFlow<List<NovelEntity>> = novelRepository.getBookmarkedNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _popularNovels = MutableStateFlow<List<com.example.vantaread.data.model.Novel>>(emptyList())
    val popularNovels: StateFlow<List<com.example.vantaread.data.model.Novel>> = _popularNovels

    init {
        viewModelScope.launch {
            sourcePrefs.activeSourceId.collect { sourceId ->
                fetchPopularNovels(sourceId)
            }
        }
    }

    private fun fetchPopularNovels(sourceId: String) {
        viewModelScope.launch {
            try {
                _popularNovels.value = novelRepository.getPopularNovels(sourceId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
