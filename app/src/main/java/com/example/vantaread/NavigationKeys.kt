package com.example.vantaread

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Library : NavKey
@Serializable data object Discover : NavKey
@Serializable data object Suggestions : NavKey
@Serializable data object History : NavKey
@Serializable data object Settings : NavKey
@Serializable data class NovelDetail(val novelUrl: String, val sourceId: String) : NavKey
@Serializable data class Reader(val chapterUrl: String, val sourceId: String, val novelUrl: String = "", val chapterTitle: String = "") : NavKey
