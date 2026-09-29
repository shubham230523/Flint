package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemoryContentRepository
import com.shubhamthorat.flint.data.repository.InMemorySourceRepository
import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramCaption
import com.shubhamthorat.flint.domain.model.InstagramCarousel
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramQuotePost
import com.shubhamthorat.flint.domain.model.InstagramReelContent
import com.shubhamthorat.flint.domain.model.InstagramStorySequence
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.repository.FakeTranscriptProvider
import com.shubhamthorat.flint.domain.repository.FakeYouTubeVideoProvider
import com.shubhamthorat.flint.domain.usecase.AnalyzeYouTubeContentUseCase
import com.shubhamthorat.flint.domain.usecase.CreateYouTubeSourceUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCaptionUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCarouselUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramOpportunitiesUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramQuotePostsUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramReelUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramStoriesUseCase
import com.shubhamthorat.flint.domain.usecase.ProcessYouTubeSourceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YouTubePipelineEndToEndTest {

    @Test
    fun testCompleteYouTubeToInstagramPipeline() = runTest {
        // 1. Repositories & Providers Setup
        val sourceRepo = InMemorySourceRepository()
        val contentRepo = InMemoryContentRepository()
        val videoProvider = FakeYouTubeVideoProvider()
        val transcriptProvider = FakeTranscriptProvider()

        // Canned AI JSON Responses for deterministic E2E testing
        val analysisJson = """
            {
              "summary": "Full tutorial on building a YouTube to Instagram pipeline.",
              "mainTopics": ["YouTube Ingestion", "AI Video Analysis", "Instagram Content"],
              "keyPoints": ["Ground AI in transcript facts", "Preserve Creator DNA", "Multi-platform export"],
              "notableQuotes": ["One spark. Endless stories."],
              "potentialHooks": ["Turn 1 YouTube video into 10 Instagram posts."],
              "audience": "Content Creators & Software Developers",
              "contentThemes": ["AI", "Content Repurposing"]
            }
        """.trimIndent()

        val opportunitiesJson = """
            {
              "opportunities": [
                {
                  "id": "opp_e2e_1",
                  "type": "REEL_IDEA",
                  "title": "Why Manual Video Repurposing is Dead",
                  "description": "Short reel explaining automated transcript analysis",
                  "sourceReference": "Transcript 00:00 - 05:00",
                  "suggestedHook": "Stop spending 4 hours editing a single Reel."
                },
                {
                  "id": "opp_e2e_2",
                  "type": "CAROUSEL",
                  "title": "3 Steps to Repurpose YouTube Videos",
                  "description": "Educational carousel on video content breakdown",
                  "sourceReference": "Transcript 05:00 - 12:00",
                  "suggestedHook": "How top creators turn 1 video into 5 posts."
                }
              ]
            }
        """.trimIndent()

        val reelJson = """
            {
              "hook": "Stop spending 4 hours editing a single Reel.",
              "body": "Flint ingests your YouTube transcript and extracts core story sparks automatically.",
              "ending": "Try Flint today and scale your brand effortlessly.",
              "CTA": "Comment SPARK for access.",
              "suggestedDuration": "30s"
            }
        """.trimIndent()

        val captionJson = """
            {
              "caption": "Transform your video content strategy in seconds with Flint AI.",
              "CTA": "Link in bio to get started.",
              "hashtags": ["#KotlinMultiplatform", "#ContentCreator", "#FlintAI"]
            }
        """.trimIndent()

        val fakeAiProvider = FakeAiProvider(fixedResponseText = analysisJson)
        val aiTaskRouter = AiTaskRouter(listOf(fakeAiProvider))

        val createSourceUseCase = CreateYouTubeSourceUseCase(sourceRepo)
        val processSourceUseCase = ProcessYouTubeSourceUseCase(sourceRepo, videoProvider, transcriptProvider)
        val analyzeUseCase = AnalyzeYouTubeContentUseCase(aiTaskRouter)

        // Step 1: Create YouTube Source
        val createRes = createSourceUseCase.execute("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(createRes is FlintResult.Success<*>)
        val createdSource = (createRes as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.repository.SourceItem

        // Step 2: Process YouTube Source (Metadata + Transcript)
        val processRes = processSourceUseCase.execute(createdSource.id)
        assertTrue(processRes is FlintResult.Success<*>)
        val procData = (processRes as FlintResult.Success<*>).data as com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult

        // Step 3: Analyze Video Content
        val analyzeRes = analyzeUseCase.execute(procData, CreatorDNA(preferredTone = "Authoritative"))
        assertTrue(analyzeRes is FlintResult.Success<*>)
        val analysis = (analyzeRes as FlintResult.Success<*>).data as YouTubeContentAnalysis
        assertEquals("Full tutorial on building a YouTube to Instagram pipeline.", analysis.summary)

        // Step 4: Discover Instagram Opportunities
        val oppAiProvider = FakeAiProvider(fixedResponseText = opportunitiesJson)
        val oppRouter = AiTaskRouter(listOf(oppAiProvider))
        val oppUseCase = GenerateInstagramOpportunitiesUseCase(oppRouter)

        val oppRes = oppUseCase.execute(analysis)
        assertTrue(oppRes is FlintResult.Success<*>)
        val opps = (oppRes as FlintResult.Success<*>).data as List<InstagramContentOpportunity>
        assertEquals(2, opps.size)

        // Step 5: Generate Reel Script & Caption
        val reelAiProvider = FakeAiProvider(fixedResponseText = reelJson)
        val reelRouter = AiTaskRouter(listOf(reelAiProvider))
        val reelUseCase = GenerateInstagramReelUseCase(reelRouter)

        val reelRes = reelUseCase.execute(opps[0])
        assertTrue(reelRes is FlintResult.Success<*>)
        val reel = (reelRes as FlintResult.Success<*>).data as InstagramReelContent
        assertEquals("Stop spending 4 hours editing a single Reel.", reel.hook)

        val captionAiProvider = FakeAiProvider(fixedResponseText = captionJson)
        val captionRouter = AiTaskRouter(listOf(captionAiProvider))
        val captionUseCase = GenerateInstagramCaptionUseCase(captionRouter)

        val captionRes = captionUseCase.execute("Reel", opps[0])
        assertTrue(captionRes is FlintResult.Success<*>)
        val caption = (captionRes as FlintResult.Success<*>).data as InstagramCaption

        // Step 6: Save Generated Asset to Content Library
        val savedAsset = ContentAsset(
            id = "asset_e2e_001",
            sourceId = createdSource.id,
            title = opps[0].title,
            body = "${reel.hook}\n\n${reel.body}\n\n${caption.caption}\n\n${caption.hashtags.joinToString(" ")}",
            type = ContentType.REEL_SCRIPT,
            status = ContentStatus.DRAFT,
            platform = "Instagram"
        )

        contentRepo.saveContent(savedAsset)
        val libraryAssets = contentRepo.observeContentAssets().first()

        assertEquals(1, libraryAssets.size)
        assertEquals("asset_e2e_001", libraryAssets[0].id)
        assertEquals("Why Manual Video Repurposing is Dead", libraryAssets[0].title)
        assertEquals("Instagram", libraryAssets[0].platform)
    }
}
