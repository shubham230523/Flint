package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.TranscriptSegment
import com.shubhamthorat.flint.domain.model.VideoTranscript
import com.shubhamthorat.flint.domain.repository.FakeTranscriptProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptProviderTest {

    @Test
    fun testTranscriptFormatting() {
        val segments = listOf(
            TranscriptSegment("Hello world.", 0L, 1000L),
            TranscriptSegment("Welcome to Flint.", 1000L, 2000L)
        )
        val transcript = VideoTranscript(
            videoId = "vid_123",
            segments = segments
        )

        assertEquals("Hello world. Welcome to Flint.", transcript.getFormattedTranscript())
    }

    @Test
    fun testFetchTranscriptSuccess() = runTest {
        val provider = FakeTranscriptProvider()
        val result = provider.getTranscript("vid_123")

        assertTrue(result is FlintResult.Success<*>)
        val transcript = (result as FlintResult.Success<VideoTranscript>).data
        assertEquals("vid_123", transcript.videoId)
        assertTrue(transcript.segments.isNotEmpty())
        assertTrue(transcript.getFormattedTranscript().contains("Flint"))
    }

    @Test
    fun testFetchTranscriptFailure() = runTest {
        val provider = FakeTranscriptProvider(
            shouldFail = true,
            failureError = AppError.Validation("Captions disabled")
        )

        val result = provider.getTranscript("vid_no_captions")
        assertTrue(result is FlintResult.Error<*>)
        val error = (result as FlintResult.Error<AppError>).error
        assertEquals("Captions disabled", error.message)
    }

    @Test
    fun testEmptyTranscriptReturnsError() = runTest {
        val provider = FakeTranscriptProvider()
        provider.registerTranscript(
            VideoTranscript(
                videoId = "empty_vid",
                language = "en",
                segments = emptyList(),
                fullText = ""
            )
        )

        val result = provider.getTranscript("empty_vid")
        assertTrue(result is FlintResult.Error<*>)
        assertEquals("Video transcript is empty", (result as FlintResult.Error<AppError>).error.message)
    }
}
