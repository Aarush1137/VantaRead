package com.example.vantaread.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vantaread.data.db.ReadingHistoryEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.MoreVert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDiscover: () -> Unit,
    onNovelClick: (String, String) -> Unit, // novelUrl, sourceId
    onContinueReading: (String, String, String, String) -> Unit, // chapterUrl, sourceId, novelUrl, chapterTitle
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val novels by viewModel.savedNovels.collectAsState()
    val popularNovels by viewModel.popularNovels.collectAsState()
    val recentReads by viewModel.recentReads.collectAsState()
    val isLoadingPopularNovels by viewModel.isLoadingPopularNovels.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VantaRead Library") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    var showSortMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    var showSourceMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    val currentSort by viewModel.currentSortOption.collectAsState()
                    val activeSourceId by viewModel.activeSourceId.collectAsState()
                    
                    val sources = mapOf(
                        "novelfull" to "NovelFull",
                        "wtr-lab" to "WTR Lab",
                        "royalroad" to "Royal Road",
                        "lightnovelpub" to "LightNovelPub"
                    )

                    IconButton(onClick = { showSourceMenu = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                            contentDescription = "Select Source"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false }
                    ) {
                        sources.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.setActiveSource(id)
                                    showSourceMenu = false
                                },
                                trailingIcon = if (activeSourceId == id) {
                                    { Text("✓") }
                                } else null
                            )
                        }
                    }

                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option.displayName, 
                                        fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal,
                                        color = if (option == currentSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToDiscover) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (recentReads.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Continue Reading",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentReads) { entry ->
                            RecentReadItem(
                                entry = entry,
                                onClick = { onContinueReading(entry.chapterUrl, entry.sourceId, entry.novelUrl, entry.chapterTitle) }
                            )
                        }
                    }
                }
            }

            if (novels.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "My Bookmarks",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
                gridItems(novels) { novel ->
                    NovelItemUi(
                        title = novel.title,
                        coverUrl = novel.coverUrl,
                        onClick = { onNovelClick(novel.url, novel.sourceId) }
                    )
                }
            } else if (!isLoadingPopularNovels && recentReads.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Your library is empty.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentReadItem(entry: ReadingHistoryEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.coverUrl,
                contentDescription = entry.novelTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.novelTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.chapterTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Progress
                LinearProgressIndicator(
                    progress = { if (entry.maxScrollPosition > 0) entry.scrollPosition.toFloat() / entry.maxScrollPosition else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(android.graphics.Color.parseColor("#8A2BE2")), // Vanta Purple
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
