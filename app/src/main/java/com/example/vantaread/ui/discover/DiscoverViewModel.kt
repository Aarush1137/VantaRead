package com.example.vantaread.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.vantaread.data.prefs.SourcePreferencesManager
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val sourcePrefs: SourcePreferencesManager
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Novel>>(emptyList())
    val searchResults: StateFlow<List<Novel>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val activeSourceId: StateFlow<String> = sourcePrefs.activeSourceId

    fun setActiveSource(sourceId: String) {
        sourcePrefs.setActiveSource(sourceId)
        _searchResults.value = emptyList() // clear results on source change
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentSourceId = sourcePrefs.activeSourceId.value
                val results = novelRepository.searchNovels(query, currentSourceId)
                android.util.Log.d("DiscoverViewModel", "Search results for '$query': ${results.size}")
                _searchResults.value = results
            } catch (e: Exception) {
                android.util.Log.e("DiscoverViewModel", "Search error", e)
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
