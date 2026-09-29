package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeVideoData
import com.shubhamthorat.flint.domain.repository.FakeYouTubeVideoProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YouTubeVideoProviderTest {

    @Test
    fun testYouTubeVideoDataSerialization() {
        val original = YouTubeVideoData(
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            description = "Official Music Video",
            channelName = "Rick Astley",
            publishedAt = "2009-10-25",
            duration = "3:33",
            thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            canonicalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )

        val jsonString = Json.encodeToString(YouTubeVideoData.serializer(), original)
        val decoded = Json.decodeFromString(YouTubeVideoData.serializer(), jsonString)

        assertEquals(original, decoded)
    }

    @Test
    fun testFetchVideoDataSuccess() = runTest {
        val provider = FakeYouTubeVideoProvider()
        val result = provider.fetchVideoData("dQw4w9WgXcQ")

        assertTrue(result is FlintResult.Success<*>)
        val data = (result as FlintResult.Success<YouTubeVideoData>).data
        assertEquals("dQw4w9WgXcQ", data.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", data.canonicalUrl)
    }

    @Test
    fun testFetchVideoDataCustomRegistration() = runTest {
        val provider = FakeYouTubeVideoProvider()
        val custom = YouTubeVideoData(
            videoId = "custom_id",
            title = "Custom Title",
            description = "Custom Description",
            channelName = "Custom Channel",
            publishedAt = "2026-01-01",
            duration = "05:00",
            thumbnailUrl = "https://example.com/thumb.jpg",
            canonicalUrl = "https://www.youtube.com/watch?v=custom_id"
        )
        provider.registerVideo(custom)

        val result = provider.fetchVideoData("custom_id")
        assertTrue(result is FlintResult.Success<*>)
        assertEquals(custom, (result as FlintResult.Success<YouTubeVideoData>).data)
    }

    @Test
    fun testFetchVideoDataFailureCases() = runTest {
        val provider = FakeYouTubeVideoProvider(
            shouldFail = true,
            failureError = AppError.Network("Video unavailable or private")
        )

        val result = provider.fetchVideoData("private_video")
        assertTrue(result is FlintResult.Error<*>)
        val error = (result as FlintResult.Error<AppError>).error
        assertTrue(error is AppError.Network)
        assertEquals("Video unavailable or private", error.message)
    }
}
