package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.core.FlintLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ultra-lenient JSON parser and repair engine for Flint AI.
 * Designed specifically to handle free/imperfect LLMs that produce hallucinated keys,
 * markdown chain-of-thought preambles, smart quotes, single quotes, trailing commas,
 * unquoted keys, or plain markdown bullet points.
 */
object LenientJsonParser {

    private val tag = "LenientJsonParser"

    val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Sanitizes raw LLM output by removing codeblock wrappers, single-quote braces,
     * smart quotes, inline comments, and trailing commas.
     */
    fun sanitize(raw: String): String {
        if (raw.isBlank()) return ""

        var s = raw.trim()

        // 1. Remove markdown code blocks (```json ... ``` or ``` ...)
        s = s.replace(Regex("""^```(?:json|javascript|text)?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*```$"""), "")
            .trim()

        // 2. Normalize smart quotes to standard ASCII quotes
        s = s.replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('„', '"')
            .replace('«', '"')
            .replace('»', '"')

        // 3. Remove single-line JS comments (// ...)
        s = s.replace(Regex("""//.*"""), "")

        // 4. Fix trailing commas before } or ]
        s = s.replace(Regex(""",\s*([\}\]])"""), "$1")

        // 5. Extract main JSON object/array substring if surrounded by preambles/postambles
        val firstBrace = s.indexOf('{')
        val firstBracket = s.indexOf('[')

        val startIdx = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> -1
        }

        if (startIdx != -1) {
            val isObject = s[startIdx] == '{'
            val lastEndIdx = if (isObject) s.lastIndexOf('}') else s.lastIndexOf(']')

            s = if (lastEndIdx > startIdx) {
                s.substring(startIdx, lastEndIdx + 1)
            } else {
                repairIncompleteJson(s.substring(startIdx))
            }
        }

        return s.trim()
    }

    /**
     * Balances unclosed brackets/braces for truncated LLM responses.
     */
    fun repairIncompleteJson(partial: String): String {
        var trimmed = partial.trim().replace(Regex("""[,\s]+$"""), "")
        val openBraces = trimmed.count { it == '{' } - trimmed.count { it == '}' }
        val openBrackets = trimmed.count { it == '[' } - trimmed.count { it == ']' }
        val sb = StringBuilder(trimmed)
        repeat(openBrackets.coerceAtLeast(0)) { sb.append("]") }
        repeat(openBraces.coerceAtLeast(0)) { sb.append("}") }
        return sb.toString()
    }

    /**
     * Dynamically finds a string value in a JsonObject matching any key alias (case-insensitive).
     */
    fun extractString(obj: JsonObject, vararg keyAliases: String): String {
        for (alias in keyAliases) {
            val key = obj.keys.firstOrNull { it.equals(alias, ignoreCase = true) }
            if (key != null) {
                val elem = obj[key]
                if (elem != null) {
                    try {
                        val text = elem.jsonPrimitive.content
                        if (text.isNotBlank()) return text
                    } catch (_: Exception) {}
                }
            }
        }
        return ""
    }

    /**
     * Dynamically finds a list of strings in a JsonObject matching any key alias.
     * Coerces single strings or arrays of strings into a List<String>.
     */
    fun extractStringList(obj: JsonObject, vararg keyAliases: String): List<String> {
        for (alias in keyAliases) {
            val key = obj.keys.firstOrNull { it.equals(alias, ignoreCase = true) }
            if (key != null) {
                val elem = obj[key]
                if (elem != null) {
                    try {
                        val array = elem.jsonArray
                        return array.mapNotNull {
                            try { it.jsonPrimitive.content } catch (_: Exception) { null }
                        }.filter { it.isNotBlank() }
                    } catch (_: Exception) {
                        try {
                            val singleText = elem.jsonPrimitive.content
                            if (singleText.isNotBlank()) {
                                return singleText.split(Regex("""[\n,;•·]+"""))
                                    .map { it.trim().removePrefix("-").trim() }
                                    .filter { it.isNotBlank() }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return emptyList()
    }

    /**
     * Lenient Regex Extractor: Searches raw text for key aliases using Regex.
     */
    fun regexExtractString(text: String, vararg keyAliases: String): String {
        for (alias in keyAliases) {
            val regex = Regex(""""?$alias"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
            val match = regex.find(text)?.groupValues?.get(1)
            if (!match.isNullOrBlank()) return match.trim()
        }
        return ""
    }

    /**
     * Lenient Regex Extractor: Searches raw text for array values using Regex.
     */
    fun regexExtractStringList(text: String, vararg keyAliases: String): List<String> {
        for (alias in keyAliases) {
            val arrayRegex = Regex(""""?$alias"\s*:\s*\[\s*([\s\S]*?)\s*\]""", RegexOption.IGNORE_CASE)
            val match = arrayRegex.find(text)?.groupValues?.get(1)
            if (!match.isNullOrBlank()) {
                val items = Regex(""""([^"]+)"""").findAll(match).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
                if (items.isNotEmpty()) return items
            }
        }
        return emptyList()
    }

    /**
     * Plain Markdown Fallback Extractor: Extracts structured sections from markdown text.
     */
    fun markdownExtractSection(text: String, vararg headingAliases: String): List<String> {
        val lines = text.lines()
        val results = mutableListOf<String>()
        var recording = false

        for (line in lines) {
            val trimmed = line.trim()
            val isHeading = trimmed.startsWith("#") || trimmed.startsWith("**") || trimmed.endsWith(":")
            if (isHeading && headingAliases.any { alias -> trimmed.contains(alias, ignoreCase = true) }) {
                recording = true
                if (trimmed.contains(":")) {
                    val afterColon = trimmed.substringAfter(":", "").trim().removePrefix("-").trim()
                    if (afterColon.isNotBlank()) results.add(afterColon)
                }
                continue
            }
            if (recording) {
                if (trimmed.startsWith("#") || (trimmed.endsWith(":") && !trimmed.startsWith("-"))) {
                    break
                }
                val item = trimmed.removePrefix("-").removePrefix("*").removePrefix("•").trim()
                if (item.isNotBlank()) {
                    results.add(item)
                }
            }
        }
        return results
    }
}
