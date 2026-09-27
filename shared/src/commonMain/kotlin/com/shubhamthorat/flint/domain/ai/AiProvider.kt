package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

interface AiProvider {
    val providerName: String
    suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError>
    suspend fun isHealthy(): Boolean
}
