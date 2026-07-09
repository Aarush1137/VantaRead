package com.example.vantaread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.vantaread.ui.detail.NovelDetailScreen
import com.example.vantaread.ui.discover.DiscoverScreen
import com.example.vantaread.ui.library.LibraryScreen
import com.example.vantaread.ui.reader.ReaderScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Library)
    val currentRoute = backStack.lastOrNull()
    
    val showBottomBar = currentRoute is Library || currentRoute is Discover || currentRoute is History

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
                        icon = { Icon(Icons.Filled.Explore, contentDescription = "Discover") },
                        label = { Text("Discover") }
                    )
                    NavigationBarItem(
                        selected = currentRoute is History,
                        onClick = { 
                            if (currentRoute !is History) {
                                backStack.clear()
                                backStack.add(History)
                            }
                        },
                        icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                        label = { Text("History") }
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
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) }
                    )
                }
                entry<Discover> {
                    DiscoverScreen(
                        onNavigateBack = { backStack.removeLast() },
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) }
                    )
                }
                entry<History> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("History Screen (Coming Soon)")
                    }
                }
                entry<NovelDetail> { navKey ->
                    NovelDetailScreen(
                        novelUrl = navKey.novelUrl,
                        sourceId = navKey.sourceId,
                        onNavigateBack = { backStack.removeLast() },
                        onChapterClick = { chapterUrl, sourceId -> backStack.add(Reader(chapterUrl, sourceId)) }
                    )
                }
                entry<Reader> { navKey ->
                    ReaderScreen(
                        chapterUrl = navKey.chapterUrl,
                        sourceId = navKey.sourceId,
                        onNavigateBack = { backStack.removeLast() }
                    )
                }
            },
        )
    }
}
