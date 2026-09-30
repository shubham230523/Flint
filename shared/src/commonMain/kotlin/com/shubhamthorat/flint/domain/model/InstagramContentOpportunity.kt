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
            if (jsonString.isBlank()) return emptyList()

            val sanitized = com.shubhamthorat.flint.domain.ai.LenientJsonParser.sanitize(jsonString)

            try {
                if (sanitized.startsWith("[")) {
                    return com.shubhamthorat.flint.domain.ai.LenientJsonParser.lenientJson.decodeFromString<List<InstagramContentOpportunity>>(sanitized)
                } else {
                    val container = com.shubhamthorat.flint.domain.ai.LenientJsonParser.lenientJson.decodeFromString<OpportunityListContainer>(sanitized)
                    if (container.opportunities.isNotEmpty()) return container.opportunities
                }
            } catch (e: Exception) {
                com.shubhamthorat.flint.core.FlintLogger.w("OpportunityListContainer", "Serialization parse error (${e.message}). Executing lenient JsonElement extraction...")
            }

            try {
                val elem = com.shubhamthorat.flint.domain.ai.LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is kotlinx.serialization.json.JsonArray) {
                    return elem.mapNotNull { item ->
                        try { com.shubhamthorat.flint.domain.ai.LenientJsonParser.lenientJson.decodeFromJsonElement(InstagramContentOpportunity.serializer(), item) } catch (_: Exception) { null }
                    }
                } else if (elem is kotlinx.serialization.json.JsonObject) {
                    val oppsKey = elem.keys.firstOrNull { it.equals("opportunities", ignoreCase = true) || it.equals("items", ignoreCase = true) }
                    if (oppsKey != null) {
                        val arr = elem[oppsKey]
                        if (arr is kotlinx.serialization.json.JsonArray) {
                            return arr.mapNotNull { item ->
                                try { com.shubhamthorat.flint.domain.ai.LenientJsonParser.lenientJson.decodeFromJsonElement(InstagramContentOpportunity.serializer(), item) } catch (_: Exception) { null }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            return emptyList()
        }
    }
}
