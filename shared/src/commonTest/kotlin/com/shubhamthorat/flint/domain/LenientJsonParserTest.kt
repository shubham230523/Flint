package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.LenientJsonParser
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LenientJsonParserTest {

    @Test
    fun testLenientJsonSanitizeSmartQuotesAndTrailingCommas() {
        val messyJson = """
            ```json
            // Here is the json output:
            {
              “summary”: “This is a test summary”,
              “mainTopics”: [“KMP”, “Compose”,],
              “keyPoints”: [“Point 1”, “Point 2”],
            }
            ```
        """.trimIndent()

        val sanitized = LenientJsonParser.sanitize(messyJson)
        val parsed = YouTubeContentAnalysis.parseFromJson(sanitized)

        assertNotNull(parsed)
        assertEquals("This is a test summary", parsed.summary)
        assertEquals(2, parsed.mainTopics.size)
        assertEquals("KMP", parsed.mainTopics[0])
    }

    @Test
    fun testLenientJsonParseWithReasoningPreambleAndKeyAliases() {
        val NemotronOutput = """
            1. **Analyze the Request:**
            Let's extract information and draft JSON.
            
            ```json
            {
              "overview": "Flint Multiplatform System",
              "topics": ["KMP", "AI Gateway"],
              "takeaways": ["Ground AI in facts", "Preserve Brand DNA"],
              "quotes": ["One spark. Endless stories."],
              "hooks": ["Stop writing business logic twice"],
              "target_audience": "Developers & Creators",
              "themes": ["Architecture", "AI"]
            }
            ```
            Hope this helps!
        """.trimIndent()

        val parsed = YouTubeContentAnalysis.parseFromJson(NemotronOutput)

        assertNotNull(parsed)
        assertEquals("Flint Multiplatform System", parsed.summary)
        assertEquals(2, parsed.mainTopics.size)
        assertEquals("KMP", parsed.mainTopics[0])
        assertEquals("Developers & Creators", parsed.audience)
    }

    @Test
    fun testLenientJsonParseTruncatedJsonRepair() {
        val truncatedJson = """
            {
              "summary": "Truncated analysis text",
              "mainTopics": ["KMP", "Compose"],
              "keyPoints": ["Takeaway 1"
        """.trimIndent()

        val parsed = YouTubeContentAnalysis.parseFromJson(truncatedJson)

        assertNotNull(parsed)
        assertEquals("Truncated analysis text", parsed.summary)
        assertEquals(2, parsed.mainTopics.size)
    }

    @Test
    fun testLenientJsonParsePlainMarkdownFallback() {
        val markdownText = """
            ### Summary
            This video is an overview of Flint KMP architecture.

            ### Main Topics
            - Kotlin Multiplatform
            - Compose Multiplatform
            - AI Gateway

            ### Key Takeaways
            - Ground AI in source facts
            - Preserve Creator DNA
        """.trimIndent()

        val parsed = YouTubeContentAnalysis.parseFromJson(markdownText)

        assertNotNull(parsed)
        assertEquals("This video is an overview of Flint KMP architecture.", parsed.summary)
        assertEquals(3, parsed.mainTopics.size)
        assertEquals("Kotlin Multiplatform", parsed.mainTopics[0])
        assertEquals(2, parsed.keyPoints.size)
    }
}
