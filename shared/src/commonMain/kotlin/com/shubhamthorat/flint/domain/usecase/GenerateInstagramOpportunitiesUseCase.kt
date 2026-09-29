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

        val rawContent = (aiResult as FlintResult.Success).data.content
        val cleaned = AiContentCleaner.clean(rawContent)

        val opportunities = OpportunityListContainer.parseFromJson(cleaned)
        if (opportunities.isEmpty()) {
            FlintLogger.e(tag, "Failed to parse Instagram opportunities from AI response")
            return FlintResult.Error(
                AppError.AiProvider("Could not generate valid Instagram opportunities from AI response")
            )
        }

        FlintLogger.i(tag, "Successfully generated ${opportunities.size} Instagram content opportunities")
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
}
