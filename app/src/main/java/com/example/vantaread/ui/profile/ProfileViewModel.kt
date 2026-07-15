package com.example.vantaread.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.db.DownloadedChapter
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.db.ReadingHistoryEntity
import com.example.vantaread.data.repository.AuthRepository
import com.example.vantaread.data.repository.CloudSyncRepository
import com.example.vantaread.data.repository.NovelRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val bookmarkCount: Int = 0,
    val novelsStarted: Int = 0,
    val chaptersRead: Int = 0,
    val downloadedChapters: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    novelRepository: NovelRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        novelRepository.getBookmarkedNovels(),
        novelRepository.getReadingHistory(),
        novelRepository.getDownloadedChapters()
    ) { user: FirebaseUser?, bookmarks: List<NovelEntity>, history: List<ReadingHistoryEntity>, downloads: List<DownloadedChapter> ->
        ProfileUiState(
            user = user,
            bookmarkCount = bookmarks.size,
            novelsStarted = history.distinctBy { it.novelUrl }.size,
            chaptersRead = history.size,
            downloadedChapters = downloads.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun signOut() {
        authRepository.signOut()
        _message.value = "Signed out"
    }

    fun syncBookmarks() {
        viewModelScope.launch {
            _message.value = "Syncing bookmarks..."
            runCatching { cloudSyncRepository.syncBookmarksToCloud() }
                .onSuccess { _message.value = "Bookmarks synced" }
                .onFailure { _message.value = it.message ?: "Bookmark sync failed" }
        }
    }

    fun restoreBookmarks() {
        viewModelScope.launch {
            _message.value = "Restoring bookmarks..."
            runCatching { cloudSyncRepository.syncBookmarksFromCloud() }
                .onSuccess { _message.value = "Bookmarks restored" }
                .onFailure { _message.value = it.message ?: "Bookmark restore failed" }
        }
    }
}
