package com.example.vantaread.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NovelDetailViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    var novelUrl: String = ""
        private set
    var sourceId: String = ""
        private set

    private val _novelDetails = MutableStateFlow<NovelDetails?>(null)
    val novelDetails: StateFlow<NovelDetails?> = _novelDetails.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _readChapterUrls = MutableStateFlow<Set<String>>(emptySet())
    val readChapterUrls: StateFlow<Set<String>> = _readChapterUrls.asStateFlow()

    private val _lastReadChapterUrl = MutableStateFlow<String?>(null)
    val lastReadChapterUrl: StateFlow<String?> = _lastReadChapterUrl.asStateFlow()

    fun initialize(novelUrl: String, sourceId: String) {
        if (this.novelUrl == novelUrl) return
        this.novelUrl = novelUrl
        this.sourceId = sourceId
        
        _novelDetails.value = null
        _chapters.value = emptyList()
        _isBookmarked.value = false
        
        loadNovelDetails()
        checkBookmarkStatus()
    }

    private fun loadNovelDetails() {
        viewModelScope.launch {
            try {
                // Fetch details from network
                val details = novelRepository.getNovelDetails(novelUrl, sourceId)
                _novelDetails.value = details
                
                // Fetch chapters from network and cache
                val fetchedChapters = novelRepository.fetchAndCacheChapters(novelUrl, sourceId)
                
                // Load chapters from DB to get read status
                novelRepository.getChaptersForNovelDb(novelUrl).collect { dbChapters ->
                    if (dbChapters.isNotEmpty()) {
                        _chapters.value = dbChapters
                    }
                    
                    val readUrls = novelRepository.getReadChapterUrls(novelUrl)
                    _readChapterUrls.value = readUrls.toSet()
                    
                    val lastRead = novelRepository.getLastReadChapter(novelUrl)
                    _lastReadChapterUrl.value = lastRead?.chapterUrl
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            val dbNovel = novelRepository.getNovelFromDb(novelUrl)
            _isBookmarked.value = dbNovel?.isBookmarked == true
        }
    }

    fun toggleBookmark() {
        val details = _novelDetails.value ?: return
        viewModelScope.launch {
            novelRepository.toggleBookmark(details, sourceId)
            _isBookmarked.value = !_isBookmarked.value
        }
    }
}
