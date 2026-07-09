package com.example.vantaread.data.model

data class NovelDetails(
    val url: String,
    val title: String,
    val coverUrl: String,
    val synopsis: String,
    val author: String,
    val genres: List<String>,
    val status: String,
    val latestUpdate: String
)
