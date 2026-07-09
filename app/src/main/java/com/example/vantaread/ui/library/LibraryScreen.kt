package com.example.vantaread.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vantaread.data.db.NovelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDiscover: () -> Unit,
    onNovelClick: (String, String) -> Unit, // novelUrl, sourceId
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val novels by viewModel.savedNovels.collectAsState()
    val popularNovels by viewModel.popularNovels.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VantaRead Library") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToDiscover) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (novels.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Your library is empty. Here are some suggestions:")
                }
                
                if (popularNovels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(popularNovels) { novel ->
                            NovelItemUi(
                                title = novel.title,
                                coverUrl = novel.coverUrl,
                                onClick = { onNovelClick(novel.url, "wtr-lab") }
                            )
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(novels) { novel ->
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

@Composable
fun NovelItemUi(title: String, coverUrl: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.66f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

