package com.example.vantaread

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.vantaread.ui.auth.AuthScreen
import com.example.vantaread.ui.auth.AuthViewModel
import com.example.vantaread.ui.detail.NovelDetailScreen
import com.example.vantaread.ui.discover.DiscoverScreen
import com.example.vantaread.ui.downloads.DownloadsScreen
import com.example.vantaread.ui.history.HistoryScreen
import com.example.vantaread.ui.library.LibraryScreen
import com.example.vantaread.ui.reader.ReaderScreen
import com.example.vantaread.ui.profile.ProfileScreen
import com.example.vantaread.ui.settings.SettingsScreen
import com.example.vantaread.ui.stats.StatsScreen
import com.example.vantaread.ui.suggestions.SuggestionsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val activity = LocalContext.current as? Activity
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    var continuedAsGuest by rememberSaveable { mutableStateOf(false) }
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }
    val backStack = rememberNavBackStack(if (currentUser == null) Auth else Library)
    val currentRoute = backStack.lastOrNull()

    LaunchedEffect(currentUser, continuedAsGuest) {
        if (currentUser == null && !continuedAsGuest && currentRoute !is Auth) {
            backStack.clear()
            backStack.add(Auth)
        }
    }
    
    val showBottomBar = currentRoute is Library ||
        currentRoute is Discover ||
        currentRoute is Suggestions ||
        currentRoute is Downloads ||
        currentRoute is History ||
        currentRoute is Stats ||
        currentRoute is Profile ||
        currentRoute is Settings
    val isMoreDestination = currentRoute is History ||
        currentRoute is Stats ||
        currentRoute is Profile ||
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
                    NavigationBarItem(
                        selected = showMoreSheet || isMoreDestination,
                        onClick = {
                            showMoreSheet = true
                        },
                        icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                } else {
                    activity?.finish()
                }
            },
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            popTransitionSpec = {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            },
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
                        onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) },
                        onNovelClick = { url, sourceId -> backStack.add(NovelDetail(url, sourceId)) }
                    )
                }
                entry<Auth> {
                    AuthScreen(
                        onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) },
                        onAuthenticated = {
                            continuedAsGuest = false
                            backStack.clear()
                            backStack.add(Library)
                        },
                        onContinueAsGuest = {
                            continuedAsGuest = true
                            backStack.clear()
                            backStack.add(Library)
                        }
                    )
                }
                entry<Suggestions> {
                    SuggestionsScreen(
                        onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) },
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
                        onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) },
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
                        onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) },
                        onNavigateToNovel = { novelUrl ->
                            if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
                            backStack.add(NovelDetail(novelUrl, navKey.sourceId))
                        },
                        onNavigateToChapter = { chapterUrl, chapterTitle ->
                            if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
                            backStack.add(Reader(chapterUrl, navKey.sourceId, navKey.novelUrl, chapterTitle))
                        }
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        onNavigateToAuth = { backStack.add(Auth) },
                        onNavigateToProfile = {
                            backStack.clear()
                            backStack.add(Profile)
                        }
                    )
                }
                entry<Stats> {
                    StatsScreen()
                }
                entry<Profile> {
                    ProfileScreen(
                        onNavigateToAuth = { backStack.add(Auth) },
                        onNavigateToStats = {
                            backStack.clear()
                            backStack.add(Stats)
                        },
                        onNavigateToSettings = { backStack.add(Settings) },
                        onSignedOut = {
                            continuedAsGuest = false
                            backStack.clear()
                            backStack.add(Auth)
                        }
                    )
                }
            },
        )
    }

    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            Text(
                text = "More",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Reading history") },
                leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMoreSheet = false
                    if (currentRoute !is History) {
                        backStack.clear()
                        backStack.add(History)
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Reading statistics") },
                leadingContent = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMoreSheet = false
                    if (currentRoute !is Stats) {
                        backStack.clear()
                        backStack.add(Stats)
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Profile & cloud sync") },
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMoreSheet = false
                    if (currentRoute !is Profile) {
                        backStack.clear()
                        backStack.add(Profile)
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Settings") },
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMoreSheet = false
                    if (currentRoute !is Settings) {
                        backStack.clear()
                        backStack.add(Settings)
                    }
                }
            )
        }
    }
}
