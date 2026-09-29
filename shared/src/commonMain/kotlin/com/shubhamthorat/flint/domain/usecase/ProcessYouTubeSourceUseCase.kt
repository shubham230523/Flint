package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult
import com.shubhamthorat.flint.domain.model.YouTubeUrlParser
import com.shubhamthorat.flint.domain.repository.ProcessingStatus
import com.shubhamthorat.flint.domain.repository.SourceRepository
import com.shubhamthorat.flint.domain.repository.TranscriptProvider
import com.shubhamthorat.flint.domain.repository.YouTubeVideoProvider

class ProcessYouTubeSourceUseCase(
    private val sourceRepository: SourceRepository,
    private val videoProvider: YouTubeVideoProvider,
    private val transcriptProvider: TranscriptProvider
) {
    suspend fun execute(sourceId: String): FlintResult<YouTubeSourceProcessingResult, AppError> {
        val tag = "ProcessYouTubeSourceUseCase"
        FlintLogger.i(tag, "Starting processing for source ID: $sourceId")

        // 1. Fetch Source Item from repository
        val sourceResult = sourceRepository.getSourceById(sourceId)
        if (sourceResult is FlintResult.Error) {
            FlintLogger.e(tag, "Source $sourceId not found")
            return FlintResult.Error(sourceResult.error)
        }

        val source = (sourceResult as FlintResult.Success).data
        val parsed = YouTubeUrlParser.parse(source.contentOrUrl)
            ?: run {
                val err = AppError.Validation("Invalid YouTube URL in source item: ${source.contentOrUrl}")
                sourceRepository.addSource(source.copy(status = ProcessingStatus.FAILED))
                return FlintResult.Error(err)
            }

        // 2. Transition state to PROCESSING
        sourceRepository.addSource(source.copy(status = ProcessingStatus.PROCESSING))

        // 3. Fetch Video Metadata
        FlintLogger.d(tag, "Fetching metadata for videoId: ${parsed.videoId}")
        val videoResult = videoProvider.fetchVideoData(parsed.videoId)
        if (videoResult is FlintResult.Error) {
            FlintLogger.w(tag, "Metadata fetch failed for ${parsed.videoId}: ${videoResult.error.message}")
            sourceRepository.addSource(source.copy(status = ProcessingStatus.FAILED))
            return FlintResult.Error(videoResult.error)
        }
        val videoData = (videoResult as FlintResult.Success).data

        // 4. Fetch Transcript
        FlintLogger.d(tag, "Fetching transcript for videoId: ${parsed.videoId}")
        val transcriptResult = transcriptProvider.getTranscript(parsed.videoId)
        if (transcriptResult is FlintResult.Error) {
            FlintLogger.w(tag, "Transcript fetch failed for ${parsed.videoId}: ${transcriptResult.error.message}")
            sourceRepository.addSource(source.copy(status = ProcessingStatus.FAILED))
            return FlintResult.Error(transcriptResult.error)
        }
        val transcript = (transcriptResult as FlintResult.Success).data

        // 5. Update Source Title if default and mark COMPLETED
        val updatedTitle = if (source.title.startsWith("YouTube Video (")) videoData.title else source.title
        val completedSource = source.copy(
            title = updatedTitle,
            status = ProcessingStatus.COMPLETED
        )
        sourceRepository.addSource(completedSource)

        val processingResult = YouTubeSourceProcessingResult(
            source = completedSource,
            videoData = videoData,
            transcript = transcript,
            normalizedUrl = parsed.normalizedUrl
        )

        FlintLogger.i(tag, "Processing COMPLETED for video: ${videoData.title} (${parsed.videoId})")
        return FlintResult.Success(processingResult)
    }
}
