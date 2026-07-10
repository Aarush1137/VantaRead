package com.example.vantaread.ui.downloads

data class DownloadWorkStatus(
    val id: String,
    val novelTitle: String,
    val chapterTitle: String,
    val state: String,
    val progressText: String
)
