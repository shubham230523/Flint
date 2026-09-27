package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OpenRouterProvider(
    apiKey: String? = null,
    modelName: String? = null
) : AiProvider {

    val activeApiKey: String = apiKey?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.OPENROUTER_API_KEY
    val activeModelName: String = modelName?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.OPENROUTER_MODEL_NAME.ifEmpty { "anthropic/claude-3.5-sonnet" }

    override val providerName: String = "OpenRouter ($activeModelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val effectiveModel = request.modelName ?: activeModelName
        return FlintResult.Success(
            AiResponse(
                content = "OpenRouter [$effectiveModel] streaming-enabled response for prompt: ${request.prompt}",
                providerUsed = "OpenRouter ($effectiveModel)",
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val effectiveModel = request.modelName ?: activeModelName
        val fullResponse = "OpenRouter [$effectiveModel] streamed response for: ${request.prompt}"
        val words = fullResponse.split(" ")

        var accumulated = ""
        for (word in words) {
            accumulated = if (accumulated.isEmpty()) word else "$accumulated $word"
            emit(FlintResult.Success(accumulated))
            delay(30)
        }
    }

    override suspend fun isHealthy(): Boolean = true
}
