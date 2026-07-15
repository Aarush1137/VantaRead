package com.example.vantaread.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.model.ReaderSettings
import com.example.vantaread.data.prefs.ReaderPreferencesManager
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val novelRepository: NovelRepository,
    private val preferencesManager: ReaderPreferencesManager
) : ViewModel(), TextToSpeech.OnInitListener {

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

    val settings: StateFlow<ReaderSettings> = preferencesManager.settings

    private var saveScrollJob: Job? = null

    private var tts: TextToSpeech? = null
    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying.asStateFlow()

    private val _ttsHighlightIndex = MutableStateFlow(-1)
    val ttsHighlightIndex: StateFlow<Int> = _ttsHighlightIndex.asStateFlow()

    private var ttsParagraphs = listOf<String>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.toIntOrNull()?.let { index ->
                        _ttsHighlightIndex.value = index
                    }
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.toIntOrNull()?.let { index ->
                        if (index == ttsParagraphs.size - 1) {
                            _isTtsPlaying.value = false
                            _ttsHighlightIndex.value = -1
                        } else if (_isTtsPlaying.value) {
                            speakParagraph(index + 1)
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })
        }
    }

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

    fun updateSettings(newSettings: ReaderSettings) {
        preferencesManager.updateSettings(newSettings)
    }

    fun startTts(paragraphs: List<String>, startIndex: Int = 0) {
        if (paragraphs.isEmpty()) return
        ttsParagraphs = paragraphs.map { androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString() }
        _isTtsPlaying.value = true
        speakParagraph(startIndex)
    }

    fun pauseTts() {
        _isTtsPlaying.value = false
        tts?.stop()
    }

    fun stopTts() {
        _isTtsPlaying.value = false
        _ttsHighlightIndex.value = -1
        tts?.stop()
    }

    private fun speakParagraph(index: Int) {
        if (index in ttsParagraphs.indices && _isTtsPlaying.value) {
            val text = ttsParagraphs[index]
            if (text.isNotBlank()) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, index.toString())
            } else {
                // Skip empty paragraphs immediately
                speakParagraph(index + 1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
