package com.example.vantaread.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.DownloadedChapter
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {

    val downloadedChapters: StateFlow<List<DownloadedChapter>> = novelRepository.getDownloadedChapters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeDownload(chapterUrl: String) {
        viewModelScope.launch {
            novelRepository.removeDownloadedChapter(chapterUrl)
        }
    }
}
