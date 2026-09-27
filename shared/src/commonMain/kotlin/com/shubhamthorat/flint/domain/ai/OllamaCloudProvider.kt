package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

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

    override suspend fun isHealthy(): Boolean = true
}
