package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow
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

        val preferred = request.providerPreference
        val matchingProviders = if (!preferred.isNullOrBlank()) {
            providers.filter { it.providerName.contains(preferred, ignoreCase = true) }
        } else emptyList()

        val targetProviders = if (matchingProviders.isNotEmpty()) matchingProviders else providers

        FlintLogger.d(tag, "Target providers: ${targetProviders.map { it.providerName }}")

        var lastError: AppError? = null

        for (provider in targetProviders) {
            if (!provider.isHealthy()) {
                val unhealthyErr = AppError.AiProvider("${provider.providerName} is not configured or failed health check.")
                FlintLogger.w(tag, "Provider ${provider.providerName} failed health check. Skipping.")
                lastError = unhealthyErr
                continue
            }

            FlintLogger.i(tag, "Executing request with provider: ${provider.providerName}")
            when (val result = provider.generate(request)) {
                is FlintResult.Success -> {
                    FlintLogger.i(tag, "Generation SUCCESS with ${result.data.providerUsed} | Tokens used: ${result.data.tokensUsed}")
                    return result
                }
                is FlintResult.Error -> {
                    FlintLogger.w(tag, "Provider ${provider.providerName} failed with error: ${result.error.message}.")
                    lastError = result.error
                }
            }
        }

        val finalError = lastError ?: AppError.AiProvider("Configured AI provider failed")
        FlintLogger.e(tag, "AI generation failed. Returning error: ${finalError.message}")
        return FlintResult.Error(finalError)
    }

    override fun generateContentStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val tag = "AiTaskRouter[Stream]"
        FlintLogger.i(tag, "Received generateContentStream request | Prompt len: ${request.prompt.length} | Preferred: ${request.providerPreference}")

        if (providers.isEmpty()) {
            FlintLogger.e(tag, "No AI providers configured in AiTaskRouter for streaming")
            emit(FlintResult.Error(AppError.AiProvider("No AI providers configured in AiTaskRouter")))
            return@flow
        }

        val preferred = request.providerPreference
        val matchingProviders = if (!preferred.isNullOrBlank()) {
            providers.filter { it.providerName.contains(preferred, ignoreCase = true) }
        } else emptyList()

        val targetProviders = if (matchingProviders.isNotEmpty()) matchingProviders else providers

        var lastError: AppError? = null

        for (provider in targetProviders) {
            if (!provider.isHealthy()) {
                FlintLogger.w(tag, "Provider ${provider.providerName} is unhealthy. Skipping for stream.")
                continue
            }
            FlintLogger.i(tag, "Initiating stream flow with provider: ${provider.providerName}")

            var providerSucceeded = false
            provider.generateStream(request).collect { result ->
                when (result) {
                    is FlintResult.Success -> {
                        providerSucceeded = true
                        emit(result)
                    }
                    is FlintResult.Error -> {
                        lastError = result.error
                        FlintLogger.w(tag, "Provider ${provider.providerName} stream failed: ${result.error.message}")
                    }
                }
            }

            if (providerSucceeded) {
                FlintLogger.i(tag, "Streaming SUCCESS with ${provider.providerName}")
                return@flow
            }

            FlintLogger.w(tag, "Streaming failed with ${provider.providerName}. Falling back to next provider...")
        }

        val finalError = lastError ?: AppError.AiProvider("Configured streaming AI provider failed")
        FlintLogger.e(tag, "All target streaming AI providers failed: ${finalError.message}")
        emit(FlintResult.Error(finalError))
    }
}
