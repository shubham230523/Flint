package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

class FakeAiProvider(
    override val providerName: String,
    private val shouldSucceed: Boolean = true,
    private val responseDelayMs: Long = 0L
) : AiProvider {

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        if (!shouldSucceed) {
            return FlintResult.Error(
                AppError.AiProvider(
                    message = "Provider $providerName failed to generate response",
                    provider = providerName
                )
            )
        }
        return FlintResult.Success(
            AiResponse(
                content = "Generated response from $providerName for prompt: ${request.prompt}",
                providerUsed = providerName,
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override suspend fun isHealthy(): Boolean = shouldSucceed
}
