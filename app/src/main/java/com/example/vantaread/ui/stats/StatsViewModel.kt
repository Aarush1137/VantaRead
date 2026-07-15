package com.example.vantaread.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val totalChaptersRead: Int = 0,
    val totalNovelsStarted: Int = 0,
    val mostReadNovelTitle: String = "None yet",
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = novelRepository.getReadingHistory()
        .map { historyList ->
            if (historyList.isEmpty()) {
                StatsUiState(isLoading = false)
            } else {
                val totalChapters = historyList.size
                val uniqueNovels = historyList.map { it.novelUrl }.distinct().size
                
                // Group by novel URL to find the most read novel
                val mostRead = historyList.groupBy { it.novelUrl }
                    .maxByOrNull { it.value.size }
                    ?.value?.firstOrNull()?.novelTitle ?: "Unknown"
                    
                StatsUiState(
                    totalChaptersRead = totalChapters,
                    totalNovelsStarted = uniqueNovels,
                    mostReadNovelTitle = mostRead,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatsUiState(isLoading = true)
        )
}
