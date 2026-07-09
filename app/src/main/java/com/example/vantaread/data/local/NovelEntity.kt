package com.example.vantaread.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey val id: String, // Typically the URL or a unique slug
    val title: String,
    val author: String,
    val coverUrl: String,
    val summary: String,
    val sourceId: String // Identifies the plugin (e.g., "wtr-lab")
)
