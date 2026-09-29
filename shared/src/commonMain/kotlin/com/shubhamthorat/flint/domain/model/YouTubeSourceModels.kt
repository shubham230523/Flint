package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ParsedYouTubeUrl(
    val videoId: String,
    val normalizedUrl: String
)

@Serializable
data class YouTubeVideoData(
    val videoId: String,
    val title: String,
    val description: String,
    val channelName: String,
    val publishedAt: String,
    val duration: String,
    val thumbnailUrl: String,
    val canonicalUrl: String
)

@Serializable
data class TranscriptSegment(
    val text: String,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
)

@Serializable
data class VideoTranscript(
    val videoId: String,
    val language: String = "en",
    val segments: List<TranscriptSegment> = emptyList(),
    val fullText: String = ""
) {
    fun getFormattedTranscript(): String {
        return if (fullText.isNotBlank()) {
            fullText
        } else {
            segments.joinToString(" ") { it.text }
        }
    }
}

@Serializable
data class YouTubeSourceProcessingResult(
    val source: com.shubhamthorat.flint.domain.repository.SourceItem,
    val videoData: YouTubeVideoData,
    val transcript: VideoTranscript,
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
