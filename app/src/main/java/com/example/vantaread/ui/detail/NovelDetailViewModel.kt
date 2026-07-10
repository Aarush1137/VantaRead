package com.example.vantaread.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data
import com.example.vantaread.worker.ChapterDownloadWorker

@HiltViewModel
class NovelDetailViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val workManager: WorkManager
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

    private val _downloadMessage = MutableStateFlow<String?>(null)
    val downloadMessage: StateFlow<String?> = _downloadMessage.asStateFlow()

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

    private fun NovelEntity.toDetails(): NovelDetails {
        return NovelDetails(
            url = url,
            title = title,
            coverUrl = coverUrl,
            synopsis = synopsis,
            author = author,
            genres = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            status = status,
            latestUpdate = latestUpdate
        )
    }

    private fun loadNovelDetails() {
        viewModelScope.launch {
            novelRepository.getNovelFromDb(novelUrl)?.let { cachedNovel ->
                _novelDetails.value = cachedNovel.toDetails()
                _isBookmarked.value = cachedNovel.isBookmarked
            }
        }

        // Start collecting from DB immediately for fast display
        viewModelScope.launch {
            novelRepository.getChaptersForNovelDb(novelUrl).collect { dbChapters ->
                if (dbChapters.isNotEmpty()) {
                    _chapters.value = dbChapters
                }
                
                val readUrls = novelRepository.getReadChapterUrls(novelUrl)
                _readChapterUrls.value = readUrls.toSet()
                
                val lastRead = novelRepository.getLastReadChapter(novelUrl)
                _lastReadChapterUrl.value = lastRead?.chapterUrl
            }
        }
        
        // Fetch updates from network in background
        viewModelScope.launch {
            try {
                // Fetch details from network
                val details = novelRepository.getNovelDetails(novelUrl, sourceId)
                _novelDetails.value = details
                
                // Fetch chapters from network and cache
                novelRepository.fetchAndCacheChapters(novelUrl, sourceId)
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

    fun downloadChapters(startIndex: Int, count: Int) {
        val chapterList = _chapters.value
        if (chapterList.isEmpty()) return

        val chaptersToDownload = chapterList
            .drop(startIndex)
            .filter { !it.isDownloaded }
            .take(count)
        
        val workRequests = chaptersToDownload.map { chapter ->
            val data = Data.Builder()
                .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapter.url)
                .putString(ChapterDownloadWorker.KEY_SOURCE_ID, sourceId)
                .build()
                
            OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setInputData(data)
                .build()
        }
        
        if (workRequests.isNotEmpty()) {
            workManager.enqueue(workRequests)
            _downloadMessage.value = "Queued ${workRequests.size} chapter download(s)."
        } else {
            _downloadMessage.value = "Selected chapters are already downloaded."
        }
    }

    fun downloadAllChapters() {
        downloadChapters(0, Int.MAX_VALUE)
    }

    fun clearDownloadMessage() {
        _downloadMessage.value = null
    }
}
