package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemorySourceRepository
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeUrlParser
import com.shubhamthorat.flint.domain.repository.ProcessingStatus
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceType
import com.shubhamthorat.flint.domain.usecase.CreateYouTubeSourceUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeSourceTest {

    @Test
    fun testYouTubeUrlParserStandardUrl() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    @Test
    fun testYouTubeUrlParserShortUrl() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    @Test
    fun testYouTubeUrlParserShortsUrl() {
        val url = "https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share"
        assertTrue(YouTubeUrlParser.isValidUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    @Test
    fun testYouTubeUrlParserUrlWithParameters() {
        val url = "https://youtube.com/watch?v=dQw4w9WgXcQ&t=120s&list=xyz"
        assertTrue(YouTubeUrlParser.isValidUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    @Test
    fun testYouTubeUrlParserInvalidUrls() {
        assertFalse(YouTubeUrlParser.isValidUrl(""))
        assertFalse(YouTubeUrlParser.isValidUrl("   "))
        assertFalse(YouTubeUrlParser.isValidUrl("not_a_url"))
        assertFalse(YouTubeUrlParser.isValidUrl("https://vimeo.com/123456"))
        assertFalse(YouTubeUrlParser.isValidUrl("https://youtube.com/watch"))
        assertFalse(YouTubeUrlParser.isValidUrl("https://youtube.com/watch?v="))

        assertNull(YouTubeUrlParser.parse(""))
        assertNull(YouTubeUrlParser.parse("invalid"))
    }

    @Test
    fun testCreateYouTubeSourceUseCaseSuccess() = runTest {
        val repository = InMemorySourceRepository()
        val useCase = CreateYouTubeSourceUseCase(repository)

        val result = useCase.execute("https://youtu.be/dQw4w9WgXcQ", titleOverride = "Rickroll Video")
        assertTrue(result is FlintResult.Success<*>)

        val source = (result as FlintResult.Success<SourceItem>).data
        assertEquals(SourceType.YOUTUBE_VIDEO, source.type)
        assertEquals("Rickroll Video", source.title)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", source.contentOrUrl)
        assertEquals(ProcessingStatus.QUEUED, source.status)
    }

    @Test
    fun testCreateYouTubeSourceUseCaseInvalidUrl() = runTest {
        val repository = InMemorySourceRepository()
        val useCase = CreateYouTubeSourceUseCase(repository)

        val result = useCase.execute("https://vimeo.com/999")
        assertTrue(result is FlintResult.Error<*>)
        assertTrue((result as FlintResult.Error<AppError>).error is AppError.Validation)
    }
}
