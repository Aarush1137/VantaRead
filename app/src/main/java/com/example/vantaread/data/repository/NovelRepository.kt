package com.example.vantaread.data.repository

import com.example.vantaread.data.db.ChapterEntity
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.db.NovelEntity
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NovelRepository @Inject constructor(
    private val sources: Map<String, @JvmSuppressWildcards NovelSource>,
    private val novelDao: NovelDao
) {

    private fun getSource(sourceId: String): NovelSource {
        return sources[sourceId] ?: throw IllegalArgumentException("Source \$sourceId not found")
    }

    // --- Network Calls ---
    
    suspend fun searchNovels(query: String, sourceId: String): List<Novel> {
        return getSource(sourceId).searchNovels(query)
    }

    suspend fun getPopularNovels(sourceId: String): List<Novel> {
        return getSource(sourceId).getPopularNovels()
    }

    suspend fun getNovelDetails(novelUrl: String, sourceId: String): NovelDetails {
        val details = getSource(sourceId).getNovelDetails(novelUrl)
        // Optionally update the local DB if it's bookmarked
        val existing = novelDao.getNovel(novelUrl)
        if (existing != null) {
            novelDao.insertNovel(
                existing.copy(
                    title = details.title,
                    coverUrl = details.coverUrl,
                    synopsis = details.synopsis,
                    author = details.author,
                    genres = details.genres.joinToString(","),
                    status = details.status,
                    latestUpdate = details.latestUpdate
                )
            )
        }
        return details
    }

    suspend fun fetchAndCacheChapters(novelUrl: String, sourceId: String): List<Chapter> {
        val chapters = getSource(sourceId).getChapterList(novelUrl)
        novelDao.insertChapters(chapters.map {
            ChapterEntity(it.url, it.novelUrl, it.title, it.index)
        })
        return chapters
    }

    suspend fun getChapterContent(chapterUrl: String, sourceId: String): String {
        return getSource(sourceId).getChapterContent(chapterUrl)
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
                    sourceId = sourceId
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
}
