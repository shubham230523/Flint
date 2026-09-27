package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse

class GeminiProvider(
    private val apiKey: String? = null
) : AiProvider {

    override val providerName: String = "Gemini"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        if (apiKey.isNull_or_Empty()) {
            return FlintResult.Success(
                AiResponse(
                    content = "Gemini AI response for prompt: ${request.prompt}",
                    providerUsed = providerName,
                    tokensUsed = request.prompt.length * 2
                )
            )
        }
        return FlintResult.Success(
            AiResponse(
                content = "Gemini response for prompt: ${request.prompt}",
                providerUsed = providerName,
                tokensUsed = request.prompt.length * 2
            )
        )
    }

    override suspend fun isHealthy(): Boolean = true
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()
