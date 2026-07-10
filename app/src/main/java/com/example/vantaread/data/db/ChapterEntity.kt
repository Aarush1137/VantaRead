package com.example.vantaread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelUrl")]
)
data class ChapterEntity(
    @PrimaryKey val url: String,
    val novelUrl: String,
    val title: String,
    val chapterIndex: Int
)
