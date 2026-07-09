package com.example.vantaread

import androidx.compose.runtime.Composable
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

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                androidx.compose.material3.NavigationBar {
                    androidx.compose.material3.NavigationBarItem(
                        selected = currentRoute is Library,
                        onClick = { 
                            if (currentRoute !is Library) {
                                backStack.clear()
                                backStack.add(Library)
                            }
                        },
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Bookmarks, contentDescription = "Library") },
                        label = { androidx.compose.material3.Text("Library") }
                    )
                    androidx.compose.material3.NavigationBarItem(
                        selected = currentRoute is Discover,
                        onClick = { 
                            if (currentRoute !is Discover) {
                                backStack.clear()
                                backStack.add(Discover)
                            }
                        },
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Explore, contentDescription = "Discover") },
                        label = { androidx.compose.material3.Text("Discover") }
                    )
                    androidx.compose.material3.NavigationBarItem(
                        selected = currentRoute is History,
                        onClick = { 
                            if (currentRoute !is History) {
                                backStack.clear()
                                backStack.add(History)
                            }
                        },
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.History, contentDescription = "History") },
                        label = { androidx.compose.material3.Text("History") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
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
                    // History Screen Placeholder
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.Text("History Screen (Coming Soon)")
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
