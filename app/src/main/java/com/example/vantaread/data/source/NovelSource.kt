package com.example.vantaread.data.source

import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails

interface NovelSource {
    suspend fun searchNovels(query: String): List<Novel>
    suspend fun getNovelDetails(novelUrl: String): NovelDetails
    suspend fun getChapterList(novelUrl: String): List<Chapter>
    suspend fun getChapterContent(chapterUrl: String): String
}
