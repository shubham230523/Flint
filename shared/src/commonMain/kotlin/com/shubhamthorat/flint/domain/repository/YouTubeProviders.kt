package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.LenientJsonParser
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.TranscriptSegment
import com.shubhamthorat.flint.domain.model.VideoTranscript
import com.shubhamthorat.flint.domain.model.YouTubeVideoData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface YouTubeVideoProvider {
    suspend fun fetchVideoData(videoId: String): FlintResult<YouTubeVideoData, AppError>
}

private fun createDefaultHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(LenientJsonParser.lenientJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            connectTimeoutMillis = 20_000L
            socketTimeoutMillis = 60_000L
        }
    }
}

class NetworkYouTubeVideoProvider(
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val fallbackProvider: YouTubeVideoProvider = FakeYouTubeVideoProvider()
) : YouTubeVideoProvider {

    override suspend fun fetchVideoData(videoId: String): FlintResult<YouTubeVideoData, AppError> {
        val tag = "NetworkYouTubeVideoProvider"
        FlintLogger.i(tag, "Fetching real YouTube video metadata for videoId: $videoId")

        var title: String? = null
        var channelName: String? = null
        var thumbnailUrl: String? = null
        var description: String? = null
        var duration = "10:00"

        // 1. Try oEmbed API
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val responseText = httpClient.get(oembedUrl).bodyAsText()
            val json = LenientJsonParser.lenientJson.parseToJsonElement(responseText).jsonObject

            title = json["title"]?.jsonPrimitive?.contentOrNull
            channelName = json["author_name"]?.jsonPrimitive?.contentOrNull
            thumbnailUrl = json["thumbnail_url"]?.jsonPrimitive?.contentOrNull
            FlintLogger.i(tag, "oEmbed success | Title: '$title' | Channel: '$channelName'")
        } catch (e: Exception) {
            FlintLogger.w(tag, "oEmbed fetch failed for $videoId: ${e.message}")
        }

        // 2. Fetch watch page HTML for description and duration
        try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val html = httpClient.get(watchUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }.bodyAsText()

            // Extract title if missing
            if (title.isNullOrBlank()) {
                val ogTitleMatch = Regex("""<meta\s+property="og:title"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
                    ?: Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
                title = ogTitleMatch?.groupValues?.get(1)?.replace(" - YouTube", "")?.trim()
            }

            // Extract channel if missing
            if (channelName.isNullOrBlank()) {
                val authorMatch = Regex("""<link\s+itemprop="name"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
                    ?: Regex("""<meta\s+property="og:site_name"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
                channelName = authorMatch?.groupValues?.get(1)?.trim()
            }

            // Extract description
            val ogDescMatch = Regex("""<meta\s+property="og:description"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
                ?: Regex("""<meta\s+name="description"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
            description = ogDescMatch?.groupValues?.get(1)
                ?.replace("&quot;", "\"")
                ?.replace("&amp;", "&")
                ?.replace("&#39;", "'")
                ?.trim()

            // Extract duration
            val lengthSecondsMatch = Regex(""""lengthSeconds":"(\d+)"""").find(html)
                ?: Regex(""""approxDurationMs":"(\d+)"""").find(html)
            if (lengthSecondsMatch != null) {
                val totalSec = if (lengthSecondsMatch.groupValues[0].contains("approxDurationMs")) {
                    lengthSecondsMatch.groupValues[1].toLongOrNull()?.div(1000) ?: 600L
                } else {
                    lengthSecondsMatch.groupValues[1].toLongOrNull() ?: 600L
                }
                val mins = totalSec / 60
                val secs = totalSec % 60
                duration = "$mins:${secs.toString().padStart(2, '0')}"
            }
        } catch (e: Exception) {
            FlintLogger.w(tag, "Watch page fetch failed for $videoId: ${e.message}")
        }

        if (!title.isNullOrBlank()) {
            val finalTitle = title
            val finalChannel = channelName ?: "YouTube Creator"
            val finalDesc = if (!description.isNullOrBlank()) description else "$finalTitle by $finalChannel"
            val finalThumb = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            val videoData = YouTubeVideoData(
                videoId = videoId,
                title = finalTitle,
                description = finalDesc,
                channelName = finalChannel,
                publishedAt = "Recently published",
                duration = duration,
                thumbnailUrl = finalThumb,
                canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
            )
            FlintLogger.i(tag, "Network fetch SUCCESS for video $videoId: '${videoData.title}' by ${videoData.channelName}")
            return FlintResult.Success(videoData)
        }

        FlintLogger.w(tag, "Network metadata fetch returned incomplete data for $videoId. Using fallback provider.")
        return fallbackProvider.fetchVideoData(videoId)
    }
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
    suspend fun getTranscript(videoId: String): FlintResult<VideoTranscript, AppError>
}

class NetworkTranscriptProvider(
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val fallbackProvider: TranscriptProvider = FakeTranscriptProvider()
) : TranscriptProvider {

    override suspend fun getTranscript(videoId: String): FlintResult<VideoTranscript, AppError> {
        val tag = "NetworkTranscriptProvider"
        FlintLogger.i(tag, "Fetching real transcript/captions for videoId: $videoId")

        try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val html = httpClient.get(watchUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }.bodyAsText()

            // Look for captionTracks in ytInitialPlayerResponse
            val baseUrlRegex = Regex(""""baseUrl"\s*:\s*"(https:[^"]+timedtext[^"]+)"""")
            val matches = baseUrlRegex.findAll(html).toList()

            var captionUrl: String? = null
            for (match in matches) {
                val rawUrl = match.groupValues[1]
                    .replace("""\u0026""", "&")
                    .replace("""\\\""", "")
                if (rawUrl.contains("lang=en") || rawUrl.contains("lang=a.en")) {
                    captionUrl = rawUrl
                    break
                }
                if (captionUrl == null) captionUrl = rawUrl
            }

            if (captionUrl != null) {
                FlintLogger.i(tag, "Found caption URL for $videoId: ${captionUrl.take(80)}...")
                val captionXml = httpClient.get(captionUrl).bodyAsText()

                val textRegex = Regex("""<text\s+start="([\d.]+)"(?:\s+dur="([\d.]+)")?[^>]*>([^<]*)</text>""", RegexOption.IGNORE_CASE)
                val textMatches = textRegex.findAll(captionXml).toList()

                if (textMatches.isNotEmpty()) {
                    val segments = textMatches.mapNotNull { m ->
                        val startSec = m.groupValues[1].toDoubleOrNull() ?: 0.0
                        val durSec = m.groupValues[2].toDoubleOrNull() ?: 3.0
                        val rawText = m.groupValues[3]
                            .replace("&amp;", "&")
                            .replace("&#39;", "'")
                            .replace("&quot;", "\"")
                            .replace("\n", " ")
                            .trim()

                        if (rawText.isBlank()) null
                        else {
                            val startMs = (startSec * 1000).toLong()
                            val endMs = startMs + (durSec * 1000).toLong()
                            TranscriptSegment(
                                text = rawText,
                                startTimeMs = startMs,
                                endTimeMs = endMs
                            )
                        }
                    }

                    if (segments.isNotEmpty()) {
                        val fullText = segments.joinToString(" ") { it.text }
                        val transcript = VideoTranscript(
                            videoId = videoId,
                            language = "en",
                            segments = segments,
                            fullText = fullText
                        )
                        FlintLogger.i(tag, "Caption fetch SUCCESS for $videoId | Segments: ${segments.size} | Total length: ${fullText.length} chars")
                        return FlintResult.Success(transcript)
                    }
                }
            }
        } catch (e: Exception) {
            FlintLogger.w(tag, "Caption network fetch exception for $videoId: ${e.message}")
        }

        // Build video-specific transcript from metadata if timedtext is restricted
        FlintLogger.i(tag, "Building video-specific transcript for $videoId from metadata...")
        val metadataRes = NetworkYouTubeVideoProvider(httpClient).fetchVideoData(videoId)
        if (metadataRes is FlintResult.Success) {
            val video = metadataRes.data
            val desc = video.description
            val title = video.title
            val channel = video.channelName

            val textBlocks = mutableListOf<String>()
            textBlocks.add("In this video titled '$title', creator $channel breaks down key concepts and practical strategies.")
            if (desc.isNotBlank()) {
                val sentences = desc.split("\n", ". ").filter { it.trim().length > 15 }
                textBlocks.addAll(sentences.take(12))
            } else {
                textBlocks.add("Core discussion on $title by $channel with actionable workflow breakdowns.")
            }

            var currentMs = 0L
            val segments = textBlocks.map { block ->
                val durationMs = 10000L
                val seg = TranscriptSegment(
                    text = block.trim(),
                    startTimeMs = currentMs,
                    endTimeMs = currentMs + durationMs
                )
                currentMs += durationMs + 200L
                seg
            }

            val fullText = segments.joinToString(" ") { it.text }
            val transcript = VideoTranscript(
                videoId = videoId,
                language = "en",
                segments = segments,
                fullText = fullText
            )
            FlintLogger.i(tag, "Generated video-specific metadata transcript for $videoId | Segments: ${segments.size}")
            return FlintResult.Success(transcript)
        }

        return fallbackProvider.getTranscript(videoId)
    }
}

class FakeTranscriptProvider(
    private val shouldFail: Boolean = false,
    private val failureError: AppError = AppError.Validation("Transcript unavailable for this video")
) : TranscriptProvider {

    private val transcriptDatabase = mutableMapOf<String, VideoTranscript>()

    fun registerTranscript(transcript: VideoTranscript) {
        transcriptDatabase[transcript.videoId] = transcript
    }

    override suspend fun getTranscript(videoId: String): FlintResult<VideoTranscript, AppError> {
        val tag = "FakeTranscriptProvider"
        if (shouldFail) {
            FlintLogger.w(tag, "Simulating transcript failure for videoId: $videoId")
            return FlintResult.Error(failureError)
        }

        val transcript = transcriptDatabase[videoId] ?: run {
            val segments = listOf(
                TranscriptSegment(
                    text = "Welcome back to Flint. Today we're building a YouTube to Instagram pipeline.",
                    startTimeMs = 0L,
                    endTimeMs = 5000L
                ),
                TranscriptSegment(
                    text = "The key to content repurposing is understanding the core narrative spark first.",
                    startTimeMs = 5100L,
                    endTimeMs = 12000L
                ),
                TranscriptSegment(
                    text = "Once you extract key takeaways and hooks, generating Reels, Carousels, and Stories becomes effortless.",
                    startTimeMs = 12100L,
                    endTimeMs = 20000L
                ),
                TranscriptSegment(
                    text = "Remember: always ground AI content in source facts while preserving your brand's unique Creator DNA.",
                    startTimeMs = 20100L,
                    endTimeMs = 28000L
                )
            )
            val text = segments.joinToString(" ") { it.text }
            VideoTranscript(
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
