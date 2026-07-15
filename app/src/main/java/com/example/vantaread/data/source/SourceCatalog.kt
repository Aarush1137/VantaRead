package com.example.vantaread.data.source

data class SourceInfo(
    val id: String,
    val name: String,
    val hosts: List<String>
)

object SourceCatalog {
    const val ROYAL_ROAD = "royalroad"
    const val NOVEL_FULL = "novelfull"
    const val LIGHT_NOVEL_PUB = "lightnovelpub"
    const val FREE_WEB_NOVEL = "freewebnovel"
    const val SCRIBBLE_HUB = "scribblehub"

    const val DEFAULT_SOURCE_ID = ROYAL_ROAD

    val sources = listOf(
        SourceInfo(ROYAL_ROAD, "Royal Road", listOf("royalroad.com")),
        SourceInfo(NOVEL_FULL, "NovelFull", listOf("novelfull.com")),
        SourceInfo(LIGHT_NOVEL_PUB, "LightNovelPub", listOf("lightnovelpub.me", "lightnovelpub.com", "lightnovelpub.vip")),
        SourceInfo(FREE_WEB_NOVEL, "FreeWebNovel", listOf("freewebnovel.com")),
        SourceInfo(SCRIBBLE_HUB, "ScribbleHub", listOf("scribblehub.com"))
    )

    fun normalize(sourceId: String): String {
        return when (sourceId.trim().lowercase()) {
            "royal_road", "royal-road" -> ROYAL_ROAD
            "free_web_novel", "free-web-novel" -> FREE_WEB_NOVEL
            "scribble_hub", "scribble-hub" -> SCRIBBLE_HUB
            "light_novel_pub", "light-novel-pub" -> LIGHT_NOVEL_PUB
            else -> sourceId.trim().lowercase()
        }
    }

    fun nameFor(sourceId: String): String {
        val normalized = normalize(sourceId)
        return sources.firstOrNull { it.id == normalized }?.name ?: normalized
    }

    fun detectSourceId(url: String): String? {
        val normalizedUrl = url.trim().lowercase()
        return sources.firstOrNull { source ->
            source.hosts.any { host -> normalizedUrl.contains(host) }
        }?.id
    }
}
