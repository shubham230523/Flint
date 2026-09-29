package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
enum class SourceType {
    VIDEO, AUDIO, PDF, DOCUMENT, PRESENTATION, ARTICLE, BLOG, URL, IDEA, GITHUB, TEXT, YOUTUBE_VIDEO
}

@Serializable
enum class ProcessingStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED
}

@Serializable
data class SourceItem(
    val id: String,
    val title: String,
    val type: SourceType,
    val contentOrUrl: String,
    val status: ProcessingStatus = ProcessingStatus.QUEUED,
    val createdAtTimestamp: Long = 0L
)

interface SourceRepository {
    fun observeSources(): Flow<List<SourceItem>>
    suspend fun getSourceById(id: String): FlintResult<SourceItem, AppError>
    suspend fun addSource(source: SourceItem): FlintResult<SourceItem, AppError>
    suspend fun deleteSource(id: String): FlintResult<Unit, AppError>
}
