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
        if (opportunities.isNotEmpty()) {
            FlintLogger.i(tag, "Successfully generated ${opportunities.size} Instagram content opportunities from AI")
            return FlintResult.Success(opportunities)
        }

        FlintLogger.w(tag, "AI response unparseable. Constructing dynamic opportunities from analysis facts")
        val dynamicOpportunities = createDynamicOpportunitiesFromAnalysis(analysis)
        return FlintResult.Success(dynamicOpportunities)
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

    private fun createDynamicOpportunitiesFromAnalysis(
        analysis: YouTubeContentAnalysis
    ): List<InstagramContentOpportunity> {
        val list = mutableListOf<InstagramContentOpportunity>()

        val primaryTopic = analysis.mainTopics.firstOrNull() ?: "Video Key Takeaways"
        val primaryKeyPoint = analysis.keyPoints.firstOrNull() ?: analysis.summary
        val primaryQuote = analysis.notableQuotes.firstOrNull() ?: primaryKeyPoint
        val primaryHook = analysis.potentialHooks.firstOrNull() ?: primaryKeyPoint

        list.add(
            InstagramContentOpportunity(
                id = "opp_real_1",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.REEL_IDEA,
                title = primaryTopic,
                description = "Short viral Reel exploring: $primaryKeyPoint",
                sourceReference = "Video Summary",
                suggestedHook = primaryHook
            )
        )

        if (analysis.keyPoints.size > 1) {
            list.add(
                InstagramContentOpportunity(
                    id = "opp_real_2",
                    type = com.shubhamthorat.flint.domain.model.OpportunityType.CAROUSEL,
                    title = "Key Insights: $primaryTopic",
                    description = "Carousel breakdown of: ${analysis.keyPoints.take(3).joinToString("; ")}",
                    sourceReference = "Key Points",
                    suggestedHook = "Here are the top takeaways from this breakdown 👇"
                )
            )
        }

        list.add(
            InstagramContentOpportunity(
                id = "opp_real_3",
                type = com.shubhamthorat.flint.domain.model.OpportunityType.QUOTE_POST,
                title = "Notable Statement: $primaryTopic",
                description = "Quote graphic featuring key video statement: \"$primaryQuote\"",
                sourceReference = "Notable Quote",
                suggestedHook = primaryQuote
            )
        )

        if (analysis.mainTopics.size > 1) {
            list.add(
                InstagramContentOpportunity(
                    id = "opp_real_4",
                    type = com.shubhamthorat.flint.domain.model.OpportunityType.STORY_SEQUENCE,
                    title = "Story Deep Dive: ${analysis.mainTopics.last()}",
                    description = "Interactive story sequence exploring ${analysis.mainTopics.last()}",
                    sourceReference = "Main Topics",
                    suggestedHook = "Let's break down ${analysis.mainTopics.last()} in 3 quick steps."
                )
            )
        }

        return list
    }
}
