package com.example.vantaread.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _content = MutableStateFlow<String?>(null)
    val content: StateFlow<String?> = _content.asStateFlow()

    fun initialize(chapterUrl: String, sourceId: String) {
        if (this.chapterUrl.isNotEmpty()) return
        this.chapterUrl = chapterUrl
        this.sourceId = sourceId
        loadChapter()
    }

    private fun loadChapter() {
        viewModelScope.launch {
            try {
                // Fetch from network for now
                val htmlContent = novelRepository.getChapterContent(chapterUrl, sourceId)
                _content.value = htmlContent
            } catch (e: Exception) {
                e.printStackTrace()
                _content.value = "Failed to load chapter content."
            }
        }
    }
}
