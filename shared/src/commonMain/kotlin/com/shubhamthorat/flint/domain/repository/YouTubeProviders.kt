package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeVideoData

interface YouTubeVideoProvider {
    suspend fun fetchVideoData(videoId: String): FlintResult<YouTubeVideoData, AppError>
}

class FakeYouTubeVideoProvider(
    private val shouldFail: Boolean = false,
    private val failureError: AppError = AppError.Network("Video not found or network error")
) : YouTubeVideoProvider {

    private val videoDatabase = mutableMapOf<String, YouTubeVideoData>()

    fun registerVideo(data: YouTubeVideoData) {
        videoDatabase[data.videoId] = data
    }

    override suspend fun fetchVideoData(videoId: String): FlintResult<YouTubeVideoData, AppError> {
        val tag = "FakeYouTubeVideoProvider"
        if (shouldFail) {
            FlintLogger.w(tag, "Simulating failure for videoId: $videoId")
            return FlintResult.Error(failureError)
        }

        val data = videoDatabase[videoId] ?: YouTubeVideoData(
            videoId = videoId,
            title = "Building a Multiplatform AI Content System with Flint",
            description = "In this video we explore KMP, Compose Multiplatform, and AI Gateway patterns to power content creation workflows.",
            channelName = "Flint Engineering",
            publishedAt = "2026-09-01T12:00:00Z",
            duration = "12:30",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        )

        FlintLogger.i(tag, "Successfully fetched metadata for videoId: $videoId (${data.title})")
        return FlintResult.Success(data)
    }
}

interface TranscriptProvider {
    suspend fun getTranscript(videoId: String): FlintResult<com.shubhamthorat.flint.domain.model.VideoTranscript, AppError>
}

class FakeTranscriptProvider(
    private val shouldFail: Boolean = false,
    private val failureError: AppError = AppError.Validation("Transcript unavailable for this video")
) : TranscriptProvider {

    private val transcriptDatabase = mutableMapOf<String, com.shubhamthorat.flint.domain.model.VideoTranscript>()

    fun registerTranscript(transcript: com.shubhamthorat.flint.domain.model.VideoTranscript) {
        transcriptDatabase[transcript.videoId] = transcript
    }

    override suspend fun getTranscript(videoId: String): FlintResult<com.shubhamthorat.flint.domain.model.VideoTranscript, AppError> {
        val tag = "FakeTranscriptProvider"
        if (shouldFail) {
            FlintLogger.w(tag, "Simulating transcript failure for videoId: $videoId")
            return FlintResult.Error(failureError)
        }

        val transcript = transcriptDatabase[videoId] ?: run {
            val segments = listOf(
                com.shubhamthorat.flint.domain.model.TranscriptSegment(
                    text = "Welcome back to Flint. Today we're building a YouTube to Instagram pipeline.",
                    startTimeMs = 0L,
                    endTimeMs = 5000L
                ),
                com.shubhamthorat.flint.domain.model.TranscriptSegment(
                    text = "The key to content repurposing is understanding the core narrative spark first.",
                    startTimeMs = 5100L,
                    endTimeMs = 12000L
                ),
                com.shubhamthorat.flint.domain.model.TranscriptSegment(
                    text = "Once you extract key takeaways and hooks, generating Reels, Carousels, and Stories becomes effortless.",
                    startTimeMs = 12100L,
                    endTimeMs = 20000L
                ),
                com.shubhamthorat.flint.domain.model.TranscriptSegment(
                    text = "Remember: always ground AI content in source facts while preserving your brand's unique Creator DNA.",
                    startTimeMs = 20100L,
                    endTimeMs = 28000L
                )
            )
            val text = segments.joinToString(" ") { it.text }
            com.shubhamthorat.flint.domain.model.VideoTranscript(
                videoId = videoId,
                language = "en",
                segments = segments,
                fullText = text
            )
        }

        if (transcript.getFormattedTranscript().isBlank()) {
            FlintLogger.w(tag, "Transcript is empty for videoId: $videoId")
            return FlintResult.Error(AppError.Validation("Video transcript is empty"))
        }

        FlintLogger.i(tag, "Successfully retrieved transcript for videoId: $videoId (${transcript.segments.size} segments)")
        return FlintResult.Success(transcript)
    }
}
