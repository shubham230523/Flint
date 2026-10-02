package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramCarousel
import com.shubhamthorat.flint.domain.model.InstagramCarouselSlide
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramCarouselUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        opportunity: InstagramContentOpportunity,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<InstagramCarousel, AppError> {
        val tag = "GenerateInstagramCarouselUseCase"
        FlintLogger.i(tag, "Generating Instagram Carousel for opportunity: ${opportunity.title}")

        val prompt = """
            Role: Instagram Carousel Designer & Copywriter
            Task: Convert the given idea into a 4-5 slide educational or story-driven Instagram Carousel.
            
            IMPORTANT:
            DO NOT output reasoning, thinking process, or preamble text. Output ONLY valid JSON immediately starting with '{'.
            
            CREATOR DNA STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Informative" }}
            Style: ${creatorDna.writingStyle.ifBlank { "Clear & Structured" }}
            
            OUTPUT SCHEMA:
            {
              "title": "${opportunity.title}",
              "slides": [
                {
                  "slideNumber": 1,
                  "headline": "Slide Headline",
                  "body": "Slide body text"
                }
              ]
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            Source Reference: ${opportunity.sourceReference}
            
            Provide ONLY raw JSON matching the schema.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.5f))
        if (aiResult is FlintResult.Success) {
            val cleaned = AiContentCleaner.clean(aiResult.data.content)
            val carousel = InstagramCarousel.parseFromJson(cleaned)
            if (carousel != null) {
                return FlintResult.Success(carousel)
            }
        }

        // Fallback for timeout / network stalls
        FlintLogger.w(tag, "AI call failed or stalled. Returning dynamic Carousel from opportunity details")
        val fallbackCarousel = InstagramCarousel(
            title = opportunity.title,
            slides = listOf(
                InstagramCarouselSlide(1, opportunity.title, opportunity.description),
                InstagramCarouselSlide(2, "Key Takeaway", opportunity.suggestedHook.ifBlank { opportunity.sourceReference }),
                InstagramCarouselSlide(3, "Deep Dive", "Exploring ${opportunity.title} in depth: ${opportunity.sourceReference}"),
                InstagramCarouselSlide(4, "Summary", "Save & share this post if you found it valuable!")
            )
        )
        return FlintResult.Success(fallbackCarousel)
    }
}
