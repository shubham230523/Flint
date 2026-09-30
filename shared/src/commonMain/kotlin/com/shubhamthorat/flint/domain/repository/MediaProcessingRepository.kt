package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.MediaProcessingJob
import com.shubhamthorat.flint.domain.model.ReelCandidate
import kotlinx.coroutines.flow.Flow

interface MediaProcessingRepository {
    fun observeJobs(userId: String): Flow<List<MediaProcessingJob>>
    suspend fun getJobById(jobId: String): FlintResult<MediaProcessingJob, AppError>
    suspend fun saveJob(job: MediaProcessingJob): FlintResult<MediaProcessingJob, AppError>
    suspend fun cancelJob(jobId: String): FlintResult<Unit, AppError>

    fun observeReelCandidates(sourceId: String): Flow<List<ReelCandidate>>
    suspend fun getCandidateById(candidateId: String): FlintResult<ReelCandidate, AppError>
    suspend fun saveCandidate(candidate: ReelCandidate): FlintResult<ReelCandidate, AppError>
    suspend fun deleteCandidate(candidateId: String): FlintResult<Unit, AppError>
}
