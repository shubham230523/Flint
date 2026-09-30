package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.MediaJobStatus
import com.shubhamthorat.flint.domain.model.MediaProcessingJob
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.repository.MediaProcessingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class InMemoryMediaProcessingRepository : MediaProcessingRepository {

    private val tag = "InMemoryMediaProcessingRepository"
    private val jobsFlow = MutableStateFlow<List<MediaProcessingJob>>(emptyList())
    private val candidatesFlow = MutableStateFlow<List<ReelCandidate>>(emptyList())

    override fun observeJobs(userId: String): Flow<List<MediaProcessingJob>> {
        return jobsFlow.asStateFlow().map { list -> list.filter { it.userId == userId } }
    }

    override suspend fun getJobById(jobId: String): FlintResult<MediaProcessingJob, AppError> {
        val job = jobsFlow.value.find { it.id == jobId }
        return if (job != null) {
            FlintResult.Success(job)
        } else {
            FlintResult.Error(AppError.Validation("MediaJob with id $jobId not found"))
        }
    }

    override suspend fun saveJob(job: MediaProcessingJob): FlintResult<MediaProcessingJob, AppError> {
        val current = jobsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == job.id }
        if (index >= 0) {
            current[index] = job
        } else {
            current.add(0, job)
        }
        jobsFlow.value = current
        FlintLogger.i(tag, "Saved job ID ${job.id} | Status: ${job.status.name} | Progress: ${job.progress}%")
        return FlintResult.Success(job)
    }

    override suspend fun cancelJob(jobId: String): FlintResult<Unit, AppError> {
        val current = jobsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == jobId }
        if (index >= 0) {
            val updated = current[index].copy(status = MediaJobStatus.CANCELLED)
            current[index] = updated
            jobsFlow.value = current
            FlintLogger.i(tag, "Cancelled job ID $jobId")
            return FlintResult.Success(Unit)
        }
        return FlintResult.Error(AppError.Validation("MediaJob with id $jobId not found"))
    }

    override fun observeReelCandidates(sourceId: String): Flow<List<ReelCandidate>> {
        return candidatesFlow.asStateFlow().map { list -> list.filter { it.sourceId == sourceId } }
    }

    override suspend fun getCandidateById(candidateId: String): FlintResult<ReelCandidate, AppError> {
        val candidate = candidatesFlow.value.find { it.id == candidateId }
        return if (candidate != null) {
            FlintResult.Success(candidate)
        } else {
            FlintResult.Error(AppError.Validation("ReelCandidate with id $candidateId not found"))
        }
    }

    override suspend fun saveCandidate(candidate: ReelCandidate): FlintResult<ReelCandidate, AppError> {
        val current = candidatesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == candidate.id }
        if (index >= 0) {
            current[index] = candidate
        } else {
            current.add(0, candidate)
        }
        candidatesFlow.value = current
        FlintLogger.i(tag, "Saved candidate ID ${candidate.id} | Title: ${candidate.title}")
        return FlintResult.Success(candidate)
    }

    override suspend fun deleteCandidate(candidateId: String): FlintResult<Unit, AppError> {
        val current = candidatesFlow.value.toMutableList()
        val removed = current.removeAll { it.id == candidateId }
        candidatesFlow.value = current
        FlintLogger.i(tag, "Deleted candidate ID $candidateId | Success: $removed")
        return FlintResult.Success(Unit)
    }
}
