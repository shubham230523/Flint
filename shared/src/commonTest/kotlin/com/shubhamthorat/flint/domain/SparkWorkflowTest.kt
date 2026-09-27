package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.getOrThrow
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.usecase.CreateSparkCampaignUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateContentUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparkWorkflowTest {

    @Test
    fun testGenerateContentUseCaseWithCreatorDna() = runTest {
        val aiRouter = AiTaskRouter(listOf(FakeAiProvider("Gemini")))
        val useCase = GenerateContentUseCase(aiRepository = aiRouter)

        val dna = CreatorDNA(
            preferredTone = "Punchy",
            targetAudience = "KMP Engineers",
        )

        val result = useCase.execute(
            sourceText = "Kotlin Multiplatform 2.0 is revolutionary for desktop and mobile.",
            targetType = ContentType.LINKEDIN_POST,
            creatorDna = dna
        )

        assertTrue(result is FlintResult.Success)
        val asset = result.getOrThrow()
        assertEquals(ContentType.LINKEDIN_POST, asset.type)
        assertTrue(asset.body.contains("Gemini"))
    }

    @Test
    fun testCreateSparkCampaignUseCaseGeneratesMultipleAssets() = runTest {
        val aiRouter = AiTaskRouter(listOf(FakeAiProvider("OpenRouter")))
        val generateUseCase = GenerateContentUseCase(aiRepository = aiRouter)
        val campaignUseCase = CreateSparkCampaignUseCase(generateContentUseCase = generateUseCase)

        val result = campaignUseCase.execute(
            ideaOrSource = "AI Content Operating System",
            targetTypes = listOf(ContentType.LINKEDIN_POST, ContentType.X_THREAD, ContentType.NEWSLETTER),
            creatorDna = CreatorDNA()
        )

        assertTrue(result is FlintResult.Success)
        val campaign = result.getOrThrow()
        assertEquals("Campaign: AI Content Operating System", campaign.title)
        assertEquals(3, campaign.items.size)
    }
}