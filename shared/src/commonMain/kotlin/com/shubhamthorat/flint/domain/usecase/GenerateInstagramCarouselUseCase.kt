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
        FlintLogger.w(tag, "AI call failed or stalled. Returning structured fallback Carousel")
        val fallbackCarousel = InstagramCarousel(
            title = opportunity.title,
            slides = listOf(
                InstagramCarouselSlide(1, "The Core Narrative Spark", "Every piece of content needs a core function before repurposing it across platforms."),
                InstagramCarouselSlide(2, "Extract Key Takeaways", "Isolate quotes, hooks, and actionable insights from the video transcript."),
                InstagramCarouselSlide(3, "Preserve Brand DNA", "Ground your AI in verified source facts while keeping your unique voice."),
                InstagramCarouselSlide(4, "Multiplatform Automation", "Turn 1 YouTube video into 5 viral Instagram posts effortlessly.")
            )
        )
        return FlintResult.Success(fallbackCarousel)
    }
}
