package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val providerName: String
    suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError>
    fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>>
    suspend fun isHealthy(): Boolean
}
