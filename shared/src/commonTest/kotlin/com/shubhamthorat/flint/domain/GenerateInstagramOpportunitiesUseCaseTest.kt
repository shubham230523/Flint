package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.OpportunityListContainer
import com.shubhamthorat.flint.domain.model.OpportunityType
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramOpportunitiesUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateInstagramOpportunitiesUseCaseTest {

    @Test
    fun testParseOpportunitiesContainerJson() {
        val json = """
            {
              "opportunities": [
                {
                  "id": "opp_1",
                  "type": "REEL_IDEA",
                  "title": "Why KMP is the future",
                  "description": "Short reel highlighting KMP cross platform speed",
                  "sourceReference": "00:45 transcript point",
                  "suggestedHook": "Stop rewriting business logic twice."
                },
                {
                  "id": "opp_2",
                  "type": "CAROUSEL",
                  "title": "5 Rules of Flint AI Gateway",
                  "description": "Carousel explaining multi provider fallback",
                  "sourceReference": "03:12 transcript point",
                  "suggestedHook": "Never rely on a single AI provider."
                }
              ]
            }
        """.trimIndent()

        val parsed = OpportunityListContainer.parseFromJson(json)
        assertEquals(2, parsed.size)
        assertEquals(OpportunityType.REEL_IDEA, parsed[0].type)
        assertEquals("Why KMP is the future", parsed[0].title)
        assertEquals("Stop rewriting business logic twice.", parsed[0].suggestedHook)
        assertEquals(OpportunityType.CAROUSEL, parsed[1].type)
    }

    @Test
    fun testParseOpportunitiesDirectArrayJson() {
        val json = """
            [
              {
                "id": "opp_3",
                "type": "QUOTE_POST",
                "title": "One Spark Quote",
                "description": "Inspiring quote post for story",
                "sourceReference": "Key takeaway",
                "suggestedHook": "One spark. Endless stories."
              }
            ]
        """.trimIndent()

        val parsed = OpportunityListContainer.parseFromJson(json)
        assertEquals(1, parsed.size)
        assertEquals(OpportunityType.QUOTE_POST, parsed[0].type)
    }

    @Test
    fun testGenerateInstagramOpportunitiesSuccess() = runTest {
        val jsonResponse = """
            {
              "opportunities": [
                {
                  "id": "opp_101",
                  "type": "REEL_IDEA",
                  "title": "How Flint handles AI fallback",
                  "description": "Reel showcasing Gemini -> OpenRouter fallback",
                  "sourceReference": "Transcript 02:15",
                  "suggestedHook": "Your AI provider will go down. Here is how to prepare."
                },
                {
                  "id": "opp_102",
                  "type": "STORY_SEQUENCE",
                  "title": "Behind the scenes of YouTube ingestion",
                  "description": "Story sequence explaining transcript extraction",
                  "sourceReference": "Transcript 05:30",
                  "suggestedHook": "How do you turn a 10 min YouTube video into 5 posts?"
                }
              ]
            }
        """.trimIndent()

        val fakeAiProvider = FakeAiProvider(fixedResponseText = jsonResponse)
        val aiRepository = AiTaskRouter(listOf(fakeAiProvider))
        val useCase = GenerateInstagramOpportunitiesUseCase(aiRepository)

        val sampleAnalysis = YouTubeContentAnalysis(
            summary = "Video about Flint AI system.",
            mainTopics = listOf("KMP", "AI Gateway"),
            keyPoints = listOf("Fallback strategy", "Source truthfulness"),
            notableQuotes = listOf("One spark. Endless stories."),
            potentialHooks = listOf("Stop rewriting logic."),
            audience = "Engineers",
            contentThemes = listOf("Architecture")
        )

        val result = useCase.execute(sampleAnalysis, maxRetries = 2, initialDelayMs = 1L)
        assertTrue(result is FlintResult.Success<*>)

        val opportunities = (result as FlintResult.Success<*>).data as List<InstagramContentOpportunity>
        assertEquals(2, opportunities.size)
        assertEquals(OpportunityType.REEL_IDEA, opportunities[0].type)
        assertEquals(OpportunityType.STORY_SEQUENCE, opportunities[1].type)
    }

    @Test
    fun testGenerateInstagramOpportunitiesAiFailure() = runTest {
        val fakeAiProvider = FakeAiProvider(shouldSucceed = false)
        val aiRepository = AiTaskRouter(listOf(fakeAiProvider))
        val useCase = GenerateInstagramOpportunitiesUseCase(aiRepository)

        val sampleAnalysis = YouTubeContentAnalysis(summary = "Sample")
        val result = useCase.execute(sampleAnalysis, maxRetries = 2, initialDelayMs = 1L)

        assertTrue(result is FlintResult.Error<*>)
        assertTrue(((result as FlintResult.Error<*>).error as AppError).message.contains("failed"))
    }
}
