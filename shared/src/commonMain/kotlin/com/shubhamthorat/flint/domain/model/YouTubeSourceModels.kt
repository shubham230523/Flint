package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ParsedYouTubeUrl(
    val videoId: String,
    val normalizedUrl: String
)

object YouTubeUrlParser {

    private val videoIdPatterns = listOf(
        Regex("""(?:https?://)?(?:www\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.)?youtu\.be/([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})""")
    )

    fun isValidUrl(url: String): Boolean {
        return parse(url) != null
    }

    fun parse(rawUrl: String): ParsedYouTubeUrl? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        for (pattern in videoIdPatterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                val videoId = match.groupValues[1]
                if (videoId.isNotBlank()) {
                    return ParsedYouTubeUrl(
                        videoId = videoId,
                        normalizedUrl = "https://www.youtube.com/watch?v=$videoId"
                    )
                }
            }
        }
        return null
    }
}
