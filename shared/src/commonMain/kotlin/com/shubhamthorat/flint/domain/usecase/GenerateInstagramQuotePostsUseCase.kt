package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramQuotePost
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramQuotePostsUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        opportunity: InstagramContentOpportunity,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<InstagramQuotePost, AppError> {
        val tag = "GenerateInstagramQuotePostsUseCase"
        FlintLogger.i(tag, "Generating Instagram Quote Post for opportunity: ${opportunity.title}")

        val prompt = """
            Role: Instagram Quote Card Copywriter
            Task: Extract or format a memorable, high-impact quote from the given content opportunity.
            
            IMPORTANT RULE:
            Quote MUST come directly from the source transcript/reference. Do NOT fabricate fake quotes.
            
            OUTPUT SCHEMA (Raw JSON):
            {
              "quote": "<memorable key statement from the video>",
              "context": "<speaker or video topic background>",
              "caption": "<engaging Instagram caption expanding on the quote>",
              "CTA": "<actionable call to action text>"
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            Source Reference: ${opportunity.sourceReference}
            
            Provide ONLY raw JSON.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.4f))
        if (aiResult is FlintResult.Error) return FlintResult.Error(aiResult.error)

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val quotePost = InstagramQuotePost.parseFromJson(cleaned)
            ?: return FlintResult.Error(AppError.AiProvider("Failed to parse Quote Post JSON"))

        return FlintResult.Success(quotePost)
    }
}
