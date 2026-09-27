package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

class OpenRouterProvider(
    val apiKey: String? = null,
    val modelName: String = "anthropic/claude-3.5-sonnet"
) : AiProvider {

    override val providerName: String = "OpenRouter ($modelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val effectiveModel = request.modelName ?: modelName
        return FlintResult.Success(
            AiResponse(
                content = "OpenRouter [$effectiveModel] response for prompt: ${request.prompt}",
                providerUsed = "OpenRouter ($effectiveModel)",
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override suspend fun isHealthy(): Boolean = true
}
