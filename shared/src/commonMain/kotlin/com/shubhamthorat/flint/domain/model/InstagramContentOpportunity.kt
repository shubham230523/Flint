package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class OpportunityType {
    REEL_IDEA,
    CAROUSEL,
    STORY_SEQUENCE,
    QUOTE_POST,
    EDUCATIONAL_POST,
    QUESTION_POST
}

@Serializable
data class InstagramContentOpportunity(
    val id: String,
    val type: OpportunityType,
    val title: String,
    val description: String,
    val sourceReference: String = "",
    val suggestedHook: String = ""
)

@Serializable
data class OpportunityListContainer(
    val opportunities: List<InstagramContentOpportunity> = emptyList()
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun parseFromJson(jsonString: String): List<InstagramContentOpportunity> {
            return try {
                val cleanedJson = jsonString
                    .replace(Regex("""^```(?:json)?\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*```$"""), "")
                    .trim()

                if (cleanedJson.startsWith("[")) {
                    json.decodeFromString<List<InstagramContentOpportunity>>(cleanedJson)
                } else {
                    json.decodeFromString<OpportunityListContainer>(cleanedJson).opportunities
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
