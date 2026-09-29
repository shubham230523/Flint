package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAiProvider(
    override val providerName: String = "OpenRouter",
    private val shouldSucceed: Boolean = true,
    private val fixedResponseText: String? = null,
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
        val text = fixedResponseText ?: "Generated response from $providerName for prompt: ${request.prompt}"
        return FlintResult.Success(
            AiResponse(
                content = text,
                providerUsed = providerName,
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        if (!shouldSucceed) {
            emit(
                FlintResult.Error(
                    AppError.AiProvider(
                        message = "Provider $providerName streaming failed",
                        provider = providerName
                    )
                )
            )
        } else {
            emit(FlintResult.Success("Generated response from $providerName for prompt: ${request.prompt}"))
        }
    }

    override suspend fun isHealthy(): Boolean = shouldSucceed
}
