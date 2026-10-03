package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemoryContentRepository
import com.shubhamthorat.flint.data.repository.InMemoryMediaProcessingRepository
import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.MediaJobStatus
import com.shubhamthorat.flint.domain.model.MediaJobType
import com.shubhamthorat.flint.domain.model.MediaProcessingJob
import com.shubhamthorat.flint.domain.model.OpportunityType
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.model.ReelCandidateStatus
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramReelUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReelPipelineE2ETest {

    @Test
    fun testGenerateInstagramReelUseCaseSuccess() = runTest {
        val reelJson = """
            {
              "hook": "Stop scrolling! Here is how to scale your video workflow.",
              "body": "Flint automatically turns 30-minute YouTube videos into 9:16 vertical Reels.",
              "ending": "Follow for more AI creator tips.",
              "CTA": "Save this Reel now!",
              "suggestedDuration": "30s"
            }
        """.trimIndent()

        val aiProvider = FakeAiProvider(fixedResponseText = reelJson)
        val router = AiTaskRouter(listOf(aiProvider))
        val useCase = GenerateInstagramReelUseCase(router)

        val opportunity = InstagramContentOpportunity(
            id = "opp_1",
            type = OpportunityType.REEL_IDEA,
            title = "Automated Video Repurposing",
            description = "AI pipeline video clip extraction",
            sourceReference = "00:00 - 05:00",
            suggestedHook = "Stop scrolling!"
        )

        val result = useCase.execute(opportunity, CreatorDNA())
        assertTrue(result is FlintResult.Success)

        val reel = result.data
        assertEquals("Stop scrolling! Here is how to scale your video workflow.", reel.hook)
        assertEquals("Save this Reel now!", reel.CTA)
    }

    @Test
    fun testGenerateInstagramReelUseCaseFallbackOnAiFailure() = runTest {
        // AI provider that fails
        val failingProvider = FakeAiProvider(shouldSucceed = false)
        val router = AiTaskRouter(listOf(failingProvider))
        val useCase = GenerateInstagramReelUseCase(router)

        val opportunity = InstagramContentOpportunity(
            id = "opp_fail_1",
            type = OpportunityType.REEL_IDEA,
            title = "Fallback Test Title",
            description = "Fallback Test Description",
            sourceReference = "05:00 - 10:00",
            suggestedHook = "Default Hook"
        )

        val result = useCase.execute(opportunity, CreatorDNA())
        assertTrue(result is FlintResult.Success)

        val reel = result.data
        assertEquals("Default Hook", reel.hook)
        assertTrue(reel.body.contains("Fallback Test Description"))
    }

    @Test
    fun testMediaProcessingJobResumableStateAndRetry() = runTest {
        val repo = InMemoryMediaProcessingRepository()

        val job = MediaProcessingJob(
            id = "job_reel_001",
            sourceId = "src_yt_45K3zHckCnQ",
            userId = "user_test_1",
            type = MediaJobType.REEL_RENDER,
            status = MediaJobStatus.FAILED,
            progress = 45,
            error = "FFmpeg transient error"
        )

        repo.saveJob(job)
        val fetchedRes = repo.getJobById("job_reel_001")
        assertTrue(fetchedRes is FlintResult.Success)
        val fetched = fetchedRes.data

        assertEquals(MediaJobStatus.FAILED, fetched.status)

        // Retry job from failed state
        val retriedJob = fetched.copy(status = MediaJobStatus.RENDERING, progress = 50, error = null)
        repo.saveJob(retriedJob)

        val updatedRes = repo.getJobById("job_reel_001")
        assertTrue(updatedRes is FlintResult.Success)
        val updated = updatedRes.data

        assertEquals(MediaJobStatus.RENDERING, updated.status)
        assertEquals(50, updated.progress)
    }

    @Test
    fun testReelContentAssetPersistenceInContentLibrary() = runTest {
        val contentRepo = InMemoryContentRepository()

        val candidate = ReelCandidate(
            id = "cand_45K3zHckCnQ_1",
            sourceId = "src_45K3zHckCnQ",
            startTimeMs = 0L,
            endTimeMs = 30000L,
            transcript = "Sample transcript for Reel candidate",
            title = "Canonical Reel Candidate #1",
            hook = "Hook overlay text",
            ctaText = "Drop a comment!",
            status = ReelCandidateStatus.ACCEPTED,
            videoUrl = "C:/tmp/flint_media/reels/cand_45K3zHckCnQ_1_final_reel.mp4"
        )

        val asset = ContentAsset(
            id = "reel_asset_45K3zHckCnQ_1",
            sourceId = candidate.sourceId,
            title = candidate.title,
            body = "🎬 REEL HOOK:\n${candidate.hook}\n\n📹 SCRIPT:\n${candidate.transcript}\n\n📣 CTA:\n${candidate.ctaText}\n\n🎥 Output MP4: ${candidate.videoUrl}",
            type = ContentType.INSTAGRAM_REEL,
            status = ContentStatus.DRAFT,
            platform = "Instagram"
        )

        contentRepo.saveContent(asset)
        val assets = contentRepo.observeContentAssets().first()

        assertEquals(1, assets.size)
        val saved = assets[0]
        assertEquals("reel_asset_45K3zHckCnQ_1", saved.id)
        assertEquals(ContentType.INSTAGRAM_REEL, saved.type)
        assertTrue(saved.body.contains("Output MP4"))
    }
}
