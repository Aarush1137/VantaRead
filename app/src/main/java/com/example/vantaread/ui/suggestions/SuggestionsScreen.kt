package com.example.vantaread.ui.suggestions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.vantaread.ui.library.LibraryViewModel
import com.example.vantaread.ui.library.NovelItemUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    onNavigateBack: () -> Unit,
    onNovelClick: (String, String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val popularNovels by viewModel.popularNovels.collectAsState()
    val isLoading by viewModel.isLoadingPopularNovels.collectAsState()
    val activeSourceId by viewModel.activeSourceId.collectAsState()
    val activeSourceName = SourceCatalog.nameFor(activeSourceId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggestions") }
            )
        }
    ) { padding ->
        if (isLoading && popularNovels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (popularNovels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No suggestions available from $activeSourceName right now.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(popularNovels) { novel ->
                    NovelItemUi(
                        title = novel.title,
                        coverUrl = novel.coverUrl,
                        onClick = { onNovelClick(novel.url, activeSourceId) }
                    )
                }
            }
        }
    }
}
