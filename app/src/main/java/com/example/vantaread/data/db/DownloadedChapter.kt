package com.example.vantaread.data.db

data class DownloadedChapter(
    val chapterUrl: String,
    val novelUrl: String,
    val chapterTitle: String,
    val chapterIndex: Int,
    val novelTitle: String,
    val coverUrl: String,
    val sourceId: String
)
