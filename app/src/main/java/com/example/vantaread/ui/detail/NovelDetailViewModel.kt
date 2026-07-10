package com.example.vantaread.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private val _isLoadingMoreChapters = MutableStateFlow(false)
    val isLoadingMoreChapters: StateFlow<Boolean> = _isLoadingMoreChapters.asStateFlow()

    private val _canLoadMoreChapters = MutableStateFlow(false)
    val canLoadMoreChapters: StateFlow<Boolean> = _canLoadMoreChapters.asStateFlow()

    private var cachedNovelJob: Job? = null
    private var chaptersJob: Job? = null
    private var networkJob: Job? = null
    private var nextChapterPage = 1

    fun initialize(novelUrl: String, sourceId: String) {
        if (this.novelUrl == novelUrl && this.sourceId == sourceId) return
        cachedNovelJob?.cancel()
        chaptersJob?.cancel()
        networkJob?.cancel()

        this.novelUrl = novelUrl
        this.sourceId = sourceId
        
        _novelDetails.value = null
        _chapters.value = emptyList()
        _isBookmarked.value = false
        _readChapterUrls.value = emptySet()
        _lastReadChapterUrl.value = null
        _isLoadingMoreChapters.value = false
        _canLoadMoreChapters.value = sourceId == "lightnovelpub"
        nextChapterPage = 1
        
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
        val activeNovelUrl = novelUrl
        val activeSourceId = sourceId

        cachedNovelJob = viewModelScope.launch {
            novelRepository.getNovelFromDb(novelUrl)?.let { cachedNovel ->
                if (this@NovelDetailViewModel.novelUrl != activeNovelUrl) return@let
                _novelDetails.value = cachedNovel.toDetails()
                _isBookmarked.value = cachedNovel.isBookmarked
            }
        }

        // Start collecting from DB immediately for fast display
        chaptersJob = viewModelScope.launch {
            novelRepository.getChaptersForNovelDb(activeNovelUrl).collect { dbChapters ->
                if (this@NovelDetailViewModel.novelUrl != activeNovelUrl) return@collect
                if (dbChapters.isNotEmpty()) {
                    _chapters.value = dbChapters
                }
                
                val readUrls = novelRepository.getReadChapterUrls(activeNovelUrl)
                _readChapterUrls.value = readUrls.toSet()
                
                val lastRead = novelRepository.getLastReadChapter(activeNovelUrl)
                _lastReadChapterUrl.value = lastRead?.chapterUrl
            }
        }
        
        // Fetch updates from network in background
        networkJob = viewModelScope.launch {
            try {
                // Fetch details from network
                val details = novelRepository.getNovelDetails(activeNovelUrl, activeSourceId)
                if (this@NovelDetailViewModel.novelUrl != activeNovelUrl) return@launch
                _novelDetails.value = details
                
                // Fetch chapters from network and cache
                val fetchedChapters = novelRepository.fetchAndCacheChapters(activeNovelUrl, activeSourceId)
                if (this@NovelDetailViewModel.novelUrl != activeNovelUrl) return@launch
                _canLoadMoreChapters.value = activeSourceId == "lightnovelpub" && fetchedChapters.isNotEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMoreChapters() {
        if (_isLoadingMoreChapters.value || !_canLoadMoreChapters.value) return

        val activeNovelUrl = novelUrl
        val activeSourceId = sourceId
        val pageToLoad = nextChapterPage
        viewModelScope.launch {
            _isLoadingMoreChapters.value = true
            try {
                val fetchedChapters = novelRepository.fetchAndCacheChapterPage(activeNovelUrl, activeSourceId, pageToLoad)
                if (this@NovelDetailViewModel.novelUrl != activeNovelUrl) return@launch
                if (fetchedChapters.isEmpty()) {
                    _canLoadMoreChapters.value = false
                } else {
                    nextChapterPage += 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _canLoadMoreChapters.value = false
            } finally {
                _isLoadingMoreChapters.value = false
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

        val currentNovelTitle = _novelDetails.value?.title ?: "Novel"
        val chaptersToDownload = chapterList
            .drop(startIndex)
            .filter { !it.isDownloaded }
            .take(count)
        
        val workRequests = chaptersToDownload.map { chapter ->
            val data = Data.Builder()
                .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapter.url)
                .putString(ChapterDownloadWorker.KEY_SOURCE_ID, sourceId)
                .putString(ChapterDownloadWorker.KEY_NOVEL_URL, chapter.novelUrl)
                .putString(ChapterDownloadWorker.KEY_NOVEL_TITLE, currentNovelTitle)
                .putString(ChapterDownloadWorker.KEY_CHAPTER_TITLE, chapter.title)
                .build()
                
            OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setInputData(data)
                .addTag(ChapterDownloadWorker.TAG_DOWNLOAD)
                .addTag(ChapterDownloadWorker.tagForNovel(chapter.novelUrl))
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
