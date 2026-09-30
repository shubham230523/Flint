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
import io.ktor.client.request.post
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
    val messages: List<OpenRouterMessageRequest>,
    val stream: Boolean = false
)

@Serializable
private data class OpenRouterMessageRequest(
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
    val message: OpenRouterMessageResponse? = null,
    val delta: OpenRouterMessageResponse? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
private data class OpenRouterMessageResponse(
    val role: String? = null,
    val content: String? = null,
    val reasoning: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
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
    val activeModelName: String = normalizeModelName(
        modelName?.takeIf { it.isNotEmpty() } ?: FlintBuildConfig.OPENROUTER_MODEL_NAME.ifEmpty { "google/gemini-2.0-flash-exp:free" }
    )

    override val providerName: String = "OpenRouter ($activeModelName)"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        val tag = "OpenRouterProvider"
        val effectiveModel = normalizeModelName(request.modelName ?: activeModelName)

        if (activeApiKey.isBlank()) {
            FlintLogger.e(tag, "OpenRouter API key is missing")
            return FlintResult.Error(
                AppError.AiProvider("OpenRouter API key is missing. Please set 'openrouter.api.key' in local.properties or settings.")
            )
        }

        FlintLogger.i(tag, "Calling OpenRouter SSE stream endpoint as default | Model: $effectiveModel")
        return generateFromStream(request, effectiveModel)
    }

    private suspend fun generateFromStream(request: AiRequest, effectiveModel: String): FlintResult<AiResponse, AppError> {
        val tag = "OpenRouterProvider[StreamAcc]"
        return try {
            val requestBody = OpenRouterRequest(
                model = effectiveModel,
                messages = listOf(OpenRouterMessageRequest(role = "user", content = request.prompt)),
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
                    lastErrorMsg = parseErrorMessage(responseText) ?: "HTTP ${httpResponse.status.value}: ${httpResponse.status.description} | $responseText"
                    return@execute
                }

                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<OpenRouterResponse>(data)
                            if (response.error != null) {
                                lastErrorMsg = response.error.message
                                break
                            }
                            val choice = response.choices?.firstOrNull()
                            val chunkText = extractTextFromChoice(choice)
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

            val cleanedText = AiContentCleaner.clean(accumulatedText)

            if (cleanedText.isBlank()) {
                FlintLogger.e(tag, "OpenRouter API stream returned blank text")
                return FlintResult.Error(AppError.AiProvider("OpenRouter API returned an empty response."))
            }

            FlintResult.Success(
                AiResponse(
                    content = cleanedText,
                    providerUsed = "OpenRouter REST API ($effectiveModel)",
                    tokensUsed = request.prompt.length * 2
                )
            )
        } catch (e: Exception) {
            FlintLogger.e(tag, "Exception in generateFromStream: ${e.message}")
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
                messages = listOf(OpenRouterMessageRequest(role = "user", content = request.prompt)),
                stream = true
            )

            FlintLogger.i(tag, "Connecting to OpenRouter SSE stream | Model: $effectiveModel | Timeout limit: 300s")

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
                    FlintLogger.e(tag, "Stream Error $errorMessage")
                    return@execute
                }

                FlintLogger.i(tag, "OpenRouter SSE stream connected! Waiting for $effectiveModel tokens...")
                val channel = httpResponse.bodyAsChannel()
                val sb = StringBuilder()
                var chunkCount = 0

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") {
                            FlintLogger.i(tag, "Received [DONE] signal from OpenRouter SSE stream. Total chars: ${sb.length}")
                            break
                        }
                        if (data.isBlank()) continue

                        try {
                            val response = jsonParser.decodeFromString<OpenRouterResponse>(data)
                            val choice = response.choices?.firstOrNull()
                            val chunkText = extractTextFromChoice(choice)
                            if (!chunkText.isNullOrEmpty()) {
                                sb.append(chunkText)
                                chunkCount++
                                if (chunkCount % 5 == 0 || sb.length < 100) {
                                    FlintLogger.d(tag, "Stream chunk #$chunkCount (${chunkText.length} chars) | Total buffer: ${sb.length} chars")
                                }
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

    private fun extractTextFromChoice(choice: OpenRouterChoice?): String? {
        if (choice == null) return null
        val delta = choice.delta
        val msg = choice.message

        return delta?.content?.takeIf { it.isNotEmpty() }
            ?: delta?.reasoningContent?.takeIf { it.isNotEmpty() }
            ?: delta?.reasoning?.takeIf { it.isNotEmpty() }
            ?: msg?.content?.takeIf { it.isNotEmpty() }
            ?: msg?.reasoningContent?.takeIf { it.isNotEmpty() }
            ?: msg?.reasoning?.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun normalizeModelName(rawModel: String): String {
            val trimmed = rawModel.trim()
            return if (trimmed.isBlank()) "nvidia/nemotron-3.5-lightning:free" else trimmed
        }

        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        private fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(jsonParser)
                }
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 300_000L
                    connectTimeoutMillis = 60_000L
                    socketTimeoutMillis = 300_000L
                }
            }
        }
    }
}
