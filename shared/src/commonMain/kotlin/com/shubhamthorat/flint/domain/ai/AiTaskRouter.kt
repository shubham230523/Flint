package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class AiTaskRouter(
    private val providers: List<AiProvider>
) : AiRepository {

    override suspend fun generateContent(request: AiRequest): FlintResult<AiResponse, AppError> {
        val tag = "AiTaskRouter"
        FlintLogger.i(tag, "Received generateContent request | Prompt len: ${request.prompt.length} | Preferred: ${request.providerPreference}")

        if (providers.isEmpty()) {
            FlintLogger.e(tag, "No AI providers configured in AiTaskRouter")
            return FlintResult.Error(
                AppError.AiProvider("No AI providers configured in AiTaskRouter")
            )
        }

        val preferred = request.providerPreference ?: "OpenRouter"
        val sortedProviders = providers.sortedByDescending { it.providerName.contains(preferred, ignoreCase = true) }

        FlintLogger.d(tag, "Provider search order: ${sortedProviders.map { it.providerName }}")

        var lastError: AppError? = null

        for (provider in sortedProviders) {
            if (!provider.isHealthy()) {
                FlintLogger.w(tag, "Provider ${provider.providerName} failed health check. Skipping.")
                continue
            }

            FlintLogger.i(tag, "Executing request with provider: ${provider.providerName}")
            when (val result = provider.generate(request)) {
                is FlintResult.Success -> {
                    FlintLogger.i(tag, "Generation SUCCESS with ${result.data.providerUsed} | Tokens used: ${result.data.tokensUsed}")
                    return result
                }
                is FlintResult.Error -> {
                    FlintLogger.w(tag, "Provider ${provider.providerName} failed with error: ${result.error.message}. Attempting failover...")
                    lastError = result.error
                }
            }
        }

        FlintLogger.e(tag, "All AI providers failed. Returning last error: ${lastError?.message}")
        return FlintResult.Error(
            lastError ?: AppError.AiProvider("All configured AI providers failed")
        )
    }

    override fun generateContentStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val tag = "AiTaskRouter[Stream]"
        FlintLogger.i(tag, "Received generateContentStream request | Prompt len: ${request.prompt.length} | Preferred: ${request.providerPreference}")

        if (providers.isEmpty()) {
            FlintLogger.e(tag, "No AI providers configured in AiTaskRouter for streaming")
            emit(FlintResult.Error(AppError.AiProvider("No AI providers configured in AiTaskRouter")))
            return@flow
        }

        val preferred = request.providerPreference ?: "OpenRouter"
        val sortedProviders = providers.sortedByDescending { it.providerName.contains(preferred, ignoreCase = true) }

        for (provider in sortedProviders) {
            if (!provider.isHealthy()) {
                FlintLogger.w(tag, "Provider ${provider.providerName} is unhealthy. Skipping for stream.")
                continue
            }
            FlintLogger.i(tag, "Initiating stream flow with provider: ${provider.providerName}")
            emitAll(provider.generateStream(request))
            return@flow
        }

        FlintLogger.e(tag, "All configured streaming AI providers failed")
        emit(FlintResult.Error(AppError.AiProvider("All configured streaming AI providers failed")))
    }
}
