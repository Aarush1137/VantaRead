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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
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
            entry<NovelDetail> { navKey ->
                NovelDetailScreen(
                    onNavigateBack = { backStack.removeLast() },
                    onChapterClick = { chapterUrl, sourceId -> backStack.add(Reader(chapterUrl, sourceId)) }
                )
            }
            entry<Reader> { navKey ->
                ReaderScreen(
                    onNavigateBack = { backStack.removeLast() }
                )
            }
        },
    )
}
