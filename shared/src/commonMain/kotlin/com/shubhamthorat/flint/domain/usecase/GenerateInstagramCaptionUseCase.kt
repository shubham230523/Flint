package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramCaption
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramCaptionUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        postType: String,
        opportunity: InstagramContentOpportunity,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<InstagramCaption, AppError> {
        val tag = "GenerateInstagramCaptionUseCase"
        FlintLogger.i(tag, "Generating Instagram Caption for post type: $postType | ${opportunity.title}")

        val toneStr = creatorDna.preferredTone.ifBlank { "Engaging & Direct" }
        val prompt = """
            Role: Instagram Caption Copywriter
            Task: Write a complete Instagram post caption with relevant hashtags for a $postType post.
            
            IMPORTANT:
            DO NOT output reasoning, thinking process, or preamble text. Output ONLY valid JSON immediately starting with '{'.
            
            CREATOR DNA STYLE:
            Tone: $toneStr
            Writing Style: ${creatorDna.writingStyle.ifBlank { "Conversational" }}
            CTA Style: ${creatorDna.ctaStyle.ifBlank { "Natural engagement" }}
            
            OUTPUT SCHEMA:
            {
              "caption": "Full Instagram caption text",
              "CTA": "Call to action sentence",
              "hashtags": ["#Tag1", "#Tag2", "#Tag3"]
            }
            
            POST DETAILS:
            Type: $postType
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            
            Provide ONLY raw JSON matching the schema.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.5f))
        if (aiResult is FlintResult.Success) {
            val cleaned = AiContentCleaner.clean(aiResult.data.content)
            val caption = InstagramCaption.parseFromJson(cleaned)
            if (caption != null && caption.caption.isNotBlank()) {
                return FlintResult.Success(caption)
            }
        }

        // Fallback for timeout / network stalls
        FlintLogger.w(tag, "AI call failed or stalled. Returning structured fallback Caption")
        val fallbackCaption = InstagramCaption(
            caption = "Finding your core narrative spark before you post changes everything. AI is only as reliable as the facts you feed it — ground your content in verified sources while keeping your brand's unique voice.",
            CTA = "What core spark are you building toward? Let us know below! 👇",
            hashtags = listOf("#CoreNarrative", "#DevWisdom", "#ContentFirst", "#MinimalistMedia", "#PostWithPurpose", "#StoryStrategy", "#DigitalCreativity")
        )
        return FlintResult.Success(fallbackCaption)
    }
}
