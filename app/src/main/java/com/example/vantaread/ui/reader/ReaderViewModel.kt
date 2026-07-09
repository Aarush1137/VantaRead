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
    private val novelRepository: NovelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterUrl: String = checkNotNull(savedStateHandle["chapterUrl"])
    private val sourceId: String = checkNotNull(savedStateHandle["sourceId"])

    private val _content = MutableStateFlow<String?>(null)
    val content: StateFlow<String?> = _content.asStateFlow()

    init {
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
