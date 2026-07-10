package com.example.vantaread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey val chapterUrl: String,
    val novelUrl: String,
    val novelTitle: String,
    val chapterTitle: String,
    val coverUrl: String,
    val sourceId: String,
    val lastReadTimestamp: Long,
    val scrollPosition: Int = 0,
    val maxScrollPosition: Int = 0
)
