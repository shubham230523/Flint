package com.shubhamthorat.flint.domain.ai

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
        if (providers.isEmpty()) {
            return FlintResult.Error(
                AppError.AiProvider("No AI providers configured in AiTaskRouter")
            )
        }

        val preferred = request.providerPreference ?: "OpenRouter"
        val sortedProviders = providers.sortedByDescending { it.providerName.contains(preferred, ignoreCase = true) }

        var lastError: AppError? = null

        for (provider in sortedProviders) {
            if (!provider.isHealthy()) continue

            when (val result = provider.generate(request)) {
                is FlintResult.Success -> return result
                is FlintResult.Error -> lastError = result.error
            }
        }

        return FlintResult.Error(
            lastError ?: AppError.AiProvider("All configured AI providers failed")
        )
    }

    override fun generateContentStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        if (providers.isEmpty()) {
            emit(FlintResult.Error(AppError.AiProvider("No AI providers configured in AiTaskRouter")))
            return@flow
        }

        val preferred = request.providerPreference ?: "OpenRouter"
        val sortedProviders = providers.sortedByDescending { it.providerName.contains(preferred, ignoreCase = true) }

        for (provider in sortedProviders) {
            if (!provider.isHealthy()) continue
            emitAll(provider.generateStream(request))
            return@flow
        }

        emit(FlintResult.Error(AppError.AiProvider("All configured streaming AI providers failed")))
    }
}
