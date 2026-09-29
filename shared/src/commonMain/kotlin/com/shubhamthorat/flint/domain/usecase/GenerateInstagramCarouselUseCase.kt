package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramCarousel
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
            Task: Convert the given idea into a 5-10 slide educational or story-driven Instagram Carousel.
            
            CREATOR DNA STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Informative" }}
            Style: ${creatorDna.writingStyle.ifBlank { "Clear & Structured" }}
            
            RULES:
            1. Slide 1 must have a strong hook headline.
            2. Each slide must express ONE clear takeaway with a headline and concise body text.
            3. Final slide must provide a wrap-up and CTA.
            4. Output MUST be raw JSON strictly matching:
            
            {
              "title": "${opportunity.title}",
              "slides": [
                {
                  "slideNumber": 1,
                  "headline": "Hook Slide Headline",
                  "body": "Slide 1 body text"
                }
              ]
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            Source Reference: ${opportunity.sourceReference}
            
            Provide ONLY raw JSON.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.5f))
        if (aiResult is FlintResult.Error) return FlintResult.Error(aiResult.error)

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val carousel = InstagramCarousel.parseFromJson(cleaned)
            ?: return FlintResult.Error(AppError.AiProvider("Failed to parse Carousel JSON"))

        return FlintResult.Success(carousel)
    }
}
