package com.example.vantaread.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.DownloadedChapter
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.db.ReadingHistoryEntity
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.SourceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val totalChaptersRead: Int = 0,
    val totalNovelsStarted: Int = 0,
    val bookmarkedNovels: Int = 0,
    val downloadedChapters: Int = 0,
    val offlineNovels: Int = 0,
    val readingStreakDays: Int = 0,
    val mostReadNovelTitle: String = "None yet",
    val favoriteSourceName: String = "None yet",
    val lastReadTitle: String = "Nothing yet",
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        novelRepository.getReadingHistory(),
        novelRepository.getBookmarkedNovels(),
        novelRepository.getDownloadedChapters()
    ) { historyList: List<ReadingHistoryEntity>, bookmarks: List<NovelEntity>, downloads: List<DownloadedChapter> ->
        if (historyList.isEmpty()) {
            StatsUiState(
                bookmarkedNovels = bookmarks.size,
                downloadedChapters = downloads.size,
                offlineNovels = downloads.distinctBy { it.novelUrl }.size,
                isLoading = false
            )
        } else {
            val mostRead = historyList.groupBy { it.novelUrl }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()?.novelTitle ?: "Unknown"

            val favoriteSource = historyList.groupBy { it.sourceId }
                .maxByOrNull { it.value.size }
                ?.key
                ?.let(SourceCatalog::nameFor) ?: "None yet"

            StatsUiState(
                totalChaptersRead = historyList.size,
                totalNovelsStarted = historyList.distinctBy { it.novelUrl }.size,
                bookmarkedNovels = bookmarks.size,
                downloadedChapters = downloads.size,
                offlineNovels = downloads.distinctBy { it.novelUrl }.size,
                readingStreakDays = calculateReadingStreak(historyList),
                mostReadNovelTitle = mostRead,
                favoriteSourceName = favoriteSource,
                lastReadTitle = historyList.maxByOrNull { it.lastReadTimestamp }?.novelTitle ?: "Nothing yet",
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    private fun calculateReadingStreak(historyList: List<ReadingHistoryEntity>): Int {
        val oneDayMs = 24L * 60L * 60L * 1000L
        val readDays = historyList
            .map { it.lastReadTimestamp / oneDayMs }
            .toSet()
        if (readDays.isEmpty()) return 0

        var currentDay = System.currentTimeMillis() / oneDayMs
        if (currentDay !in readDays) {
            currentDay -= 1
        }

        var streak = 0
        while (currentDay in readDays) {
            streak += 1
            currentDay -= 1
        }
        return streak
    }
}
