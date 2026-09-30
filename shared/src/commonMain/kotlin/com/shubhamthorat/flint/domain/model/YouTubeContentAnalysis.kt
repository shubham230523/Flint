package com.shubhamthorat.flint.domain.model

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.LenientJsonParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
            if (jsonString.isBlank()) return null

            val sanitized = LenientJsonParser.sanitize(jsonString)

            // Tier 1: Strict Kotlinx Serialization
            try {
                return LenientJsonParser.lenientJson.decodeFromString<YouTubeContentAnalysis>(sanitized)
            } catch (e: Exception) {
                FlintLogger.w("YouTubeContentAnalysis", "Tier 1 parse failed (${e.message}). Trying Tier 2 JsonElement extraction...")
            }

            // Tier 2: JsonElement Inspection with Key Aliases
            try {
                val jsonElem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (jsonElem is JsonObject) {
                    val summary = LenientJsonParser.extractString(jsonElem, "summary", "overview", "description")
                    val mainTopics = LenientJsonParser.extractStringList(jsonElem, "mainTopics", "topics", "main_topics")
                    val keyPoints = LenientJsonParser.extractStringList(jsonElem, "keyPoints", "key_points", "takeaways", "keyTakeaways")
                    val notableQuotes = LenientJsonParser.extractStringList(jsonElem, "notableQuotes", "notable_quotes", "quotes")
                    val potentialHooks = LenientJsonParser.extractStringList(jsonElem, "potentialHooks", "potential_hooks", "hooks")
                    val audience = LenientJsonParser.extractString(jsonElem, "audience", "targetAudience", "target_audience")
                    val contentThemes = LenientJsonParser.extractStringList(jsonElem, "contentThemes", "content_themes", "themes")

                    if (summary.isNotBlank() || mainTopics.isNotEmpty() || keyPoints.isNotEmpty()) {
                        return YouTubeContentAnalysis(
                            summary = summary.ifBlank { "Video analysis" },
                            mainTopics = mainTopics,
                            keyPoints = keyPoints,
                            notableQuotes = notableQuotes,
                            potentialHooks = potentialHooks,
                            audience = audience,
                            contentThemes = contentThemes
                        )
                    }
                }
            } catch (e: Exception) {
                FlintLogger.w("YouTubeContentAnalysis", "Tier 2 parse failed (${e.message}). Trying Tier 3 Regex extraction...")
            }

            // Tier 3: Regex Field Extractor
            try {
                val summary = LenientJsonParser.regexExtractString(jsonString, "summary", "overview", "description")
                val mainTopics = LenientJsonParser.regexExtractStringList(jsonString, "mainTopics", "topics", "main_topics")
                val keyPoints = LenientJsonParser.regexExtractStringList(jsonString, "keyPoints", "key_points", "takeaways")
                val notableQuotes = LenientJsonParser.regexExtractStringList(jsonString, "notableQuotes", "quotes")
                val potentialHooks = LenientJsonParser.regexExtractStringList(jsonString, "potentialHooks", "hooks")
                val audience = LenientJsonParser.regexExtractString(jsonString, "audience", "targetAudience")
                val contentThemes = LenientJsonParser.regexExtractStringList(jsonString, "contentThemes", "themes")

                if (summary.isNotBlank() || mainTopics.isNotEmpty() || keyPoints.isNotEmpty()) {
                    return YouTubeContentAnalysis(
                        summary = summary.ifBlank { "Video analysis" },
                        mainTopics = mainTopics,
                        keyPoints = keyPoints,
                        notableQuotes = notableQuotes,
                        potentialHooks = potentialHooks,
                        audience = audience,
                        contentThemes = contentThemes
                    )
                }
            } catch (e: Exception) {
                FlintLogger.w("YouTubeContentAnalysis", "Tier 3 parse failed (${e.message}). Trying Tier 4 Markdown fallback...")
            }

            // Tier 4: Plain Markdown / Text Heading Extractor
            val summaryText = LenientJsonParser.markdownExtractSection(jsonString, "summary", "overview", "description").joinToString("\n")
            val mainTopics = LenientJsonParser.markdownExtractSection(jsonString, "main topics", "topics")
            val keyPoints = LenientJsonParser.markdownExtractSection(jsonString, "key points", "key takeaways", "takeaways")
            val notableQuotes = LenientJsonParser.markdownExtractSection(jsonString, "notable quotes", "quotes")
            val potentialHooks = LenientJsonParser.markdownExtractSection(jsonString, "potential hooks", "hooks")
            val audienceText = LenientJsonParser.markdownExtractSection(jsonString, "audience", "target audience").joinToString("\n")
            val contentThemes = LenientJsonParser.markdownExtractSection(jsonString, "content themes", "themes")

            if (summaryText.isBlank() && mainTopics.isEmpty() && keyPoints.isEmpty() && notableQuotes.isEmpty()) {
                return null
            }

            return YouTubeContentAnalysis(
                summary = summaryText,
                mainTopics = mainTopics,
                keyPoints = keyPoints,
                notableQuotes = notableQuotes,
                potentialHooks = potentialHooks,
                audience = audienceText,
                contentThemes = contentThemes
            )
        }
    }
}
