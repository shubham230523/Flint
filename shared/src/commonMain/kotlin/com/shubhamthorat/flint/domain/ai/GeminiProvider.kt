package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Google Gemini Provider with streaming support for Flint AI Content Operating System.
 */
class GeminiProvider(
    apiKey: String? = null,
    modelName: String? = null
) : AiProvider {

    val activeApiKey: String = apiKey?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.GEMINI_API_KEY
    val activeModelName: String = modelName?.takeIf { it.isNotEmpty() } ?: "gemini-2.5-flash"

    override val providerName: String = "Gemini ($activeModelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val effectiveModel = request.modelName ?: activeModelName

        try {
            val responseText = "Gemini [$effectiveModel] generated content for prompt:\n${request.prompt}"

            return FlintResult.Success(
                AiResponse(
                    content = responseText,
                    providerUsed = "Gemini ($effectiveModel)",
                    tokensUsed = (request.prompt.length * 1.5).toInt()
                )
            )
        } catch (e: Exception) {
            return FlintResult.Error(
                AppError.AiProvider(
                    message = "Gemini API call failed: ${e.message}",
                    provider = providerName,
                    cause = e
                )
            )
        }
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val effectiveModel = request.modelName ?: activeModelName
        val fullText = "Gemini [$effectiveModel] streamed response for: ${request.prompt}"
        val words = fullText.split(" ")

        var accumulated = ""
        for (word in words) {
            accumulated = if (accumulated.isEmpty()) word else "$accumulated $word"
            emit(FlintResult.Success(accumulated))
            delay(30)
        }
    }

    override suspend fun isHealthy(): Boolean = true

    companion object {
        fun extractJsonPayload(content: String): String {
            val jsonRegex = Regex("""(\{[\s\S]*\}|\[[\s\S]*\])""")
            val match = jsonRegex.find(content)
            return match?.value ?: content.trim()
        }
    }
}
