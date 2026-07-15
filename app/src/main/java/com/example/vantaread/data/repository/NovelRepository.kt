package com.example.vantaread.data.repository

import com.example.vantaread.data.db.BookmarkDao
import com.example.vantaread.data.db.BookmarkEntity
import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.db.DownloadedChapter
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.db.ReadingHistoryDao
import com.example.vantaread.data.db.ReadingHistoryEntity
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.SourceCatalog
import com.example.vantaread.data.util.VantaStorageManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SuggestionFetchResult(
    val novels: List<Novel>,
    val successfulSourceIds: List<String>,
    val failedSourceIds: List<String>
)

class NovelRepository @Inject constructor(
    private val sources: Map<String, @JvmSuppressWildcards NovelSource>,
    private val novelDao: NovelDao,
    private val readingHistoryDao: ReadingHistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val storageManager: VantaStorageManager
) {
    private data class CachedSuggestionSource(
        val novels: List<Novel>,
        val fetchedAtMillis: Long
    )

    private val suggestionCache = mutableMapOf<String, CachedSuggestionSource>()
    private val suggestionCacheTtlMillis = 10L * 60L * 1000L
    private val suggestionSourceTimeoutMillis = 9000L
    private val sourceOperationTimeoutMillis = 15000L

    private fun getSource(sourceId: String): NovelSource {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        return sources[normalizedSourceId] ?: throw IllegalArgumentException("Source $normalizedSourceId not found")
    }

    // --- Network Calls ---
    
    suspend fun searchNovels(query: String, sourceId: String): List<Novel> {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        val primaryResults = withTimeoutOrNull(sourceOperationTimeoutMillis) {
            runCatching {
                getSource(normalizedSourceId).searchNovels(query).map { it.withSource(normalizedSourceId) }
            }.getOrElse { emptyList() }
        }.orEmpty()
        if (primaryResults.isNotEmpty() || normalizedSourceId == SourceCatalog.DEFAULT_SOURCE_ID) {
            return primaryResults
        }

        return withTimeoutOrNull(sourceOperationTimeoutMillis) {
            runCatching {
                getSource(SourceCatalog.DEFAULT_SOURCE_ID).searchNovels(query)
                    .map { it.withSource(SourceCatalog.DEFAULT_SOURCE_ID) }
            }.getOrElse { emptyList() }
        }.orEmpty()
    }

    suspend fun getPopularNovels(sourceId: String): List<Novel> {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        val primaryResults = withTimeoutOrNull(sourceOperationTimeoutMillis) {
            runCatching {
                getSource(normalizedSourceId).getPopularNovels().map { it.withSource(normalizedSourceId) }
            }.getOrElse { emptyList() }
        }.orEmpty()
        if (primaryResults.isNotEmpty() || normalizedSourceId == SourceCatalog.DEFAULT_SOURCE_ID) {
            return primaryResults
        }

        return withTimeoutOrNull(sourceOperationTimeoutMillis) {
            runCatching {
                getSource(SourceCatalog.DEFAULT_SOURCE_ID).getPopularNovels()
                    .map { it.withSource(SourceCatalog.DEFAULT_SOURCE_ID) }
            }.getOrElse { emptyList() }
        }.orEmpty()
    }

    suspend fun getSuggestedNovels(
        sourceIds: List<String>,
        forceRefresh: Boolean = false
    ): SuggestionFetchResult = supervisorScope {
        val normalizedSourceIds = sourceIds
            .map { SourceCatalog.normalize(it) }
            .distinct()

        val results = normalizedSourceIds.map { sourceId ->
            async {
                val novels = getSuggestedNovelsForSource(sourceId, forceRefresh)
                sourceId to novels
            }
        }.awaitAll()

        val successful = results.filter { it.second.isNotEmpty() }
        val dedupedNovels = successful
            .flatMap { it.second }
            .distinctBy { it.url }
            .shuffled()

        SuggestionFetchResult(
            novels = dedupedNovels,
            successfulSourceIds = successful.map { it.first },
            failedSourceIds = results.filter { it.second.isEmpty() }.map { it.first }
        )
    }

    private suspend fun getSuggestedNovelsForSource(sourceId: String, forceRefresh: Boolean): List<Novel> {
        val now = System.currentTimeMillis()
        val cached = suggestionCache[sourceId]
        if (!forceRefresh && cached != null && now - cached.fetchedAtMillis < suggestionCacheTtlMillis) {
            return cached.novels
        }

        val fresh = withTimeoutOrNull(suggestionSourceTimeoutMillis) {
            runCatching {
                getSource(sourceId).getPopularNovels()
                    .map { it.withSource(sourceId) }
                    .filter { it.title.isNotBlank() && it.url.isNotBlank() }
                    .distinctBy { it.url }
            }.getOrDefault(emptyList())
        }.orEmpty()

        if (fresh.isNotEmpty()) {
            suggestionCache[sourceId] = CachedSuggestionSource(fresh, now)
            return fresh
        }

        return cached?.novels.orEmpty()
    }

    private fun Novel.withSource(sourceId: String): Novel {
        return if (this.sourceId == sourceId) this else copy(sourceId = sourceId)
    }

    suspend fun getNovelDetails(novelUrl: String, sourceId: String): NovelDetails {
        val resolvedSourceId = SourceCatalog.detectSourceId(novelUrl) ?: SourceCatalog.normalize(sourceId)
        val details = withTimeoutOrNull(sourceOperationTimeoutMillis) {
            getSource(resolvedSourceId).getNovelDetails(novelUrl)
        } ?: throw IllegalStateException("Timed out loading novel details from ${SourceCatalog.nameFor(resolvedSourceId)}.")
        
        // Try to cache cover if available
        if (details.coverUrl.isNotBlank()) {
            try {
                // We'd need an image loader or simple HTTP client to get bytes here.
                // For now, we'll stick to content caching and maybe add cover caching later 
                // if we add a network client to the repository.
            } catch (_: Exception) {}
        }

        val existing = novelDao.getNovel(novelUrl)
        novelDao.insertNovel(
            NovelEntity(
                url = novelUrl,
                title = details.title,
                coverUrl = details.coverUrl,
                synopsis = details.synopsis,
                author = details.author,
                genres = details.genres.joinToString(","),
                status = details.status,
                latestUpdate = details.latestUpdate,
                isBookmarked = existing?.isBookmarked ?: false,
                currentChapterUrl = existing?.currentChapterUrl,
                currentScrollPosition = existing?.currentScrollPosition ?: 0,
                sourceId = resolvedSourceId
            )
        )
        
        return details
    }

    suspend fun fetchAndCacheChapters(novelUrl: String, sourceId: String): List<Chapter> {
        val normalizedSourceId = SourceCatalog.detectSourceId(novelUrl) ?: SourceCatalog.normalize(sourceId)
        val chapters = withTimeoutOrNull(sourceOperationTimeoutMillis) {
            getSource(normalizedSourceId).getChapterList(novelUrl)
        }.orEmpty()
        novelDao.insertChapters(chapters.map {
            ChapterEntity(
                url = it.url,
                novelUrl = novelUrl, // Enforce exact match to prevent FK constraint failures
                title = it.title,
                chapterIndex = it.index,
                isDownloaded = false,
                content = null
            )
        })
        return chapters
    }

    suspend fun fetchAndCacheChapterPage(novelUrl: String, sourceId: String, page: Int): List<Chapter> {
        val normalizedSourceId = SourceCatalog.detectSourceId(novelUrl) ?: SourceCatalog.normalize(sourceId)
        val chapters = withTimeoutOrNull(sourceOperationTimeoutMillis) {
            getSource(normalizedSourceId).getChapterPage(novelUrl, page)
        }.orEmpty()
        if (chapters.isNotEmpty()) {
            novelDao.insertChapters(chapters.map {
                ChapterEntity(
                    url = it.url,
                    novelUrl = novelUrl,
                    title = it.title,
                    chapterIndex = it.index,
                    isDownloaded = false,
                    content = null
                )
            })
        }
        return chapters
    }

    suspend fun getChapterContent(chapterUrl: String, sourceId: String): String {
        val resolvedSourceId = SourceCatalog.detectSourceId(chapterUrl) ?: SourceCatalog.normalize(sourceId)
        val chapterEntity = novelDao.getChapter(chapterUrl)
        
        // Try file storage first
        val storedContent = storageManager.loadChapter(chapterEntity?.novelUrl ?: "", chapterUrl)
        if (!storedContent.isNullOrBlank()) {
            return storedContent
        }

        // Fallback to DB (Legacy or downloaded before path change)
        if (chapterEntity?.isDownloaded == true && !chapterEntity.content.isNullOrBlank()) {
            // Auto-migrate this chapter to file storage
            storageManager.saveChapter(chapterEntity.novelUrl, chapterUrl, chapterEntity.content)
            return chapterEntity.content
        }

        return try {
            withTimeoutOrNull(sourceOperationTimeoutMillis) {
                val content = getSource(resolvedSourceId).getChapterContent(chapterUrl)
                // If it's a regular read (not download), we don't necessarily save to disk 
                // unless we want to cache everything. Let's cache it if it's long enough.
                if (content.length > 1000 && !chapterUrl.contains("error")) {
                    storageManager.saveChapter(chapterEntity?.novelUrl ?: "", chapterUrl, content)
                }
                content
            } ?: "Timed out loading chapter content from ${SourceCatalog.nameFor(resolvedSourceId)}."
        } catch (e: Exception) {
            "Error loading chapter content: ${e.message}"
        }
    }

    suspend fun prefetchChapter(chapterUrl: String, sourceId: String) {
        val resolvedSourceId = SourceCatalog.detectSourceId(chapterUrl) ?: SourceCatalog.normalize(sourceId)
        val chapterEntity = novelDao.getChapter(chapterUrl)
        
        // Check if already in storage
        val storedContent = storageManager.loadChapter(chapterEntity?.novelUrl ?: "", chapterUrl)
        if (!storedContent.isNullOrBlank()) return

        // Check if already in DB
        if (chapterEntity != null && !chapterEntity.content.isNullOrBlank()) {
            storageManager.saveChapter(chapterEntity.novelUrl, chapterUrl, chapterEntity.content)
            return
        }

        try {
            withTimeoutOrNull(sourceOperationTimeoutMillis) {
                val content = getSource(resolvedSourceId).getChapterContent(chapterUrl)
                if (content.length > 500) { // Basic sanity check
                    storageManager.saveChapter(chapterEntity?.novelUrl ?: "", chapterUrl, content)
                    
                    // Mark as downloaded in DB if it was a prefetch of a known chapter
                    if (chapterEntity != null) {
                        novelDao.insertChapter(chapterEntity.copy(isDownloaded = true))
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fail for prefetch
        }
    }

    fun setStorageUri(uri: String?) {
        storageManager.setBaseUri(uri)
    }

    // --- Local DB Calls ---

    fun getBookmarkedNovels(): Flow<List<NovelEntity>> {
        return novelDao.getBookmarkedNovels()
    }
    
    suspend fun getNovelFromDb(novelUrl: String): NovelEntity? {
        return novelDao.getNovel(novelUrl)
    }

    suspend fun toggleBookmark(novelDetails: NovelDetails, sourceId: String) {
        val existing = novelDao.getNovel(novelDetails.url)
        if (existing != null) {
            novelDao.updateBookmarkStatus(novelDetails.url, !existing.isBookmarked)
        } else {
            novelDao.insertNovel(
                NovelEntity(
                    url = novelDetails.url,
                    title = novelDetails.title,
                    coverUrl = novelDetails.coverUrl,
                    synopsis = novelDetails.synopsis,
                    author = novelDetails.author,
                    genres = novelDetails.genres.joinToString(","),
                    status = novelDetails.status,
                    latestUpdate = novelDetails.latestUpdate,
                    isBookmarked = true,
                    sourceId = SourceCatalog.normalize(sourceId)
                )
            )
        }
    }

    suspend fun updateReadingProgress(novelUrl: String, chapterUrl: String, scrollPos: Int) {
        novelDao.updateReadingProgress(novelUrl, chapterUrl, scrollPos)
    }

    fun getChaptersForNovelDb(novelUrl: String): Flow<List<ChapterEntity>> {
        return novelDao.getChaptersForNovel(novelUrl)
    }
    
    suspend fun getChaptersListForNovelDb(novelUrl: String): List<ChapterEntity> {
        return novelDao.getChaptersListForNovel(novelUrl)
    }

    fun getDownloadedChapters(): Flow<List<DownloadedChapter>> {
        return novelDao.getDownloadedChapters()
    }

    suspend fun removeDownloadedChapter(chapterUrl: String) {
        novelDao.removeDownloadedChapter(chapterUrl)
    }

    suspend fun removeAllDownloadedChapters() {
        novelDao.removeAllDownloadedChapters()
    }

    // --- Reading History ---

    fun getReadingHistory(): Flow<List<ReadingHistoryEntity>> {
        return readingHistoryDao.getAllHistory()
    }

    fun getRecentNovels(): Flow<List<ReadingHistoryEntity>> {
        return readingHistoryDao.getRecentNovels()
    }

    suspend fun recordChapterRead(
        chapterUrl: String,
        novelUrl: String,
        novelTitle: String,
        chapterTitle: String,
        coverUrl: String,
        sourceId: String,
        scrollPosition: Int = 0,
        maxScrollPosition: Int = 0
    ) {
        readingHistoryDao.upsert(
            ReadingHistoryEntity(
                chapterUrl = chapterUrl,
                novelUrl = novelUrl,
                novelTitle = novelTitle,
                chapterTitle = chapterTitle,
                coverUrl = coverUrl,
                sourceId = sourceId,
                lastReadTimestamp = System.currentTimeMillis(),
                scrollPosition = scrollPosition,
                maxScrollPosition = maxScrollPosition
            )
        )
    }

    suspend fun updateHistoryScrollPosition(chapterUrl: String, scrollPosition: Int, maxScrollPosition: Int) {
        val existing = readingHistoryDao.getHistoryEntry(chapterUrl)
        if (existing != null) {
            readingHistoryDao.upsert(
                existing.copy(
                    scrollPosition = scrollPosition,
                    maxScrollPosition = maxScrollPosition,
                    lastReadTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getLastReadChapter(novelUrl: String): ReadingHistoryEntity? {
        return readingHistoryDao.getLastReadChapter(novelUrl)
    }

    suspend fun getReadChapterUrls(novelUrl: String): List<String> {
        return readingHistoryDao.getReadChapterUrls(novelUrl)
    }

    suspend fun getHistoryEntry(chapterUrl: String): ReadingHistoryEntity? {
        return readingHistoryDao.getHistoryEntry(chapterUrl)
    }

    suspend fun deleteHistoryEntry(chapterUrl: String) {
        readingHistoryDao.deleteEntry(chapterUrl)
    }

    suspend fun clearHistory() {
        readingHistoryDao.clearAll()
    }

    // --- Bookmarks ---

    fun getBookmarksForChapter(chapterUrl: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForChapter(chapterUrl)
    }

    fun getBookmarksForNovel(novelUrl: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForNovel(novelUrl)
    }

    suspend fun addBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(bookmarkId: Long) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }
}
