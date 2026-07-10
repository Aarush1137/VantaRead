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

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val novelRepository: NovelRepository
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
        fetchPopularNovels()
    }

    private fun fetchPopularNovels() {
        viewModelScope.launch {
            try {
                // For now, hardcode "wtr-lab" as the default source for dashboard suggestions
                _popularNovels.value = novelRepository.getPopularNovels("wtr-lab")
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
