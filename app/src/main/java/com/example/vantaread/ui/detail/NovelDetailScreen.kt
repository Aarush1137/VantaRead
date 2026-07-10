package com.example.vantaread.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelUrl: String,
    sourceId: String,
    onNavigateBack: () -> Unit,
    onChapterClick: (chapterUrl: String, sourceId: String, chapterTitle: String) -> Unit,
    viewModel: NovelDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(novelUrl, sourceId) {
        viewModel.initialize(novelUrl, sourceId)
    }

    val details by viewModel.novelDetails.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val readChapterUrls by viewModel.readChapterUrls.collectAsState()
    val lastReadChapterUrl by viewModel.lastReadChapterUrl.collectAsState()
    val downloadMessage by viewModel.downloadMessage.collectAsState()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(downloadMessage) {
        downloadMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDownloadMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        },
        floatingActionButton = {
            if (lastReadChapterUrl != null) {
                FloatingActionButton(
                    onClick = {
                        val lastChapter = chapters.find { it.url == lastReadChapterUrl }
                        if (lastChapter != null) {
                            onChapterClick(lastChapter.url, viewModel.sourceId, lastChapter.title)
                        }
                    },
                    containerColor = Color(android.graphics.Color.parseColor("#8A2BE2")), // Vanta Purple
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Continue Reading")
                }
            }
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
                    val downloadedCount = chapters.count { it.isDownloaded }
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "$downloadedCount of ${chapters.size} chapters downloaded",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        val unreadIndex = chapters.indexOfFirst { !it.isDownloaded }
                                        viewModel.downloadChapters(if (unreadIndex >= 0) unreadIndex else 0, 5)
                                    },
                                    enabled = chapters.any { !it.isDownloaded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Next 5")
                                }
                                Button(
                                    onClick = {
                                        val unreadIndex = chapters.indexOfFirst { !it.isDownloaded }
                                        viewModel.downloadChapters(if (unreadIndex >= 0) unreadIndex else 0, 10)
                                    },
                                    enabled = chapters.any { !it.isDownloaded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Next 10")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.downloadAllChapters() },
                                    enabled = chapters.any { !it.isDownloaded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("All")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { 
                            // Download next 10 unread chapters
                            val unreadIndex = chapters.indexOfFirst { !readChapterUrls.contains(it.url) }
                            val startIndex = if (unreadIndex >= 0) unreadIndex else 0
                            viewModel.downloadChapters(startIndex, 10)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download 10")
                        }
                    }
                }
                
                items(chapters.withIndex().toList()) { (index, chapter) ->
                    ListItem(
                        headlineContent = { Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (chapter.isDownloaded) {
                                    Icon(
                                        Icons.Default.DownloadDone,
                                        contentDescription = "Downloaded",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.downloadChapters(index, 1) }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = LocalContentColor.current.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (readChapterUrls.contains(chapter.url)) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Read",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onChapterClick(chapter.url, viewModel.sourceId, chapter.title) }, 
                    )
                }
            }
        }
    }
}
