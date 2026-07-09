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

    fun initialize(novelUrl: String, sourceId: String) {
        if (this.novelUrl.isNotEmpty()) return // already initialized
        this.novelUrl = novelUrl
        this.sourceId = sourceId
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
            novelRepository.toggleBookmark(details)
            checkBookmarkStatus()
        }
    }
}
