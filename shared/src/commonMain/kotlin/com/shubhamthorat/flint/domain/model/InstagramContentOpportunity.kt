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
    val suggestedHook: String = "",
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null
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
                val list = if (sanitized.startsWith("[")) {
                    LenientJsonParser.lenientJson.decodeFromString<List<InstagramContentOpportunity>>(sanitized)
                } else {
                    LenientJsonParser.lenientJson.decodeFromString<OpportunityListContainer>(sanitized).opportunities
                }
                val filtered = list.filterNot { isTemplatePlaceholder(it) }.distinctBy { it.title.trim().lowercase() }
                if (filtered.isNotEmpty()) return filtered
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
                            val startMs = LenientJsonParser.extractString(item, "startTimeMs", "start_time_ms", "startMs").toLongOrNull()
                            val endMs = LenientJsonParser.extractString(item, "endTimeMs", "end_time_ms", "endMs").toLongOrNull()

                            if (title.isNotBlank() || description.isNotBlank()) {
                                list.add(
                                    InstagramContentOpportunity(
                                        id = id,
                                        type = OpportunityType.parse(typeRaw),
                                        title = title.ifBlank { "Instagram Post Idea" },
                                        description = description.ifBlank { title },
                                        sourceReference = sourceRef,
                                        suggestedHook = hook,
                                        startTimeMs = startMs,
                                        endTimeMs = endMs
                                    )
                                )
                            }
                        }
                    }
                    val filtered = list.filterNot { isTemplatePlaceholder(it) }.distinctBy { it.title.trim().lowercase() }
                    if (filtered.isNotEmpty()) return filtered
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
                if (list.isNotEmpty()) return list.filterNot { isTemplatePlaceholder(it) }.distinctBy { it.title.trim().lowercase() }
            } catch (_: Exception) {}

            return emptyList()
        }

        fun isTemplatePlaceholder(opp: InstagramContentOpportunity): Boolean {
            val t = opp.title.trim().lowercase()
            val d = opp.description.trim().lowercase()
            val h = opp.suggestedHook.trim().lowercase()
            return t.contains("title of the idea") ||
                   d.contains("what this instagram post is about") ||
                   h.contains("strong opening hook") ||
                   t.contains("specific topic") ||
                   t.contains("topic name") ||
                   d.contains("specific topic") ||
                   d.contains("topic name") ||
                   h.contains("specific topic") ||
                   t == "title"
        }
    }
}
