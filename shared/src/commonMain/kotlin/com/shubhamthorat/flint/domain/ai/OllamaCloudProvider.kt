package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OllamaCloudProvider(
    private val endpointUrl: String? = null
) : AiProvider {

    override val providerName: String = "OllamaCloud"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        return FlintResult.Success(
            AiResponse(
                content = "OllamaCloud response for prompt: ${request.prompt}",
                providerUsed = providerName,
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        emit(FlintResult.Success("OllamaCloud streamed response for: ${request.prompt}"))
    }

    override suspend fun isHealthy(): Boolean = true
}
