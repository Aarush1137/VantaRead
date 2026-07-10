package com.example.vantaread.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ReadingHistoryEntity
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    val readingHistory: StateFlow<List<ReadingHistoryEntity>> = novelRepository.getReadingHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            novelRepository.clearHistory()
        }
    }

    fun deleteEntry(chapterUrl: String) {
        viewModelScope.launch {
            novelRepository.deleteHistoryEntry(chapterUrl)
        }
    }
}
