package com.example.vantaread.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    onNavigateBack: () -> Unit,
    onChapterClick: (String, String) -> Unit, // chapterUrl, sourceId
    viewModel: NovelDetailViewModel = hiltViewModel()
) {
    val details by viewModel.novelDetails.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (details == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        AsyncImage(
                            model = details!!.coverUrl,
                            contentDescription = details!!.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(0.66f)
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(text = details!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Author: ${details!!.author}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Status: ${details!!.status}", style = MaterialTheme.typography.bodyMedium)
                            
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                details!!.genres.take(3).forEach { genre ->
                                    AssistChip(onClick = {}, label = { Text(genre) })
                                }
                            }
                        }
                    }
                    Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = details!!.synopsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                    
                    Text(text = "Chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }
                
                items(chapters) { chapter ->
                    ListItem(
                        headlineContent = { Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.clickable { onChapterClick(chapter.url, viewModel.sourceId) }, 
                    )
                }
            }
        }
    }
}
