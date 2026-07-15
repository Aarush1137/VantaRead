package com.example.vantaread.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NovelEntity::class, ChapterEntity::class, ReadingHistoryEntity::class, BookmarkEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
