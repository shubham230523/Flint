package com.shubhamthorat.flint.domain.ai

object AiContentCleaner {

    fun clean(rawContent: String): String {
        if (rawContent.isBlank()) return rawContent

        var content = rawContent.trim()

        // 1. Remove XML/HTML style <think>...</think> blocks
        content = content.replace(Regex("""(?s)<think>[\s\S]*?</think>"""), "").trim()

        // 2. Remove markdown thinking codeblocks
        content = content.replace(Regex("""(?s)```(?:thinking|thought|reasoning)[\s\S]*?```"""), "").trim()

        // 3. Remove "Here's a thinking process:" or "Analyze the Request:" preamble blocks
        if (content.contains("thinking process", ignoreCase = true) || content.contains("Analyze the Request", ignoreCase = true)) {
            val splitRegex = Regex("""\n(?=\*\*|#|🧵|Tweet 1|1/|1\.\s|Slide 1|Subject:|\[HOOK\]|🚀)""")
            val parts = content.split(splitRegex)

            if (parts.size > 1 && (parts[0].contains("thinking process", ignoreCase = true) || parts[0].contains("Analyze the Request", ignoreCase = true))) {
                content = parts.drop(1).joinToString("\n").trim()
            } else {
                content = content.replace(Regex("""(?i)^(Here's a thinking process:|Thinking Process:|### Thinking Process)"""), "").trim()
            }
        }

        return content.ifBlank { rawContent.trim() }
    }
}
