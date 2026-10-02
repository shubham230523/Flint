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
            
            IMPORTANT:
            DO NOT output reasoning, thinking process, or preamble text. Output ONLY valid JSON immediately starting with '{'.
            
            OUTPUT SCHEMA:
            {
              "quote": "Memorable key statement",
              "context": "Speaker or topic reference",
              "caption": "Instagram caption expanding on the quote",
              "CTA": "Call to action text"
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            Source Reference: ${opportunity.sourceReference}
            
            Provide ONLY raw JSON matching the schema.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.4f))
        if (aiResult is FlintResult.Success) {
            val cleaned = AiContentCleaner.clean(aiResult.data.content)
            val quotePost = InstagramQuotePost.parseFromJson(cleaned)
            if (quotePost != null) {
                return FlintResult.Success(quotePost)
            }
        }

        // Fallback for timeout / network stalls
        FlintLogger.w(tag, "AI call failed or stalled. Returning dynamic Quote Post from opportunity details")
        val fallbackQuote = InstagramQuotePost(
            quote = opportunity.suggestedHook.ifBlank { opportunity.description },
            context = opportunity.title,
            caption = opportunity.description,
            CTA = "Share your thoughts in the comments below! 👇"
        )
        return FlintResult.Success(fallbackQuote)
    }
}
