package com.shubhamthorat.flint.domain.ai

import kotlin.text.RegexOption

object AiContentCleaner {

    fun clean(rawContent: String): String {
        if (rawContent.isBlank()) return rawContent

        var content = rawContent.trim()

        // 1. Remove XML/HTML style <think>...</think> blocks
        content = content.replace(Regex("""<think>[\s\S]*?</think>"""), "").trim()

        // 2. Remove markdown thinking codeblocks
        content = content.replace(Regex("""```(?:thinking|thought|reasoning)[\s\S]*?```"""), "").trim()

        // 3. Remove "Analyze the Request", numbered thinking steps (e.g. 1. Analyze the Request:), or "Here's a thinking process:" preambles before JSON or final content
        if (content.contains("Analyze the Request", ignoreCase = true) || content.contains("thinking process", ignoreCase = true) || content.contains("Draft JSON", ignoreCase = true)) {
            val jsonFirstBraceIndex = content.indexOf('{')
            val jsonFirstBracketIndex = content.indexOf('[')
            val jsonStartIndex = when {
                jsonFirstBraceIndex != -1 && jsonFirstBracketIndex != -1 -> minOf(jsonFirstBraceIndex, jsonFirstBracketIndex)
                jsonFirstBraceIndex != -1 -> jsonFirstBraceIndex
                jsonFirstBracketIndex != -1 -> jsonFirstBracketIndex
                else -> -1
            }

            if (jsonStartIndex > 0) {
                content = content.substring(jsonStartIndex).trim()
            } else {
                val splitRegex = Regex("""\n(?=\*\*|#|🧵|Tweet 1|1/|1\.\s|Slide 1|Subject:|\[HOOK\]|🚀|\{)""")
                val parts = content.split(splitRegex)
                if (parts.size > 1 && (parts[0].contains("thinking process", ignoreCase = true) || parts[0].contains("Analyze the Request", ignoreCase = true))) {
                    content = parts.drop(1).joinToString("\n").trim()
                } else {
                    content = content.replace(Regex("""^(Here's a thinking process:|Thinking Process:|### Thinking Process)""", RegexOption.IGNORE_CASE), "").trim()
                }
            }
        }

        return content.ifBlank { rawContent.trim() }
    }
}
