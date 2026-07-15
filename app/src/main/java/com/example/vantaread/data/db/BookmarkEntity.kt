package com.example.vantaread.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["novelUrl"]),
        Index(value = ["chapterUrl"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelUrl: String,
    val chapterUrl: String,
    val paragraphIndex: Int,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
)
