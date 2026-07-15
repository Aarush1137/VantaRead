package com.example.vantaread

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.vantaread.ui.detail.NovelDetailScreen
import com.example.vantaread.ui.discover.DiscoverScreen
import com.example.vantaread.ui.downloads.DownloadsScreen
import com.example.vantaread.ui.history.HistoryScreen
import com.example.vantaread.ui.library.LibraryScreen
import com.example.vantaread.ui.reader.ReaderScreen
import com.example.vantaread.ui.settings.SettingsScreen
import com.example.vantaread.ui.suggestions.SuggestionsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Library)
    val currentRoute = backStack.lastOrNull()
    
    val showBottomBar = currentRoute is Library ||
        currentRoute is Discover ||
        currentRoute is Suggestions ||
        currentRoute is Downloads ||
        currentRoute is History ||
        currentRoute is Settings

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute is Library,
                        onClick = { 
                            if (currentRoute !is Library) {
                                backStack.clear()
                                backStack.add(Library)
                            }
                        },
                        icon = { Icon(Icons.Filled.Bookmarks, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentRoute is Discover,
                        onClick = { 
                            if (currentRoute !is Discover) {
                                backStack.clear()
                                backStack.add(Discover)
                            }
                        },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = currentRoute is Suggestions,
                        onClick = { 
                            if (currentRoute !is Suggestions) {
                                backStack.clear()
                                backStack.add(Suggestions)
                            }
                        },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "Suggestions") },
                        label = { Text("Suggestions") }
                    )
                    NavigationBarItem(
                        selected = currentRoute is Downloads,
                        onClick = {
                            if (currentRoute !is Downloads) {
                                backStack.clear()
                                backStack.add(Downloads)
                            }
                        },
                        icon = { Icon(Icons.Filled.DownloadDone, contentDescription = "Downloads") },
                        label = { Text("Downloads") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Library> {
                    LibraryScreen(
                        onNavigateToDiscover = { backStack.add(Discover) },
                        onNavigateToHistory = { backStack.add(History) },
                        onNavigateToSettings = { backStack.add(Settings) },
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) },
                        onContinueReading = { chapterUrl, sourceId, novelUrl, chapterTitle ->
                            backStack.add(Reader(chapterUrl, sourceId, novelUrl, chapterTitle))
                        }
                    )
                }
                entry<Discover> {
                    DiscoverScreen(
                        onNavigateBack = { backStack.removeLast() },
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) }
                    )
                }
                entry<Suggestions> {
                    SuggestionsScreen(
                        onNavigateBack = { backStack.removeLast() },
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) }
                    )
                }
                entry<Downloads> {
                    DownloadsScreen(
                        onChapterClick = { chapterUrl, sourceId, novelUrl, chapterTitle ->
                            backStack.add(Reader(chapterUrl, sourceId, novelUrl, chapterTitle))
                        }
                    )
                }
                entry<History> {
                    HistoryScreen(
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) },
                        onContinueReading = { chapterUrl, sourceId, novelUrl, chapterTitle ->
                            backStack.add(Reader(chapterUrl, sourceId, novelUrl, chapterTitle))
                        }
                    )
                }
                entry<NovelDetail> { navKey ->
                    NovelDetailScreen(
                        novelUrl = navKey.novelUrl,
                        sourceId = navKey.sourceId,
                        onNavigateBack = { backStack.removeLast() },
                        onChapterClick = { chapterUrl, sourceId, chapterTitle ->
                            backStack.add(Reader(chapterUrl, sourceId, navKey.novelUrl, chapterTitle))
                        }
                    )
                }
                entry<Reader> { navKey ->
                    ReaderScreen(
                        chapterUrl = navKey.chapterUrl,
                        sourceId = navKey.sourceId,
                        novelUrl = navKey.novelUrl,
                        onNavigateBack = { backStack.removeLast() },
                        onNavigateToChapter = { chapterUrl, chapterTitle ->
                            backStack.removeLast()
                            backStack.add(Reader(chapterUrl, navKey.sourceId, navKey.novelUrl, chapterTitle))
                        }
                    )
                }
                entry<Settings> {
                    SettingsScreen()
                }
            },
        )
    }
}
