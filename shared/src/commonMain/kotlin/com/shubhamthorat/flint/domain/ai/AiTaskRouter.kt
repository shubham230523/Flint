package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

class AiTaskRouter(
    private val providers: List<AiProvider>
) : AiRepository {

    override suspend fun generateContent(request: AiRequest): FlintResult<AiResponse, AppError> {
        if (providers.isEmpty()) {
            return FlintResult.Error(
                AppError.AiProvider("No AI providers configured in AiTaskRouter")
            )
        }

        val preferred = request.providerPreference
        val sortedProviders = if (preferred != null) {
            providers.sortedByDescending { it.providerName.equals(preferred, ignoreCase = true) }
        } else {
            providers
        }

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
}
