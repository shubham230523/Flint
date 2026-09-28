package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    YOUTUBE_SCRIPT, SHORT_SCRIPT, REEL_SCRIPT, LINKEDIN_POST, X_THREAD, INSTAGRAM_CAPTION, CAROUSEL, NEWSLETTER, BLOG, EMAIL, COMMUNITY_POST
}

@Serializable
enum class ContentStatus {
    DRAFT, SCHEDULED, PUBLISHED, ARCHIVED
}

@Serializable
data class ContentAsset(
    val id: String,
    val sourceId: String?,
    val title: String,
    val body: String,
    val type: ContentType,
    val status: ContentStatus = ContentStatus.DRAFT,
    val platform: String = "Generic",
    val createdAtTimestamp: Long = 0L
)

interface ContentRepository {
    fun observeContentAssets(): Flow<List<ContentAsset>>
    suspend fun getContentById(id: String): FlintResult<ContentAsset, AppError>
    suspend fun saveContent(asset: ContentAsset): FlintResult<ContentAsset, AppError>
    suspend fun deleteContent(id: String): FlintResult<Unit, AppError>
}
