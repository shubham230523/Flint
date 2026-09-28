package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.map
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import kotlin.random.Random

class GenerateContentUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        sourceText: String,
        targetType: ContentType,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<ContentAsset, AppError> {
        val tag = "GenerateContentUseCase"
        FlintLogger.i(tag, "Executing content generation for targetType: ${targetType.name} | Tone: ${creatorDna.preferredTone}")

        val prompt = buildPrompt(sourceText, targetType, creatorDna)
        val aiRequest = AiRequest(prompt = prompt)

        return aiRepository.generateContent(aiRequest).map { response ->
            val cleanedBody = com.shubhamthorat.flint.domain.ai.AiContentCleaner.clean(response.content)
            val randomSuffix = Random.nextInt(100000, 999999)
            val asset = ContentAsset(
                id = "asset_${targetType.name.lowercase()}_$randomSuffix",
                sourceId = null,
                title = "Generated ${targetType.name.replace('_', ' ')}",
                body = cleanedBody,
                type = targetType,
                status = ContentStatus.DRAFT,
                platform = getPlatformForType(targetType)
            )
            FlintLogger.i(tag, "Content asset successfully generated: ${asset.title} (ID: ${asset.id}) for platform ${asset.platform}")
            asset
        }
    }

    private fun buildPrompt(sourceText: String, targetType: ContentType, dna: CreatorDNA): String {
        return """
            Role: Expert Content Creator
            Target Output: ${targetType.name}
            Tone: ${dna.preferredTone}
            Style: ${dna.writingStyle}
            Audience: ${dna.targetAudience}
            CTA Style: ${dna.ctaStyle}
            
            Instruction: Provide ONLY the final generated content. Do not include any internal thinking process, analysis, or introductory preamble.
            
            Source Material:
            $sourceText
        """.trimIndent()
    }

    private fun getPlatformForType(type: ContentType): String {
        return when (type) {
            ContentType.YOUTUBE_SCRIPT -> "YouTube"
            ContentType.SHORT_SCRIPT, ContentType.REEL_SCRIPT -> "Shorts/Reels"
            ContentType.LINKEDIN_POST -> "LinkedIn"
            ContentType.X_THREAD -> "X (Twitter)"
            ContentType.INSTAGRAM_CAPTION -> "Instagram"
            ContentType.CAROUSEL -> "Carousel"
            ContentType.NEWSLETTER, ContentType.EMAIL -> "Newsletter"
            ContentType.BLOG -> "Blog"
            ContentType.COMMUNITY_POST -> "Community"
        }
    }
}
