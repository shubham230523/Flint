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
            
            IMPORTANT:
            DO NOT output reasoning, thinking process, or preamble text. Output ONLY valid JSON immediately starting with '{'.
            
            CREATOR DNA STYLE:
            Tone: $toneStr
            Writing Style: ${creatorDna.writingStyle.ifBlank { "Punchy & Conversational" }}
            Audience: ${creatorDna.targetAudience.ifBlank { "General Instagram Viewers" }}
            CTA Style: ${creatorDna.ctaStyle.ifBlank { "Value-add call to action" }}
            
            OUTPUT SCHEMA:
            {
              "hook": "Spoken opening hook in first 2 seconds",
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
            
            Provide ONLY raw JSON matching the schema.
        """.trimIndent()

        val aiResult = aiRepository.generateContent(AiRequest(prompt = prompt, temperature = 0.6f))
        if (aiResult is FlintResult.Success) {
            val cleaned = AiContentCleaner.clean(aiResult.data.content)
            val reel = InstagramReelContent.parseFromJson(cleaned)
            if (reel != null) {
                return FlintResult.Success(reel)
            }
        }

        // Fallback for timeout / network stalls
        FlintLogger.w(tag, "AI call failed or stalled. Returning structured fallback Reel Content")
        val fallbackReel = InstagramReelContent(
            hook = opportunity.suggestedHook.ifBlank { "Stop building your content pipeline without the core spark! 🔮🔥" },
            body = "Every video has a story hiding inside it. When you extract key takeaways and hooks first, generating Reels, Carousels, and Stories becomes effortless.",
            ending = "Ground your AI in source facts and preserve your brand DNA.",
            CTA = "Drop a 🔥 in the comments if you want the full breakdown!",
            suggestedDuration = "30-45s"
        )
        return FlintResult.Success(fallbackReel)
    }
}
