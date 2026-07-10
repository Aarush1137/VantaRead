package com.example.vantaread.data.model

data class Novel(
    val url: String,
    val title: String,
    val coverUrl: String,
    val author: String = "",
    val genres: List<String> = emptyList(),
    val status: String = "",
    val latestUpdate: String = "",
    val sourceId: String = ""
)
