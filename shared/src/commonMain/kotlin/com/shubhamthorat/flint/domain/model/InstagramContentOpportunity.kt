package com.shubhamthorat.flint.domain.model

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.LenientJsonParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
enum class OpportunityType {
    REEL_IDEA,
    CAROUSEL,
    STORY_SEQUENCE,
    QUOTE_POST,
    EDUCATIONAL_POST,
    QUESTION_POST;

    companion object {
        fun parse(typeStr: String): OpportunityType {
            val norm = typeStr.trim().uppercase().replace(" ", "_").replace("-", "_")
            return when {
                norm.contains("REEL") -> REEL_IDEA
                norm.contains("CAROUSEL") -> CAROUSEL
                norm.contains("STORY") -> STORY_SEQUENCE
                norm.contains("QUOTE") -> QUOTE_POST
                norm.contains("EDUCATIONAL") || norm.contains("EDU") -> EDUCATIONAL_POST
                norm.contains("QUESTION") || norm.contains("Q_AND_A") -> QUESTION_POST
                else -> REEL_IDEA
            }
        }
    }
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
        fun parseFromJson(jsonString: String): List<InstagramContentOpportunity> {
            if (jsonString.isBlank()) return emptyList()

            val sanitized = LenientJsonParser.sanitize(jsonString)

            // Tier 1: Strict Kotlinx Serialization
            try {
                if (sanitized.startsWith("[")) {
                    return LenientJsonParser.lenientJson.decodeFromString<List<InstagramContentOpportunity>>(sanitized)
                } else {
                    val container = LenientJsonParser.lenientJson.decodeFromString<OpportunityListContainer>(sanitized)
                    if (container.opportunities.isNotEmpty()) return container.opportunities
                }
            } catch (e: Exception) {
                FlintLogger.w("OpportunityListContainer", "Tier 1 parse failed (${e.message}). Executing Tier 2 JsonElement extraction...")
            }

            // Tier 2: JsonElement Manual Inspection & Enum Normalization
            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                val array = when {
                    elem is JsonArray -> elem
                    elem is JsonObject -> {
                        val key = elem.keys.firstOrNull { it.equals("opportunities", ignoreCase = true) || it.equals("items", ignoreCase = true) }
                        if (key != null) elem[key] as? JsonArray else null
                    }
                    else -> null
                }

                if (array != null) {
                    val list = mutableListOf<InstagramContentOpportunity>()
                    array.forEachIndexed { idx, item ->
                        if (item is JsonObject) {
                            val id = LenientJsonParser.extractString(item, "id").ifBlank { "opp_${idx + 1}" }
                            val typeRaw = LenientJsonParser.extractString(item, "type", "opportunityType", "category")
                            val title = LenientJsonParser.extractString(item, "title", "name", "headline")
                            val description = LenientJsonParser.extractString(item, "description", "body", "details", "summary")
                            val sourceRef = LenientJsonParser.extractString(item, "sourceReference", "source_reference", "source")
                            val hook = LenientJsonParser.extractString(item, "suggestedHook", "suggested_hook", "hook")

                            if (title.isNotBlank() || description.isNotBlank()) {
                                list.add(
                                    InstagramContentOpportunity(
                                        id = id,
                                        type = OpportunityType.parse(typeRaw),
                                        title = title.ifBlank { "Instagram Post Idea" },
                                        description = description.ifBlank { title },
                                        sourceReference = sourceRef,
                                        suggestedHook = hook
                                    )
                                )
                            }
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            } catch (e: Exception) {
                FlintLogger.w("OpportunityListContainer", "Tier 2 parse failed (${e.message}). Executing Tier 3 Regex extraction...")
            }

            // Tier 3: Regex Field Extractor
            try {
                val objectRegex = Regex("""\{\s*"id"[\s\S]*?\}""", RegexOption.IGNORE_CASE)
                val matches = objectRegex.findAll(sanitized).toList()
                val list = mutableListOf<InstagramContentOpportunity>()
                matches.forEachIndexed { idx, match ->
                    val block = match.value
                    val title = LenientJsonParser.regexExtractString(block, "title", "name")
                    val desc = LenientJsonParser.regexExtractString(block, "description", "body")
                    val typeRaw = LenientJsonParser.regexExtractString(block, "type")
                    val hook = LenientJsonParser.regexExtractString(block, "suggestedHook", "hook")

                    if (title.isNotBlank() || desc.isNotBlank()) {
                        list.add(
                            InstagramContentOpportunity(
                                id = "opp_${idx + 1}",
                                type = OpportunityType.parse(typeRaw),
                                title = title.ifBlank { "Content Opportunity" },
                                description = desc.ifBlank { title },
                                suggestedHook = hook
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}

            return emptyList()
        }
    }
}
