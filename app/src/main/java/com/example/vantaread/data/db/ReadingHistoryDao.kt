package com.example.vantaread.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {

    @Query("SELECT * FROM reading_history ORDER BY lastReadTimestamp DESC")
    fun getAllHistory(): Flow<List<ReadingHistoryEntity>>

    @Query("""
        SELECT rh.* FROM reading_history rh
        INNER JOIN (
            SELECT novelUrl, MAX(lastReadTimestamp) as maxTs
            FROM reading_history
            GROUP BY novelUrl
        ) latest ON rh.novelUrl = latest.novelUrl AND rh.lastReadTimestamp = latest.maxTs
        ORDER BY rh.lastReadTimestamp DESC
    """)
    fun getRecentNovels(): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE novelUrl = :novelUrl ORDER BY lastReadTimestamp DESC LIMIT 1")
    suspend fun getLastReadChapter(novelUrl: String): ReadingHistoryEntity?

    @Query("SELECT chapterUrl FROM reading_history WHERE novelUrl = :novelUrl")
    suspend fun getReadChapterUrls(novelUrl: String): List<String>

    @Query("SELECT * FROM reading_history WHERE chapterUrl = :chapterUrl")
    suspend fun getHistoryEntry(chapterUrl: String): ReadingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history WHERE chapterUrl = :chapterUrl")
    suspend fun deleteEntry(chapterUrl: String)

    @Query("DELETE FROM reading_history")
    suspend fun clearAll()
}
