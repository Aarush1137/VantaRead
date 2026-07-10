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
import com.example.vantaread.data.prefs.ReaderPreferencesManager
import com.example.vantaread.data.prefs.SourcePreferencesManager
import com.example.vantaread.data.source.SourceCatalog
import com.example.vantaread.worker.ChapterDownloadWorker

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.combine

enum class SortOption(val displayName: String) {
    DEFAULT("Default"),
    ALPHABETICAL("Alphabetical"),
    RECENTLY_READ("Recently Read")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val sourcePrefs: SourcePreferencesManager,
    private val readerPreferencesManager: ReaderPreferencesManager,
    private val workManager: WorkManager
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
            val trimmedUrl = url.trim()
            val sourceId = SourceCatalog.detectSourceId(trimmedUrl)
            
            if (sourceId == null) {
                _addNovelResult.value = Result.failure(Exception("Unsupported URL or Source"))
                return@launch
            }
            
            try {
                val details = novelRepository.getNovelDetails(trimmedUrl, sourceId)
                val chapters = novelRepository.fetchAndCacheChapters(trimmedUrl, sourceId)
                
                // Add to bookmark if not already
                val existing = novelRepository.getNovelFromDb(trimmedUrl)
                if (existing?.isBookmarked != true) {
                    novelRepository.toggleBookmark(details, sourceId)
                }

                val batchAmount = readerPreferencesManager.batchDownloadAmount.value
                if (batchAmount > 0 && chapters.isNotEmpty()) {
                    val requests = chapters
                        .take(if (batchAmount >= 100) chapters.size else batchAmount)
                        .map { chapter ->
                            val data = Data.Builder()
                                .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapter.url)
                                .putString(ChapterDownloadWorker.KEY_SOURCE_ID, sourceId)
                                .build()

                            OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                                .setInputData(data)
                                .build()
                        }
                    workManager.enqueue(requests)
                }
                
                _addNovelResult.value = Result.success(details.title)
            } catch (e: Exception) {
                _addNovelResult.value = Result.failure(e)
            }
        }
    }
}
