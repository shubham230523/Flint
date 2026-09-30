package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaJobType {
    VIDEO_ANALYSIS,
    REEL_CANDIDATE_GENERATION,
    REEL_RENDER
}

@Serializable
enum class MediaJobStatus {
    QUEUED,
    DOWNLOADING,
    EXTRACTING_AUDIO,
    TRANSCRIBING,
    DETECTING_SCENES,
    ANALYZING,
    GENERATING_CANDIDATES,
    RENDERING,
    COMPLETED,
    FAILED,
    CANCELLED;

    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED || this == CANCELLED
}

@Serializable
enum class ReelCandidateType {
    EDUCATIONAL,
    STORY,
    OPINION,
    HOW_TO,
    SURPRISING_INSIGHT,
    QUOTE,
    PROBLEM_SOLUTION,
    LIST,
    DEMO
}

@Serializable
enum class ReelCandidateStatus {
    DISCOVERED,
    VERIFIED,
    ACCEPTED,
    REJECTED,
    EDITED,
    RENDERING,
    RENDERED,
    FAILED
}

@Serializable
data class ReelCandidate(
    val id: String,
    val sourceId: String,
    val jobId: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long = endTimeMs - startTimeMs,
    val transcript: String,
    val title: String,
    val hook: String,
    val reason: String = "",
    val contentType: ReelCandidateType = ReelCandidateType.EDUCATIONAL,
    val candidateScore: Float = 0.0f,
    val confidence: Float = 0.0f,
    val status: ReelCandidateStatus = ReelCandidateStatus.DISCOVERED,
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val ctaText: String = "",
    val layoutVariant: String = "DEFAULT"
)

@Serializable
data class MediaProcessingJob(
    val id: String,
    val sourceId: String,
    val userId: String,
    val type: MediaJobType,
    val status: MediaJobStatus = MediaJobStatus.QUEUED,
    val progress: Int = 0,
    val createdAt: Long = 0L,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val candidateIds: List<String> = emptyList()
) {
    fun canTransitionTo(newStatus: MediaJobStatus): Boolean {
        if (status == newStatus) return true
        if (status.isTerminal() && newStatus != MediaJobStatus.QUEUED) return false

        return when (status) {
            MediaJobStatus.QUEUED -> true
            MediaJobStatus.DOWNLOADING -> newStatus != MediaJobStatus.QUEUED
            MediaJobStatus.EXTRACTING_AUDIO -> newStatus != MediaJobStatus.DOWNLOADING
            MediaJobStatus.TRANSCRIBING -> newStatus != MediaJobStatus.EXTRACTING_AUDIO
            MediaJobStatus.DETECTING_SCENES -> newStatus != MediaJobStatus.TRANSCRIBING
            MediaJobStatus.ANALYZING -> newStatus != MediaJobStatus.DETECTING_SCENES
            MediaJobStatus.GENERATING_CANDIDATES -> newStatus != MediaJobStatus.ANALYZING
            MediaJobStatus.RENDERING -> newStatus != MediaJobStatus.GENERATING_CANDIDATES
            MediaJobStatus.COMPLETED, MediaJobStatus.FAILED, MediaJobStatus.CANCELLED -> false
        }
    }
}
