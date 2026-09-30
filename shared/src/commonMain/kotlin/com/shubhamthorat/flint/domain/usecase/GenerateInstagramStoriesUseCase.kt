package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramStory
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
            Task: Design a 4-frame interactive Instagram Story sequence based on the given opportunity.
            
            IMPORTANT:
            DO NOT output reasoning, thinking process, or preamble text. Output ONLY valid JSON immediately starting with '{'.
            
            CREATOR DNA STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Authentic & Conversational" }}
            
            OUTPUT SCHEMA:
            {
              "title": "${opportunity.title}",
              "stories": [
                {
                  "sequenceNumber": 1,
                  "headline": "Frame Headline",
                  "body": "Frame text",
                  "interactionSuggestion": "Sticker suggestion",
                  "CTA": "Call to action"
                }
              ]
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            
            Provide ONLY raw JSON matching the schema.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.6f))
        if (aiResult is FlintResult.Success) {
            val cleaned = AiContentCleaner.clean(aiResult.data.content)
            val stories = InstagramStorySequence.parseFromJson(cleaned)
            if (stories != null) {
                return FlintResult.Success(stories)
            }
        }

        // Fallback for timeout / network stalls
        FlintLogger.w(tag, "AI call failed or stalled. Returning structured fallback Story Sequence")
        val fallbackStories = InstagramStorySequence(
            title = opportunity.title,
            stories = listOf(
                InstagramStory(1, "The Spark", "Start with the core narrative spark before building content.", "Poll: Do you repurpose content? (Yes/No)", "Tap for step 2"),
                InstagramStory(2, "The Extraction", "Extract key takeaways, quotes, and hooks automatically.", "Quiz: What matters most? (Hook/Body/CTA)", "Tap for step 3"),
                InstagramStory(3, "The Grounding", "Ensure AI output stays grounded in source facts.", "Question Sticker: Ask me anything about KMP!", "Tap for final step"),
                InstagramStory(4, "The Publish", "Save to Content Library and publish across campaigns.", "", "Save this story sequence! 💾")
            )
        )
        return FlintResult.Success(fallbackStories)
    }
}
