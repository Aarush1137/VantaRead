package com.example.vantaread.data.repository

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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NovelRepository @Inject constructor(
    private val sources: Map<String, @JvmSuppressWildcards NovelSource>,
    private val novelDao: NovelDao,
    private val readingHistoryDao: ReadingHistoryDao
) {

    private fun getSource(sourceId: String): NovelSource {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        return sources[normalizedSourceId] ?: throw IllegalArgumentException("Source $normalizedSourceId not found")
    }

    // --- Network Calls ---
    
    suspend fun searchNovels(query: String, sourceId: String): List<Novel> {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        val primaryResults = runCatching {
            getSource(normalizedSourceId).searchNovels(query).map { it.withSource(normalizedSourceId) }
        }.getOrElse { emptyList() }
        if (primaryResults.isNotEmpty() || normalizedSourceId == SourceCatalog.DEFAULT_SOURCE_ID) {
            return primaryResults
        }

        return runCatching {
            getSource(SourceCatalog.DEFAULT_SOURCE_ID).searchNovels(query)
                .map { it.withSource(SourceCatalog.DEFAULT_SOURCE_ID) }
        }.getOrElse { emptyList() }
    }

    suspend fun getPopularNovels(sourceId: String): List<Novel> {
        val normalizedSourceId = SourceCatalog.normalize(sourceId)
        val primaryResults = runCatching {
            getSource(normalizedSourceId).getPopularNovels().map { it.withSource(normalizedSourceId) }
        }.getOrElse { emptyList() }
        if (primaryResults.isNotEmpty() || normalizedSourceId == SourceCatalog.DEFAULT_SOURCE_ID) {
            return primaryResults
        }

        return runCatching {
            getSource(SourceCatalog.DEFAULT_SOURCE_ID).getPopularNovels()
                .map { it.withSource(SourceCatalog.DEFAULT_SOURCE_ID) }
        }.getOrElse { emptyList() }
    }

    private fun Novel.withSource(sourceId: String): Novel {
        return if (this.sourceId == sourceId) this else copy(sourceId = sourceId)
    }

    suspend fun getNovelDetails(novelUrl: String, sourceId: String): NovelDetails {
        val resolvedSourceId = SourceCatalog.detectSourceId(novelUrl) ?: SourceCatalog.normalize(sourceId)
        val details = getSource(resolvedSourceId).getNovelDetails(novelUrl)
        
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
        val chapters = getSource(normalizedSourceId).getChapterList(novelUrl)
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

    suspend fun getChapterContent(chapterUrl: String, sourceId: String): String {
        val resolvedSourceId = SourceCatalog.detectSourceId(chapterUrl) ?: SourceCatalog.normalize(sourceId)
        val chapterEntity = novelDao.getChapter(chapterUrl)
        if (chapterEntity?.isDownloaded == true && !chapterEntity.content.isNullOrBlank()) {
            return chapterEntity.content
        }
        return try {
            getSource(resolvedSourceId).getChapterContent(chapterUrl)
        } catch (e: Exception) {
            "Error loading chapter content: ${e.message}"
        }
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
}
