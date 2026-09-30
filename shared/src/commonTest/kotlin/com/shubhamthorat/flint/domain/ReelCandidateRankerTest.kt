package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.model.ReelCandidateStatus
import com.shubhamthorat.flint.domain.model.ReelCandidateType
import com.shubhamthorat.flint.domain.usecase.ReelCandidateRanker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReelCandidateRankerTest {

    @Test
    fun testCandidateRankingAndDeduplication() {
        val ranker = ReelCandidateRanker()

        val candidates = listOf(
            ReelCandidate(
                id = "cand_low",
                sourceId = "src_1",
                startTimeMs = 0L,
                endTimeMs = 30000L,
                transcript = "Low score candidate text",
                title = "Low Score",
                hook = "Okay hook",
                candidateScore = 0.5f,
                confidence = 0.6f
            ),
            ReelCandidate(
                id = "cand_high",
                sourceId = "src_1",
                startTimeMs = 60000L,
                endTimeMs = 95000L,
                transcript = "High score candidate text",
                title = "High Score",
                hook = "Amazing scroll stopping hook!",
                candidateScore = 0.95f,
                confidence = 0.90f
            ),
            ReelCandidate(
                id = "cand_invalid_dur",
                sourceId = "src_1",
                startTimeMs = 0L,
                endTimeMs = 5000L, // 5s < 15s min
                transcript = "Too short candidate",
                title = "Short Candidate",
                hook = "Short hook",
                candidateScore = 0.99f
            )
        )

        val ranked = ranker.rankCandidates(candidates, limit = 5)
        assertEquals(2, ranked.size)
        assertEquals("cand_high", ranked.first().id)
        assertEquals("cand_low", ranked[1].id)
    }
}
