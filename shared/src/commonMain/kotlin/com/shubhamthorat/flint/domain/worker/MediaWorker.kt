package com.shubhamthorat.flint.domain.worker

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.MediaProcessingJob

interface MediaWorker {
    suspend fun processJob(job: MediaProcessingJob): FlintResult<MediaProcessingJob, AppError>
    suspend fun cancelJob(jobId: String): FlintResult<Unit, AppError>
}
