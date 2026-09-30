package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.MediaJobStatus
import com.shubhamthorat.flint.domain.model.MediaProcessingJob
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.MediaProcessingRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreMediaProcessingRepository(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : MediaProcessingRepository {

    private val tag = "FirestoreMediaProcessingRepository"
    private val firestore = Firebase.firestore

    private suspend fun getUserJobsCollection(): dev.gitlive.firebase.firestore.CollectionReference? {
        val uid = authRepository.getCurrentUser()?.id
        return if (!uid.isNullOrBlank()) {
            firestore.collection("users").document(uid).collection("mediaJobs")
        } else {
            null
        }
    }

    private suspend fun getUserCandidatesCollection(): dev.gitlive.firebase.firestore.CollectionReference? {
        val uid = authRepository.getCurrentUser()?.id
        return if (!uid.isNullOrBlank()) {
            firestore.collection("users").document(uid).collection("reelCandidates")
        } else {
            null
        }
    }

    override fun observeJobs(userId: String): Flow<List<MediaProcessingJob>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(user.id).collection("mediaJobs")
                    .snapshots
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<MediaProcessingJob>()
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse job doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                    }
            }
        }
    }

    override suspend fun getJobById(jobId: String): FlintResult<MediaProcessingJob, AppError> {
        val col = getUserJobsCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access jobs."))

        return try {
            val doc = col.document(jobId).get()
            if (doc.exists) {
                FlintResult.Success(doc.data<MediaProcessingJob>())
            } else {
                FlintResult.Error(AppError.Validation("Media processing job $jobId not found"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to fetch job $jobId: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun saveJob(job: MediaProcessingJob): FlintResult<MediaProcessingJob, AppError> {
        val col = getUserJobsCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save jobs."))

        return try {
            col.document(job.id).set(job)
            FlintLogger.i(tag, "Saved job ID ${job.id} to Firestore")
            FlintResult.Success(job)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to save job ${job.id}: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun cancelJob(jobId: String): FlintResult<Unit, AppError> {
        val col = getUserJobsCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to cancel jobs."))

        return try {
            val doc = col.document(jobId).get()
            if (doc.exists) {
                val currentJob = doc.data<MediaProcessingJob>()
                col.document(jobId).set(currentJob.copy(status = MediaJobStatus.CANCELLED))
                FlintResult.Success(Unit)
            } else {
                FlintResult.Error(AppError.Validation("Job $jobId not found"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to cancel job $jobId: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override fun observeReelCandidates(sourceId: String): Flow<List<ReelCandidate>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(user.id).collection("reelCandidates")
                    .snapshots
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try {
                                val candidate = doc.data<ReelCandidate>()
                                if (candidate.sourceId == sourceId) candidate else null
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse candidate doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                    }
            }
        }
    }

    override suspend fun getCandidateById(candidateId: String): FlintResult<ReelCandidate, AppError> {
        val col = getUserCandidatesCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access candidates."))

        return try {
            val doc = col.document(candidateId).get()
            if (doc.exists) {
                FlintResult.Success(doc.data<ReelCandidate>())
            } else {
                FlintResult.Error(AppError.Validation("ReelCandidate $candidateId not found"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to fetch candidate $candidateId: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun saveCandidate(candidate: ReelCandidate): FlintResult<ReelCandidate, AppError> {
        val col = getUserCandidatesCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save candidate."))

        return try {
            col.document(candidate.id).set(candidate)
            FlintLogger.i(tag, "Saved Reel candidate ID ${candidate.id} to Firestore")
            FlintResult.Success(candidate)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to save candidate ${candidate.id}: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun deleteCandidate(candidateId: String): FlintResult<Unit, AppError> {
        val col = getUserCandidatesCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete candidate."))

        return try {
            col.document(candidateId).delete()
            FlintLogger.i(tag, "Deleted candidate ID $candidateId from Firestore")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to delete candidate $candidateId: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }
}
