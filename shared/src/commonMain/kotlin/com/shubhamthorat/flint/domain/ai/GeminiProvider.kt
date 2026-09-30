package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorDetail? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class GeminiErrorDetail(
    val message: String? = null,
    val code: Int? = null
)

/**
 * Google Gemini Provider for Flint AI Content Operating System with SSE Streaming.
 */
class GeminiProvider(
    apiKey: String? = null,
    modelName: String? = null,
    private val httpClient: HttpClient = createDefaultHttpClient()
) : AiProvider {

    val activeApiKey: String = apiKey?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.GEMINI_API_KEY
    val activeModelName: String = normalizeModelName(
        modelName?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.GEMINI_MODEL_NAME.ifEmpty { "gemini-2.0-flash" }
    )

    override val providerName: String = "Gemini ($activeModelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val tag = "GeminiProvider"
        val effectiveModel = normalizeModelName(request.modelName ?: activeModelName)

        if (activeApiKey.isBlank()) {
            FlintLogger.e(tag, "Gemini API key is missing")
            return FlintResult.Error(
                AppError.AiProvider("Gemini API key is missing. Please set 'gemini.api.key' in local.properties or settings.")
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:streamGenerateContent?alt=sse&key=$activeApiKey"
        FlintLogger.i(tag, "Calling Gemini Streaming API | Model: $effectiveModel")

        return try {
            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = request.prompt)))
                )
            )

            val statement = httpClient.preparePost(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            var accumulatedText = ""
            var lastErrorMsg: String? = null

            statement.execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    val responseText = httpResponse.bodyAsText()
                    FlintLogger.e(tag, "Gemini API HTTP Error ${httpResponse.status.value}: $responseText")
                    lastErrorMsg = parseErrorMessage(responseText) ?: "HTTP ${httpResponse.status.value}: ${httpResponse.status.description}"
                    return@execute
                }

                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<GeminiResponse>(data)
                            if (response.error != null) {
                                lastErrorMsg = response.error.message
                                break
                            }
                            val candidate = response.candidates?.firstOrNull()
                            val chunkText = candidate?.content?.parts?.firstOrNull()?.text
                            if (!chunkText.isNullOrEmpty()) {
                                sb.append(chunkText)
                            }
                        } catch (e: Exception) {
                            FlintLogger.w(tag, "Could not parse Gemini SSE chunk: $data | ${e.message}")
                        }
                    }
                }
                accumulatedText = sb.toString()
            }

            if (lastErrorMsg != null) {
                return FlintResult.Error(AppError.AiProvider("Gemini API Error: $lastErrorMsg"))
            }

            if (accumulatedText.isBlank()) {
                FlintLogger.e(tag, "Gemini API returned empty text from stream")
                return FlintResult.Error(AppError.AiProvider("Gemini API returned an empty response."))
            }

            FlintResult.Success(
                AiResponse(
                    content = accumulatedText,
                    providerUsed = "Gemini REST API ($effectiveModel)",
                    tokensUsed = (request.prompt.length * 1.5).toInt()
                )
            )
        } catch (e: Exception) {
            FlintLogger.e(tag, "Exception calling Gemini API: ${e.message}")
            FlintResult.Error(
                AppError.AiProvider("Gemini connection failed: ${e.message ?: "Unknown error"}")
            )
        }
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val effectiveModel = request.modelName ?: activeModelName

        if (activeApiKey.isBlank()) {
            emit(FlintResult.Error(AppError.AiProvider("Gemini API key is missing.")))
            return@flow
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:streamGenerateContent?alt=sse&key=$activeApiKey"

        try {
            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = request.prompt)))
                )
            )

            val statement = httpClient.preparePost(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            var hasEmitted = false

            statement.execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    return@execute
                }

                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<GeminiResponse>(data)
                            val candidate = response.candidates?.firstOrNull()
                            val chunkText = candidate?.content?.parts?.firstOrNull()?.text
                            if (!chunkText.isNullOrEmpty()) {
                                sb.append(chunkText)
                                emit(FlintResult.Success(sb.toString()))
                                hasEmitted = true
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            if (!hasEmitted) {
                when (val res = generate(request)) {
                    is FlintResult.Success -> emit(FlintResult.Success(res.data.content))
                    is FlintResult.Error -> emit(res)
                }
            }
        } catch (e: Exception) {
            emit(FlintResult.Error(AppError.AiProvider("Gemini stream failed: ${e.message}")))
        }
    }

    override suspend fun isHealthy(): Boolean {
        return activeApiKey.isNotBlank()
    }

    private fun parseErrorMessage(jsonText: String): String? {
        return try {
            val response = jsonParser.decodeFromString<GeminiResponse>(jsonText)
            response.error?.message
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun normalizeModelName(rawModel: String): String {
            val trimmed = rawModel.trim()
            return when (trimmed.lowercase()) {
                "gemini-1.5-flash" -> "gemini-2.0-flash"
                "gemini-3.5-flash-lite", "gemini-3.5-flash" -> "gemini-2.0-flash"
                else -> if (trimmed.isBlank()) "gemini-2.0-flash" else trimmed
            }
        }

        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(jsonParser)
                }
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 120_000L
                    connectTimeoutMillis = 30_000L
                    socketTimeoutMillis = 120_000L
                }
            }
        }

        fun extractJsonPayload(content: String): String {
            val jsonRegex = Regex("""(\{[\s\S]*\}|\[[\s\S]*\])""")
            val match = jsonRegex.find(content)
            return match?.value ?: content.trim()
        }
    }
}
