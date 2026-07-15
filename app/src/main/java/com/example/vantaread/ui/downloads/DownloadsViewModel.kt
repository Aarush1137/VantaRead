package com.example.vantaread.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.DownloadedChapter
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.worker.ChapterDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val downloadedChapters: StateFlow<List<DownloadedChapter>> = novelRepository.getDownloadedChapters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeDownloads = MutableStateFlow<List<DownloadWorkStatus>>(emptyList())
    val activeDownloads: StateFlow<List<DownloadWorkStatus>> = _activeDownloads.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            workManager.getWorkInfosByTagFlow(ChapterDownloadWorker.TAG_DOWNLOAD)
                .collect { workInfos ->
                    val active = workInfos
                        .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
                        .map { workInfo ->
                            DownloadWorkStatus(
                                id = workInfo.id.toString(),
                                novelTitle = workInfo.outputData.getString(ChapterDownloadWorker.KEY_NOVEL_TITLE)
                                    ?: workInfo.progress.getString(ChapterDownloadWorker.KEY_NOVEL_TITLE)
                                    ?: "Novel",
                                chapterTitle = workInfo.outputData.getString(ChapterDownloadWorker.KEY_CHAPTER_TITLE)
                                    ?: workInfo.progress.getString(ChapterDownloadWorker.KEY_CHAPTER_TITLE)
                                    ?: "Chapter",
                                state = workInfo.progress.getString(ChapterDownloadWorker.KEY_DOWNLOAD_STATE)
                                    ?: if (workInfo.state == WorkInfo.State.RUNNING) "Downloading" else "Queued",
                                progressText = workInfo.progress.getString(ChapterDownloadWorker.KEY_DOWNLOAD_PROGRESS_TEXT)
                                    ?: ""
                            )
                        }
                    _activeDownloads.value = active
                }
        }
    }



    fun removeDownload(chapterUrl: String) {
        viewModelScope.launch {
            novelRepository.removeDownloadedChapter(chapterUrl)
        }
    }
}
