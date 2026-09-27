package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val stream: Boolean = true
)

@Serializable
private data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenRouterResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>? = null,
    val usage: OpenRouterUsage? = null,
    val error: OpenRouterErrorDetail? = null
)

@Serializable
private data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    val delta: OpenRouterMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenRouterUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)

@Serializable
private data class OpenRouterErrorDetail(
    val message: String? = null,
    val code: Int? = null
)

class OpenRouterProvider(
    apiKey: String? = null,
    modelName: String? = null,
    private val httpClient: HttpClient = createDefaultHttpClient()
) : AiProvider {

    val activeApiKey: String = apiKey?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.OPENROUTER_API_KEY
    val activeModelName: String = modelName?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.OPENROUTER_MODEL_NAME.ifEmpty { "anthropic/claude-3.5-sonnet" }

    override val providerName: String = "OpenRouter ($activeModelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val tag = "OpenRouterProvider"
        val effectiveModel = request.modelName ?: activeModelName

        if (activeApiKey.isBlank()) {
            FlintLogger.e(tag, "OpenRouter API key is missing")
            return FlintResult.Error(
                AppError.AiProvider("OpenRouter API key is missing. Please set 'openrouter.api.key' in local.properties or settings.")
            )
        }

        FlintLogger.i(tag, "Calling OpenRouter Streaming API | Model: $effectiveModel | Endpoint: https://openrouter.ai/api/v1/chat/completions")

        return try {
            val requestBody = OpenRouterRequest(
                model = effectiveModel,
                messages = listOf(
                    OpenRouterMessage(role = "user", content = request.prompt)
                ),
                stream = true
            )

            val statement = httpClient.preparePost("https://openrouter.ai/api/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $activeApiKey")
                header("HTTP-Referer", "https://github.com/shubhamthorat/flint")
                header("X-Title", "Flint AI")
                setBody(requestBody)
            }

            var accumulatedText = ""
            var lastErrorMsg: String? = null

            statement.execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    val responseText = httpResponse.bodyAsText()
                    FlintLogger.e(tag, "OpenRouter API HTTP Error ${httpResponse.status.value}: $responseText")
                    lastErrorMsg = parseErrorMessage(responseText) ?: "HTTP ${httpResponse.status.value}: ${httpResponse.status.description} | $responseText"
                    return@execute
                }

                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<OpenRouterResponse>(data)
                            if (response.error != null) {
                                lastErrorMsg = response.error.message
                                break
                            }
                            val choice = response.choices?.firstOrNull()
                            val chunkText = choice?.delta?.content ?: choice?.message?.content
                            if (!chunkText.isNullOrEmpty()) {
                                sb.append(chunkText)
                            }
                        } catch (e: Exception) {
                            FlintLogger.w(tag, "Could not parse OpenRouter SSE chunk: $data | ${e.message}")
                        }
                    }
                }
                accumulatedText = sb.toString()
            }

            if (lastErrorMsg != null) {
                return FlintResult.Error(AppError.AiProvider("OpenRouter API Error: $lastErrorMsg"))
            }

            if (accumulatedText.isBlank()) {
                FlintLogger.e(tag, "OpenRouter API returned empty response from stream")
                return FlintResult.Error(AppError.AiProvider("OpenRouter API returned an empty response."))
            }

            FlintLogger.i(tag, "OpenRouter Stream Success! Received ${accumulatedText.length} chars")
            FlintResult.Success(
                AiResponse(
                    content = accumulatedText,
                    providerUsed = "OpenRouter REST API ($effectiveModel)",
                    tokensUsed = request.prompt.length * 2
                )
            )
        } catch (e: Exception) {
            FlintLogger.e(tag, "Exception calling OpenRouter API: ${e.message}")
            FlintResult.Error(
                AppError.AiProvider("OpenRouter connection failed: ${e.message ?: "Unknown error"}")
            )
        }
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        val tag = "OpenRouterProvider[Stream]"
        val effectiveModel = request.modelName ?: activeModelName

        if (activeApiKey.isBlank()) {
            emit(FlintResult.Error(AppError.AiProvider("OpenRouter API key is missing.")))
            return@flow
        }

        try {
            val requestBody = OpenRouterRequest(
                model = effectiveModel,
                messages = listOf(
                    OpenRouterMessage(role = "user", content = request.prompt)
                ),
                stream = true
            )

            val statement = httpClient.preparePost("https://openrouter.ai/api/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $activeApiKey")
                header("HTTP-Referer", "https://github.com/shubhamthorat/flint")
                header("X-Title", "Flint AI")
                setBody(requestBody)
            }

            var hasEmitted = false

            statement.execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    val responseText = httpResponse.bodyAsText()
                    val errorMessage = parseErrorMessage(responseText) ?: "HTTP ${httpResponse.status.value}: ${httpResponse.status.description}"
                    return@execute
                }

                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<OpenRouterResponse>(data)
                            val choice = response.choices?.firstOrNull()
                            val chunkText = choice?.delta?.content ?: choice?.message?.content
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
            emit(FlintResult.Error(AppError.AiProvider("OpenRouter stream failed: ${e.message}")))
        }
    }

    override suspend fun isHealthy(): Boolean {
        return activeApiKey.isNotBlank()
    }

    private fun parseErrorMessage(jsonText: String): String? {
        return try {
            val response = jsonParser.decodeFromString<OpenRouterResponse>(jsonText)
            response.error?.message
        } catch (_: Exception) {
            null
        }
    }

    companion object {
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
    }
}
