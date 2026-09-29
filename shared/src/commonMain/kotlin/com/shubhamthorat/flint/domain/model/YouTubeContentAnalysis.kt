package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class YouTubeContentAnalysis(
    val summary: String,
    val mainTopics: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val notableQuotes: List<String> = emptyList(),
    val potentialHooks: List<String> = emptyList(),
    val audience: String = "",
    val contentThemes: List<String> = emptyList()
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parseFromJson(jsonString: String): YouTubeContentAnalysis? {
            return try {
                // Remove potential markdown code blocks like ```json ... ```
                val cleanedJson = jsonString
                    .replace(Regex("""^```(?:json)?\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*```$"""), "")
                    .trim()

                json.decodeFromString<YouTubeContentAnalysis>(cleanedJson)
            } catch (_: Exception) {
                null
            }
        }
    }
}
