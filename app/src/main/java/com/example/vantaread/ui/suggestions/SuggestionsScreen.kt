package com.example.vantaread.ui.suggestions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vantaread.data.source.SourceCatalog
import com.example.vantaread.ui.library.NovelItemUi

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    onNavigateBack: () -> Unit,
    onNovelClick: (String, String) -> Unit,
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Suggestions") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.novels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SuggestionSourceChips(
                            selectedSourceId = uiState.selectedSourceId,
                            onSourceSelected = viewModel::selectSource,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    uiState.errorMessage?.let { message ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = message,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                    if (uiState.novels.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No suggestions available right now.")
                            }
                        }
                    }
                    items(uiState.novels) { novel ->
                        NovelItemUi(
                            title = novel.title,
                            coverUrl = novel.coverUrl,
                            onClick = { onNovelClick(novel.url, novel.sourceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSourceChips(
    selectedSourceId: String,
    onSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { onSourceSelected("all") },
            label = { Text(if (selectedSourceId == "all") "All sources" else "All") },
            enabled = selectedSourceId != "all"
        )
        SourceCatalog.sources.forEach { source ->
            AssistChip(
                onClick = { onSourceSelected(source.id) },
                label = { Text(source.name) },
                enabled = selectedSourceId != source.id
            )
        }
    }
}
