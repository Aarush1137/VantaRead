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

import kotlinx.coroutines.flow.combine

enum class SortOption(val displayName: String) {
    DEFAULT("Default"),
    ALPHABETICAL("Alphabetical"),
    RECENTLY_READ("Recently Read")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val sourcePrefs: SourcePreferencesManager
) : ViewModel() {

    val currentSortOption = MutableStateFlow(SortOption.DEFAULT)

    val recentReads: StateFlow<List<ReadingHistoryEntity>> = novelRepository.getRecentNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedNovels: StateFlow<List<NovelEntity>> = combine(
        novelRepository.getBookmarkedNovels(),
        currentSortOption,
        recentReads
    ) { novels, sortOption, recent ->
        when (sortOption) {
            SortOption.DEFAULT -> novels
            SortOption.ALPHABETICAL -> novels.sortedBy { it.title }
            SortOption.RECENTLY_READ -> {
                val recentMap = recent.associateBy { it.novelUrl }
                novels.sortedByDescending { recentMap[it.url]?.lastReadTimestamp ?: 0L }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _popularNovels = MutableStateFlow<List<com.example.vantaread.data.model.Novel>>(emptyList())
    val popularNovels: StateFlow<List<com.example.vantaread.data.model.Novel>> = _popularNovels

    private val _isLoadingPopularNovels = MutableStateFlow(true)
    val isLoadingPopularNovels: StateFlow<Boolean> = _isLoadingPopularNovels

    val activeSourceId: StateFlow<String> = sourcePrefs.activeSourceId

    fun setActiveSource(sourceId: String) {
        sourcePrefs.setActiveSource(sourceId)
    }

    fun setSortOption(option: SortOption) {
        currentSortOption.value = option
    }

    init {
        viewModelScope.launch {
            sourcePrefs.activeSourceId.collect { sourceId ->
                fetchPopularNovels(sourceId)
            }
        }
    }

    private fun fetchPopularNovels(sourceId: String) {
        viewModelScope.launch {
            _isLoadingPopularNovels.value = true
            try {
                _popularNovels.value = novelRepository.getPopularNovels(sourceId)
            } catch (e: Exception) {
                _popularNovels.value = emptyList() // clear on error
            } finally {
                _isLoadingPopularNovels.value = false
            }
        }
    }

    private val _addNovelResult = MutableStateFlow<Result<String>?>(null)
    val addNovelResult: StateFlow<Result<String>?> = _addNovelResult

    fun clearAddNovelResult() {
        _addNovelResult.value = null
    }

    fun addNovelViaUrl(url: String) {
        viewModelScope.launch {
            val sourceId = when {
                url.contains("novelfull.com") -> "novelfull"
                url.contains("royalroad.com") -> "royalroad"
                url.contains("lightnovelpub.vip") || url.contains("lightnovelpub.com") -> "lightnovelpub"
                url.contains("wtr-lab.com") -> "wtrlab"
                else -> null
            }
            
            if (sourceId == null) {
                _addNovelResult.value = Result.failure(Exception("Unsupported URL or Source"))
                return@launch
            }
            
            try {
                val details = novelRepository.getNovelDetails(url, sourceId)
                novelRepository.fetchAndCacheChapters(url, sourceId)
                
                // Add to bookmark if not already
                val existing = novelRepository.getNovelFromDb(url)
                if (existing?.isBookmarked != true) {
                    novelRepository.toggleBookmark(details, sourceId)
                }
                
                _addNovelResult.value = Result.success(details.title)
            } catch (e: Exception) {
                _addNovelResult.value = Result.failure(e)
            }
        }
    }
}
