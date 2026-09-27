package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.map
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType

class GenerateContentUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        sourceText: String,
        targetType: ContentType,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<ContentAsset, AppError> {
        val prompt = buildPrompt(sourceText, targetType, creatorDna)
        val aiRequest = AiRequest(prompt = prompt)

        return aiRepository.generateContent(aiRequest).map { response ->
            ContentAsset(
                id = "asset_${targetType.name.lowercase()}",
                sourceId = null,
                title = "Generated ${targetType.name.replace('_', ' ')}",
                body = response.content,
                type = targetType,
                status = ContentStatus.DRAFT,
                platform = getPlatformForType(targetType)
            )
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
            ContentType.NEWSLETTER, ContentType.EMAIL -> "Email/Substack"
            ContentType.BLOG -> "Blog"
            ContentType.COMMUNITY_POST -> "Community"
        }
    }
}
