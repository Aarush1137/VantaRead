package com.example.vantaread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey val url: String,
    val title: String,
    val coverUrl: String,
    val synopsis: String,
    val author: String,
    val genres: String, // comma-separated
    val status: String,
    val latestUpdate: String,
    val isBookmarked: Boolean = false,
    val currentChapterUrl: String? = null,
    val currentScrollPosition: Int = 0,
    val sourceId: String = "wtr-lab" // Default for backward compatibility
)
