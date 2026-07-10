package com.example.vantaread.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {

    @Query("SELECT * FROM novels WHERE isBookmarked = 1")
    fun getBookmarkedNovels(): Flow<List<NovelEntity>>

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

    @Query("SELECT * FROM chapters WHERE novelUrl = :novelUrl ORDER BY chapterIndex ASC")
    fun getChaptersForNovel(novelUrl: String): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE novelUrl = :novelUrl ORDER BY chapterIndex ASC")
    suspend fun getChaptersListForNovel(novelUrl: String): List<ChapterEntity>
}
