package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.domain.model.ReelCandidate

class ReelCandidateRanker {

    fun rankCandidates(candidates: List<ReelCandidate>, limit: Int = 10): List<ReelCandidate> {
        if (candidates.isEmpty()) return emptyList()

        return candidates
            .filter { candidate ->
                val durationSec = candidate.durationMs / 1000.0
                durationSec in 15.0..180.0 && candidate.hook.isNotBlank()
            }
            .sortedByDescending { it.candidateScore * 0.7f + it.confidence * 0.3f }
            .distinctBy { candidate ->
                // Deduplicate candidates with >80% overlapping start time
                candidate.startTimeMs / 5000L
            }
            .take(limit)
    }
}
