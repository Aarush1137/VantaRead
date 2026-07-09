package com.example.vantaread.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["novelId"])]
)
data class ChapterEntity(
    @PrimaryKey val id: String, // The URL of the chapter
    val novelId: String,
    val title: String,
    val content: String?, // Nullable: null if not downloaded/fetched yet
    val orderIndex: Int, // Keeps chapters in sequential order
    val isRead: Boolean = false,
    val scrollPosition: Int = 0 // To save reading progress within the chapter
)
