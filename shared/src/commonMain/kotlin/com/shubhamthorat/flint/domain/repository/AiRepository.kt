package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow

data class AiRequest(
    val prompt: String,
    val providerPreference: String? = "OpenRouter",
    val modelName: String? = null,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f
)

data class AiResponse(
    val content: String,
    val providerUsed: String,
    val tokensUsed: Int
)

interface AiRepository {
    suspend fun generateContent(request: AiRequest): FlintResult<AiResponse, AppError>
    fun generateContentStream(request: AiRequest): Flow<FlintResult<String, AppError>>
}
