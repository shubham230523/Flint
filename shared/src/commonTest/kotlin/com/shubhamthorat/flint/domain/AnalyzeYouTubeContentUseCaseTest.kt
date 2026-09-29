package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.TranscriptSegment
import com.shubhamthorat.flint.domain.model.VideoTranscript
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult
import com.shubhamthorat.flint.domain.model.YouTubeVideoData
import com.shubhamthorat.flint.domain.repository.ProcessingStatus
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceType
import com.shubhamthorat.flint.domain.usecase.AnalyzeYouTubeContentUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyzeYouTubeContentUseCaseTest {

    @Test
    fun testYouTubeContentAnalysisSerialization() {
        val analysis = YouTubeContentAnalysis(
            summary = "Comprehensive summary of Flint video",
            mainTopics = listOf("KMP", "AI Gateway", "Instagram Pipeline"),
            keyPoints = listOf("Ground AI in source facts", "Preserve Creator DNA"),
            notableQuotes = listOf("One spark. Endless stories."),
            potentialHooks = listOf("Stop building single-platform apps."),
            audience = "Software Engineers & Creators",
            contentThemes = listOf("Architecture", "Mobile AI")
        )

        val jsonStr = Json.encodeToString(YouTubeContentAnalysis.serializer(), analysis)
        val decoded = YouTubeContentAnalysis.parseFromJson(jsonStr)

        assertNotNull(decoded)
        assertEquals(analysis, decoded)
    }

    @Test
    fun testYouTubeContentAnalysisParseFromMarkdownCodeblock() {
        val rawAiResponse = """
            ```json
            {
              "summary": "Building AI apps with Flint",
              "mainTopics": ["KMP", "Compose"],
              "keyPoints": ["Cross platform UI"],
              "notableQuotes": ["One spark"],
              "potentialHooks": ["Did you know?"],
              "audience": "Developers",
              "contentThemes": ["Tech"]
            }
            ```
        """.trimIndent()

        val parsed = YouTubeContentAnalysis.parseFromJson(rawAiResponse)
        assertNotNull(parsed)
        assertEquals("Building AI apps with Flint", parsed.summary)
        assertEquals("Developers", parsed.audience)
    }

    @Test
    fun testYouTubeContentAnalysisParseInvalidJson() {
        val invalidJson = "This is not json text"
        val parsed = YouTubeContentAnalysis.parseFromJson(invalidJson)
        assertNull(parsed)
    }

    @Test
    fun testAnalyzeYouTubeContentUseCaseSuccess() = runTest {
        val jsonResponse = """
            {
              "summary": "Video discussing Flint architecture.",
              "mainTopics": ["AI Gateway", "KMP"],
              "keyPoints": ["AI abstraction", "Single codebase"],
              "notableQuotes": ["One spark endless stories"],
              "potentialHooks": ["How to build multiplatform AI apps"],
              "audience": "Engineers",
              "contentThemes": ["Software"]
            }
        """.trimIndent()

        val fakeAiProvider = FakeAiProvider(fixedResponseText = jsonResponse)
        val aiRepository = com.shubhamthorat.flint.domain.ai.AiTaskRouter(listOf(fakeAiProvider))
        val useCase = AnalyzeYouTubeContentUseCase(aiRepository = aiRepository)

        val sampleProcessingResult = YouTubeSourceProcessingResult(
            source = SourceItem(
                id = "yt_123",
                title = "Flint Architecture",
                type = SourceType.YOUTUBE_VIDEO,
                contentOrUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                status = ProcessingStatus.COMPLETED
            ),
            videoData = YouTubeVideoData(
                videoId = "dQw4w9WgXcQ",
                title = "Flint Architecture",
                description = "Video description",
                channelName = "Flint Channel",
                publishedAt = "2026-09-01",
                duration = "10:00",
                thumbnailUrl = "thumb.jpg",
                canonicalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            ),
            transcript = VideoTranscript(
                videoId = "dQw4w9WgXcQ",
                language = "en",
                segments = listOf(TranscriptSegment("Welcome to Flint.", 0L, 1000L)),
                fullText = "Welcome to Flint."
            ),
            normalizedUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )

        val result = useCase.execute(sampleProcessingResult)
        assertTrue(result is FlintResult.Success<*>)

        val analysis = (result as FlintResult.Success<*>).data as YouTubeContentAnalysis
        assertEquals("Video discussing Flint architecture.", analysis.summary)
        assertEquals(2, analysis.mainTopics.size)
        assertEquals("Engineers", analysis.audience)
    }

    @Test
    fun testAnalyzeYouTubeContentUseCaseAiFailure() = runTest {
        val fakeAiProvider = FakeAiProvider(
            shouldSucceed = false
        )
        val aiRepository = com.shubhamthorat.flint.domain.ai.AiTaskRouter(listOf(fakeAiProvider))
        val useCase = AnalyzeYouTubeContentUseCase(aiRepository = aiRepository)

        val sampleProcessingResult = YouTubeSourceProcessingResult(
            source = SourceItem("1", "Title", SourceType.YOUTUBE_VIDEO, "url", ProcessingStatus.COMPLETED),
            videoData = YouTubeVideoData("1", "Title", "Desc", "Chan", "2026", "10:00", "thumb", "url"),
            transcript = VideoTranscript("1", "en", emptyList(), "Some text"),
            normalizedUrl = "url"
        )

        val result = useCase.execute(sampleProcessingResult)
        assertTrue(result is FlintResult.Error<*>)
        assertTrue(((result as FlintResult.Error<*>).error as AppError).message.contains("failed"))
    }
}
