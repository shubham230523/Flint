package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramReelContent
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest

class GenerateInstagramReelUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        opportunity: InstagramContentOpportunity,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<InstagramReelContent, AppError> {
        val tag = "GenerateInstagramReelUseCase"
        FlintLogger.i(tag, "Generating Instagram Reel script for opportunity: ${opportunity.title}")

        val toneStr = creatorDna.preferredTone.ifBlank { "Engaging & Direct" }
        val prompt = """
            Role: Viral Short-Form Video Scriptwriter (Instagram Reels & YouTube Shorts)
            Task: Write a highly engaging spoken Reel video script based on the provided opportunity.
            
            CREATOR DNA STYLE:
            Tone: $toneStr
            Writing Style: ${creatorDna.writingStyle.ifBlank { "Punchy & Conversational" }}
            Audience: ${creatorDna.targetAudience.ifBlank { "General Instagram Viewers" }}
            CTA Style: ${creatorDna.ctaStyle.ifBlank { "Value-add call to action" }}
            
            RULES:
            1. Opening hook MUST grab attention in the first 2 seconds.
            2. Script body must be spoken naturally, clear, and grounded ONLY in the source reference. Do NOT make up fake facts.
            3. End with a clear ending line and CTA.
            4. Output MUST be raw JSON strictly matching:
            
            {
              "hook": "First 2 second spoken opening hook",
              "body": "Spoken script body text",
              "ending": "Punchy wrap-up sentence",
              "CTA": "Call to action text",
              "suggestedDuration": "30s"
            }
            
            OPPORTUNITY:
            Title: ${opportunity.title}
            Description: ${opportunity.description}
            Suggested Hook: ${opportunity.suggestedHook}
            Source Reference: ${opportunity.sourceReference}
            
            Provide ONLY raw JSON.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.6f))
        if (aiResult is FlintResult.Error) return FlintResult.Error(aiResult.error)

        val cleaned = AiContentCleaner.clean((aiResult as FlintResult.Success).data.content)
        val reel = InstagramReelContent.parseFromJson(cleaned)
            ?: return FlintResult.Error(AppError.AiProvider("Failed to parse Reel script JSON"))

        return FlintResult.Success(reel)
    }
}
