package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemoryMediaProcessingRepository
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.MediaJobStatus
import com.shubhamthorat.flint.domain.model.MediaJobType
import com.shubhamthorat.flint.domain.model.MediaProcessingJob
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.model.ReelCandidateStatus
import com.shubhamthorat.flint.domain.model.ReelCandidateType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaProcessingJobTest {

    @Test
    fun testJobTerminalStatuses() {
        assertFalse(MediaJobStatus.QUEUED.isTerminal())
        assertFalse(MediaJobStatus.DOWNLOADING.isTerminal())
        assertFalse(MediaJobStatus.TRANSCRIBING.isTerminal())
        assertFalse(MediaJobStatus.ANALYZING.isTerminal())
        assertFalse(MediaJobStatus.RENDERING.isTerminal())

        assertTrue(MediaJobStatus.COMPLETED.isTerminal())
        assertTrue(MediaJobStatus.FAILED.isTerminal())
        assertTrue(MediaJobStatus.CANCELLED.isTerminal())
    }

    @Test
    fun testValidJobStateTransitions() {
        val job = MediaProcessingJob(
            id = "job_1",
            sourceId = "src_1",
            userId = "user_1",
            type = MediaJobType.VIDEO_ANALYSIS,
            status = MediaJobStatus.QUEUED
        )

        assertTrue(job.canTransitionTo(MediaJobStatus.DOWNLOADING))
        assertTrue(job.canTransitionTo(MediaJobStatus.FAILED))
        assertTrue(job.canTransitionTo(MediaJobStatus.CANCELLED))

        val downloadingJob = job.copy(status = MediaJobStatus.DOWNLOADING)
        assertTrue(downloadingJob.canTransitionTo(MediaJobStatus.EXTRACTING_AUDIO))
        assertTrue(downloadingJob.canTransitionTo(MediaJobStatus.FAILED))

        val completedJob = job.copy(status = MediaJobStatus.COMPLETED)
        assertFalse(completedJob.canTransitionTo(MediaJobStatus.ANALYZING))
        assertFalse(completedJob.canTransitionTo(MediaJobStatus.FAILED))
    }

    @Test
    fun testInMemoryMediaProcessingRepositoryJobCrud() = runTest {
        val repo = InMemoryMediaProcessingRepository()

        val job = MediaProcessingJob(
            id = "job_100",
            sourceId = "src_1",
            userId = "user_abc",
            type = MediaJobType.VIDEO_ANALYSIS,
            status = MediaJobStatus.QUEUED,
            progress = 0
        )

        val saveRes = repo.saveJob(job)
        assertTrue(saveRes is FlintResult.Success)
        assertEquals(job, (saveRes as FlintResult.Success).data)

        val getRes = repo.getJobById("job_100")
        assertTrue(getRes is FlintResult.Success)
        assertEquals("job_100", (getRes as FlintResult.Success).data.id)

        val jobsList = repo.observeJobs("user_abc").first()
        assertEquals(1, jobsList.size)
        assertEquals("job_100", jobsList.first().id)

        val cancelRes = repo.cancelJob("job_100")
        assertTrue(cancelRes is FlintResult.Success)

        val cancelledJob = (repo.getJobById("job_100") as FlintResult.Success).data
        assertEquals(MediaJobStatus.CANCELLED, cancelledJob.status)
    }

    @Test
    fun testInMemoryMediaProcessingRepositoryCandidateCrud() = runTest {
        val repo = InMemoryMediaProcessingRepository()

        val candidate = ReelCandidate(
            id = "cand_1",
            sourceId = "src_yt_1",
            jobId = "job_100",
            startTimeMs = 12000L,
            endTimeMs = 45000L,
            transcript = "This is a great moment in the video.",
            title = "The Spark Strategy",
            hook = "Stop making videos without a spark!",
            contentType = ReelCandidateType.EDUCATIONAL,
            candidateScore = 0.92f,
            status = ReelCandidateStatus.DISCOVERED
        )

        val saveRes = repo.saveCandidate(candidate)
        assertTrue(saveRes is FlintResult.Success)

        val getRes = repo.getCandidateById("cand_1")
        assertTrue(getRes is FlintResult.Success)
        assertEquals("The Spark Strategy", (getRes as FlintResult.Success).data.title)

        val candidatesList = repo.observeReelCandidates("src_yt_1").first()
        assertEquals(1, candidatesList.size)

        val deleteRes = repo.deleteCandidate("cand_1")
        assertTrue(deleteRes is FlintResult.Success)

        val emptyList = repo.observeReelCandidates("src_yt_1").first()
        assertEquals(0, emptyList.size)
    }
}
