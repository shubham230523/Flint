package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramStorySequence
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramStoriesUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        opportunity: InstagramContentOpportunity,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<InstagramStorySequence, AppError> {
        val tag = "GenerateInstagramStoriesUseCase"
        FlintLogger.i(tag, "Generating Instagram Story Sequence for opportunity: ${opportunity.title}")

        val prompt = """
            Role: Instagram Stories Strategist
            Task: Design a 4-6 frame interactive Instagram Story sequence based on the given opportunity.
            
            CREATOR DNA STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Authentic & Conversational" }}
            
            RULES:
            1. Frame 1 must hook viewers immediately.
            2. Frames should be bite-sized with clear headline and short body.
            3. Include interactive suggestions (Poll, Question Sticker, Quiz, Slider) where appropriate.
            4. Output MUST be raw JSON strictly matching:
            
            {
              "title": "${opportunity.title}",
              "stories": [
                {
                  "sequenceNumber": 1,
                  "headline": "Frame 1 Headline",
                  "body": "Frame 1 text",
                  "interactionSuggestion": "Poll: Do you agree? (Yes / No)",
                  "CTA": "Tap to see next frame"
                }
              ]
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            
            Provide ONLY raw JSON.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.6f))
        if (aiResult is FlintResult.Error) return FlintResult.Error(aiResult.error)

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val stories = InstagramStorySequence.parseFromJson(cleaned)
            ?: return FlintResult.Error(AppError.AiProvider("Failed to parse Story Sequence JSON"))

        return FlintResult.Success(stories)
    }
}
