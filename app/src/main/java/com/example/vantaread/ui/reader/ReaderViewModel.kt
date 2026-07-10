package com.example.vantaread.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    var chapterUrl: String = ""
        private set
    var sourceId: String = ""
        private set
    var novelUrl: String = ""
        private set

    private val _content = MutableStateFlow<String?>(null)
    val content: StateFlow<String?> = _content.asStateFlow()

    private val _chapterTitle = MutableStateFlow("")
    val chapterTitle: StateFlow<String> = _chapterTitle.asStateFlow()

    private val _isAutoScrolling = MutableStateFlow(false)
    val isAutoScrolling: StateFlow<Boolean> = _isAutoScrolling.asStateFlow()

    private val _autoScrollSpeed = MutableStateFlow(1.0f)
    val autoScrollSpeed: StateFlow<Float> = _autoScrollSpeed.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _initialScrollIndex = MutableStateFlow(0)
    val initialScrollIndex: StateFlow<Int> = _initialScrollIndex.asStateFlow()

    private var saveScrollJob: Job? = null

    fun initialize(chapterUrl: String, sourceId: String, novelUrl: String) {
        if (this.chapterUrl == chapterUrl) return
        this.chapterUrl = chapterUrl
        this.sourceId = sourceId
        this.novelUrl = novelUrl
        
        _content.value = null
        _initialScrollIndex.value = 0
        _isAutoScrolling.value = false
        
        loadChapter()
        loadChapterList()
    }

    private fun loadChapter() {
        viewModelScope.launch {
            try {
                // Restore scroll position
                val historyEntry = novelRepository.getHistoryEntry(chapterUrl)
                if (historyEntry != null) {
                    _initialScrollIndex.value = historyEntry.scrollPosition
                }

                val htmlContent = novelRepository.getChapterContent(chapterUrl, sourceId)
                android.util.Log.d("ReaderViewModel", "Chapter loaded, length: ${htmlContent.length}")
                _content.value = htmlContent

                // Try to find the chapter to get its title
                val novel = novelRepository.getNovelFromDb(novelUrl)
                val dbChapters = novelRepository.getChaptersListForNovelDb(novelUrl)
                val chapter = dbChapters.find { it.url == chapterUrl }
                if (chapter != null) {
                    _chapterTitle.value = chapter.title
                }

                // Record history entry
                novelRepository.recordChapterRead(
                    novelUrl = novelUrl,
                    chapterUrl = chapterUrl,
                    sourceId = sourceId,
                    novelTitle = novel?.title ?: "Unknown Novel",
                    chapterTitle = chapter?.title ?: "Unknown Chapter",
                    coverUrl = novel?.coverUrl ?: ""
                )

            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Chapter load error", e)
                _content.value = "Failed to load chapter content."
            }
        }
    }

    private fun loadChapterList() {
        viewModelScope.launch {
            try {
                val chapterList = novelRepository.getChaptersListForNovelDb(novelUrl)
                _chapters.value = chapterList
                
                // Find current chapter index
                val index = chapterList.indexOfFirst { it.url == chapterUrl }
                _currentChapterIndex.value = index
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to load chapter list", e)
            }
        }
    }

    fun saveScrollPosition(firstVisibleItemIndex: Int, maxItems: Int) {
        if (chapterUrl.isEmpty() || novelUrl.isEmpty()) return

        saveScrollJob?.cancel()
        saveScrollJob = viewModelScope.launch {
            delay(1000) // Debounce for 1 second
            try {
                novelRepository.updateHistoryScrollPosition(chapterUrl, firstVisibleItemIndex, maxItems)
                novelRepository.updateReadingProgress(novelUrl, chapterUrl, firstVisibleItemIndex)
                android.util.Log.d("ReaderViewModel", "Scroll position saved: $firstVisibleItemIndex")
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to save scroll position", e)
            }
        }
    }

    fun toggleAutoScroll() {
        _isAutoScrolling.value = !_isAutoScrolling.value
    }

    fun setAutoScrollSpeed(speed: Float) {
        _autoScrollSpeed.value = speed.coerceIn(0.5f, 5.0f)
    }

    fun navigateToChapter(index: Int): ChapterEntity? {
        val chapterList = _chapters.value
        if (index in chapterList.indices) {
            return chapterList[index]
        }
        return null
    }
}
