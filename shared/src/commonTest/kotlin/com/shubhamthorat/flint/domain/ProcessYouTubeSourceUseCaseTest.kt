package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemorySourceRepository
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.VideoTranscript
import com.shubhamthorat.flint.domain.repository.FakeTranscriptProvider
import com.shubhamthorat.flint.domain.repository.FakeYouTubeVideoProvider
import com.shubhamthorat.flint.domain.repository.ProcessingStatus
import com.shubhamthorat.flint.domain.usecase.CreateYouTubeSourceUseCase
import com.shubhamthorat.flint.domain.usecase.ProcessYouTubeSourceUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessYouTubeSourceUseCaseTest {

    @Test
    fun testProcessYouTubeSourceSuccess() = runTest {
        val sourceRepository = InMemorySourceRepository()
        val createSourceUseCase = CreateYouTubeSourceUseCase(sourceRepository)
        val videoProvider = FakeYouTubeVideoProvider()
        val transcriptProvider = FakeTranscriptProvider()

        val processUseCase = ProcessYouTubeSourceUseCase(
            sourceRepository = sourceRepository,
            videoProvider = videoProvider,
            transcriptProvider = transcriptProvider
        )

        val createResult = createSourceUseCase.execute("https://youtu.be/dQw4w9WgXcQ")
        val createdSource = (createResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem

        val processResult = processUseCase.execute(createdSource.id)
        assertTrue(processResult is FlintResult.Success<*>)

        val result = (processResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult
        assertEquals("dQw4w9WgXcQ", result.videoData.videoId)
        assertTrue(result.transcript.segments.isNotEmpty())

        val updatedSourceResult = sourceRepository.getSourceById(createdSource.id)
        assertTrue(updatedSourceResult is FlintResult.Success<*>)
        assertEquals(ProcessingStatus.COMPLETED, ((updatedSourceResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem).status)
    }

    @Test
    fun testProcessYouTubeSourceVideoMetadataFailure() = runTest {
        val sourceRepository = InMemorySourceRepository()
        val createSourceUseCase = CreateYouTubeSourceUseCase(sourceRepository)
        val videoProvider = FakeYouTubeVideoProvider(shouldFail = true)
        val transcriptProvider = FakeTranscriptProvider()

        val processUseCase = ProcessYouTubeSourceUseCase(
            sourceRepository = sourceRepository,
            videoProvider = videoProvider,
            transcriptProvider = transcriptProvider
        )

        val createResult = createSourceUseCase.execute("https://youtu.be/dQw4w9WgXcQ")
        val createdSource = (createResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem

        val processResult = processUseCase.execute(createdSource.id)
        assertTrue(processResult is FlintResult.Error<*>)

        val updatedSourceResult = sourceRepository.getSourceById(createdSource.id)
        assertEquals(ProcessingStatus.FAILED, ((updatedSourceResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem).status)
    }

    @Test
    fun testProcessYouTubeSourceTranscriptFailure() = runTest {
        val sourceRepository = InMemorySourceRepository()
        val createSourceUseCase = CreateYouTubeSourceUseCase(sourceRepository)
        val videoProvider = FakeYouTubeVideoProvider()
        val transcriptProvider = FakeTranscriptProvider(
            shouldFail = true,
            failureError = AppError.Validation("Captions unavailable")
        )

        val processUseCase = ProcessYouTubeSourceUseCase(
            sourceRepository = sourceRepository,
            videoProvider = videoProvider,
            transcriptProvider = transcriptProvider
        )

        val createResult = createSourceUseCase.execute("https://youtu.be/dQw4w9WgXcQ")
        val createdSource = (createResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem

        val processResult = processUseCase.execute(createdSource.id)
        assertTrue(processResult is FlintResult.Error<*>)
        assertEquals("Captions unavailable", ((processResult as FlintResult.Error<*>).error as AppError).message)

        val updatedSourceResult = sourceRepository.getSourceById(createdSource.id)
        assertEquals(ProcessingStatus.FAILED, ((updatedSourceResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem).status)
    }

    @Test
    fun testProcessYouTubeSourceEmptyTranscript() = runTest {
        val sourceRepository = InMemorySourceRepository()
        val createSourceUseCase = CreateYouTubeSourceUseCase(sourceRepository)
        val videoProvider = FakeYouTubeVideoProvider()
        val transcriptProvider = FakeTranscriptProvider()

        transcriptProvider.registerTranscript(
            VideoTranscript(
                videoId = "dQw4w9WgXcQ",
                language = "en",
                segments = emptyList(),
                fullText = ""
            )
        )

        val processUseCase = ProcessYouTubeSourceUseCase(
            sourceRepository = sourceRepository,
            videoProvider = videoProvider,
            transcriptProvider = transcriptProvider
        )

        val createResult = createSourceUseCase.execute("https://youtu.be/dQw4w9WgXcQ")
        val createdSource = (createResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem

        val processResult = processUseCase.execute(createdSource.id)
        assertTrue(processResult is FlintResult.Error<*>)

        val updatedSourceResult = sourceRepository.getSourceById(createdSource.id)
        assertEquals(ProcessingStatus.FAILED, ((updatedSourceResult as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem).status)
    }
}
