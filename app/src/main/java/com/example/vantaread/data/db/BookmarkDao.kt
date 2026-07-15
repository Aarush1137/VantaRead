package com.example.vantaread.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE chapterUrl = :chapterUrl ORDER BY paragraphIndex ASC")
    fun getBookmarksForChapter(chapterUrl: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE novelUrl = :novelUrl ORDER BY timestamp DESC")
    fun getBookmarksForNovel(novelUrl: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: Long)
}
