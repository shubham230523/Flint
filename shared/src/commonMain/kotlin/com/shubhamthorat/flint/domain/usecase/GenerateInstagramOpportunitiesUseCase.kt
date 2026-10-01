package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.OpportunityListContainer
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramOpportunitiesUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        analysis: YouTubeContentAnalysis,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<List<InstagramContentOpportunity>, AppError> {
        val tag = "GenerateInstagramOpportunitiesUseCase"
        FlintLogger.i(tag, "Generating Instagram content opportunities from YouTube content analysis")

        if (analysis.summary.contains("MOCK_TEST_WORKSPACE", ignoreCase = true)) {
            FlintLogger.i(tag, "Using MOCK Instagram Opportunities data for test pipeline")
            return FlintResult.Success(getMockOpportunities())
        }

        val prompt = buildOpportunitiesPrompt(analysis, creatorDna)
        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.5f,
            maxTokens = 2048
        )

        val aiResult = aiRepository.generateContent(aiRequest)
        if (aiResult is FlintResult.Error) {
            FlintLogger.e(tag, "AI generation failed for Instagram opportunities: ${aiResult.error.message}")
            return FlintResult.Error(aiResult.error)
        }

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val opportunities = OpportunityListContainer.parseFromJson(cleaned)
        if (opportunities.isEmpty()) {
            FlintLogger.e(tag, "Failed to parse Instagram opportunities from AI response")
            return FlintResult.Error(
                AppError.AiProvider("Could not generate valid Instagram opportunities from AI response")
            )
        }

        FlintLogger.i(tag, "Successfully generated ${opportunities.size} Instagram content opportunities from AI")
        return FlintResult.Success(opportunities)
    }

    private fun buildOpportunitiesPrompt(
        analysis: YouTubeContentAnalysis,
        creatorDna: CreatorDNA
    ): String {
        val topicsStr = analysis.mainTopics.joinToString(", ")
        val keyPointsStr = analysis.keyPoints.joinToString("; ")
        val quotesStr = analysis.notableQuotes.joinToString(" | ")

        return """
            Role: Instagram Growth Strategist & Content Planner
            Task: Convert the provided YouTube video analysis into high-performing, varied Instagram content opportunities.
            
            CREATOR DNA STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Engaging" }}
            Audience: ${creatorDna.targetAudience.ifBlank { analysis.audience.ifBlank { "General Instagram Audience" } }}
            
            RULES:
            1. Generate 3 to 6 distinct content opportunities across types: REEL_IDEA, CAROUSEL, STORY_SEQUENCE, QUOTE_POST, EDUCATIONAL_POST, QUESTION_POST.
            2. Do NOT duplicate the same idea across types.
            3. Ensure every hook is compelling, strong, but truthful to the source material.
            4. Base ALL opportunities ONLY on facts/quotes/topics present in the video analysis.
            5. Output MUST be raw JSON adhering strictly to:
            
            {
              "opportunities": [
                {
                  "id": "opp_1",
                  "type": "REEL_IDEA",
                  "title": "Title of the idea",
                  "description": "What this Instagram post is about",
                  "sourceReference": "Reference to video point",
                  "suggestedHook": "Strong opening hook"
                }
              ]
            }
            
            YOUTUBE VIDEO ANALYSIS:
            Summary: ${analysis.summary}
            Main Topics: $topicsStr
            Key Points: $keyPointsStr
            Quotes: $quotesStr
            Audience: ${analysis.audience}
            
            Provide ONLY raw JSON.
        """.trimIndent()
    }

    private fun getMockOpportunities(): List<InstagramContentOpportunity> {
        return listOf(
            InstagramContentOpportunity(
                id = "opp_1",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.REEL_IDEA,
                title = "The Core Narrative Spark: Your Content's Main Function",
                description = "A fast-paced Reel explaining why every piece of content needs a 'core narrative spark' before repurposing it into Reels, Carousels, or Stories.",
                sourceReference = "Transcript Section 1",
                suggestedHook = "Stop building your content pipeline without the core spark — it's like shipping code without a build. 🔮🔥"
            ),
            InstagramContentOpportunity(
                id = "opp_2",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.CAROUSEL,
                title = "3 Takeaways for Turning Long-Form into Scroll-Stopping Carousels",
                description = "A 4-slide carousel breaking down how extracting key takeaways and hooks makes repurposing effortless for engineers and creators alike.",
                sourceReference = "Transcript Section 2",
                suggestedHook = "Your codebase needs refactoring, and so does your content strategy. Here's how. 🧩➡️📊"
            ),
            InstagramContentOpportunity(
                id = "opp_3",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.STORY_SEQUENCE,
                title = "Behind the Scenes: My YouTube-to-Instagram Pipeline",
                description = "A 5-story sequence walking through the KMP → Compose Multiplatform → AI Gateway pipeline, showing how each step feeds the next for seamless repurposing.",
                sourceReference = "Transcript Section 3",
                suggestedHook = "Story 1: The spark. Story 2: The extraction. Story 3: The AI ground. Story 4: The publish. Let's walk through it. 🧵👇"
            ),
            InstagramContentOpportunity(
                id = "opp_4",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.QUOTE_POST,
                title = "Quote Graphic: The Core Narrative Spark",
                description = "A visually striking quote post featuring the exact words from the video on the importance of the core narrative spark.",
                sourceReference = "Transcript Quote",
                suggestedHook = "Wisdom for devs who post: Get to the spark first. Everything else is just noise. 🚀✨"
            ),
            InstagramContentOpportunity(
                id = "opp_5",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.EDUCATIONAL_POST,
                title = "How to Ground AI Content in Source Facts (Without Losing Your Brand DNA)",
                description = "An educational post explaining the AI Gateway pattern approach to ensuring every piece of AI-generated content stays rooted in verified source material while preserving your unique voice.",
                sourceReference = "Transcript Section 4",
                suggestedHook = "AI is only as reliable as the facts you feed it. Stop hallucinating your captions and start grounding them. 🤖📝"
            )
        )
    }
}
