package com.example.vantaread.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {

    @Query("SELECT * FROM novels")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels")
    suspend fun getAllNovelsSynchronous(): List<NovelEntity>

    @Query("SELECT * FROM novels WHERE isBookmarked = 1")
    fun getBookmarkedNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE isBookmarked = 1")
    suspend fun getBookmarkedNovelsSynchronous(): List<NovelEntity>

    @Query("SELECT * FROM novels WHERE url = :url")
    suspend fun getNovel(url: String): NovelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity)

    @Query("UPDATE novels SET isBookmarked = :isBookmarked WHERE url = :url")
    suspend fun updateBookmarkStatus(url: String, isBookmarked: Boolean)

    @Query("UPDATE novels SET currentChapterUrl = :chapterUrl, currentScrollPosition = :scrollPos WHERE url = :novelUrl")
    suspend fun updateReadingProgress(novelUrl: String, chapterUrl: String, scrollPos: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Query("SELECT * FROM chapters WHERE url = :chapterUrl")
    suspend fun getChapter(chapterUrl: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelUrl = :novelUrl ORDER BY chapterIndex ASC")
    fun getChaptersForNovel(novelUrl: String): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE novelUrl = :novelUrl ORDER BY chapterIndex ASC")
    suspend fun getChaptersListForNovel(novelUrl: String): List<ChapterEntity>

    @Query("""
        SELECT
            chapters.url AS chapterUrl,
            chapters.novelUrl AS novelUrl,
            chapters.title AS chapterTitle,
            chapters.chapterIndex AS chapterIndex,
            novels.title AS novelTitle,
            novels.coverUrl AS coverUrl,
            novels.sourceId AS sourceId
        FROM chapters
        INNER JOIN novels ON chapters.novelUrl = novels.url
        WHERE chapters.isDownloaded = 1 AND chapters.content IS NOT NULL AND chapters.content != ''
        ORDER BY novels.title COLLATE NOCASE ASC, chapters.chapterIndex ASC
    """)
    fun getDownloadedChapters(): Flow<List<DownloadedChapter>>

    @Query("UPDATE chapters SET content = NULL, isDownloaded = 0 WHERE url = :chapterUrl")
    suspend fun removeDownloadedChapter(chapterUrl: String)

    @Query("UPDATE chapters SET content = NULL, isDownloaded = 0 WHERE isDownloaded = 1")
    suspend fun removeAllDownloadedChapters()

    @Query("SELECT COUNT(*) FROM chapters WHERE novelUrl = :novelUrl")
    suspend fun getChapterCountForNovel(novelUrl: String): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE novelUrl = :novelUrl AND isDownloaded = 1 AND content IS NOT NULL AND content != ''")
    suspend fun getDownloadedChapterCountForNovel(novelUrl: String): Int
}
