package com.example.vantaread.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Statistics", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Chapters Read",
                    value = uiState.totalChaptersRead.toString(),
                    icon = Icons.Default.MenuBook
                )
                
                StatCard(
                    title = "Total Novels Started",
                    value = uiState.totalNovelsStarted.toString(),
                    icon = Icons.Default.Book
                )

                StatCard(
                    title = "Reading Streak",
                    value = "${uiState.readingStreakDays} days",
                    icon = Icons.Default.LocalFireDepartment
                )

                StatCard(
                    title = "Bookmarked Novels",
                    value = uiState.bookmarkedNovels.toString(),
                    icon = Icons.Default.Bookmarks
                )

                StatCard(
                    title = "Downloaded Chapters",
                    value = "${uiState.downloadedChapters} across ${uiState.offlineNovels} novels",
                    icon = Icons.Default.DownloadDone
                )
                
                StatCard(
                    title = "Most Read Novel",
                    value = uiState.mostReadNovelTitle,
                    icon = Icons.Default.Star
                )

                StatCard(
                    title = "Favorite Source",
                    value = uiState.favoriteSourceName,
                    icon = Icons.Default.Source
                )

                StatCard(
                    title = "Last Read",
                    value = uiState.lastReadTitle,
                    icon = Icons.Default.Update
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
