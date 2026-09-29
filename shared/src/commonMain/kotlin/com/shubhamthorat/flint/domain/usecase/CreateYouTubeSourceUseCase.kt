package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeUrlParser
import com.shubhamthorat.flint.domain.repository.ProcessingStatus
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceRepository
import com.shubhamthorat.flint.domain.repository.SourceType
import kotlin.random.Random

class CreateYouTubeSourceUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend fun execute(
        url: String,
        titleOverride: String? = null
    ): FlintResult<SourceItem, AppError> {
        val tag = "CreateYouTubeSourceUseCase"
        val parsed = YouTubeUrlParser.parse(url)
            ?: return FlintResult.Error(
                AppError.Validation("Invalid YouTube URL. Supported formats include watch?v=, youtu.be/, or shorts/")
            )

        val id = "yt_source_${parsed.videoId}_${Random.nextInt(1000, 9999)}"
        val title = titleOverride?.takeIf { it.isNotBlank() } ?: "YouTube Video (${parsed.videoId})"

        val sourceItem = SourceItem(
            id = id,
            title = title,
            type = SourceType.YOUTUBE_VIDEO,
            contentOrUrl = parsed.normalizedUrl,
            status = ProcessingStatus.QUEUED
        )

        FlintLogger.i(tag, "Creating YouTube SourceItem: ${sourceItem.id} | VideoId: ${parsed.videoId}")
        return sourceRepository.addSource(sourceItem)
    }
}
