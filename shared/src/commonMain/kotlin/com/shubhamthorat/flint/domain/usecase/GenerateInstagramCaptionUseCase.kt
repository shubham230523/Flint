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
            
            CREATOR DNA STYLE:
            Tone: $toneStr
            Writing Style: ${creatorDna.writingStyle.ifBlank { "Conversational" }}
            CTA Style: ${creatorDna.ctaStyle.ifBlank { "Natural engagement" }}
            
            RULES:
            1. Write a compelling main caption that provides value and context.
            2. Include a natural call to action.
            3. Include 5 to 10 highly relevant, targeted hashtags (no generic hashtag stuffing).
            4. Output MUST be raw JSON strictly matching:
            
            {
              "caption": "<write complete Instagram caption text>",
              "CTA": "<call to action sentence>",
              "hashtags": ["#tag1", "#tag2", "#tag3"]
            }
            
            POST DETAILS:
            Type: $postType
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            
            Provide ONLY raw JSON.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.5f))
        if (aiResult is FlintResult.Error) return FlintResult.Error(aiResult.error)

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val caption = InstagramCaption.parseFromJson(cleaned)
            ?: return FlintResult.Error(AppError.AiProvider("Failed to parse Caption JSON"))

        return FlintResult.Success(caption)
    }
}
