package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.monetization.AdPlacement
import com.shubhamthorat.flint.domain.monetization.AdPolicy
import com.shubhamthorat.flint.domain.monetization.FreePlanLimits
import com.shubhamthorat.flint.domain.monetization.MemberPlanLimits
import com.shubhamthorat.flint.domain.monetization.UserUsage
import com.shubhamthorat.flint.domain.monetization.QuotaCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationTest {

    @Test
    fun testFreePlanQuotaCalculatorWithinLimits() {
        val usage = UserUsage(aiGenerationsCount = 5, projectsCount = 2)
        val calculator = QuotaCalculator(usage = usage, isMember = false)

        assertTrue(calculator.canGenerateAi())
        assertEquals(5, calculator.remainingAiGenerations())
        assertEquals(50, calculator.aiQuotaUsedPercentage())
    }

    @Test
    fun testFreePlanQuotaReachedProvidesUpgradeOption() {
        val usage = UserUsage(aiGenerationsCount = FreePlanLimits.MAX_AI_GENERATIONS)
        val calculator = QuotaCalculator(usage = usage, isMember = false)

        assertFalse(calculator.canGenerateAi())
        assertEquals(0, calculator.remainingAiGenerations())
        assertEquals(100, calculator.aiQuotaUsedPercentage())
        assertTrue(calculator.getQuotaReachedInfo().contains("10/10"))
    }

    @Test
    fun testMemberPlanQuotaCalculatorSubstantiallyHigher() {
        val usage = UserUsage(aiGenerationsCount = 50)
        val calculator = QuotaCalculator(usage = usage, isMember = true)

        assertTrue(calculator.canGenerateAi())
        assertEquals(MemberPlanLimits.MAX_AI_GENERATIONS - 50, calculator.remainingAiGenerations())
    }

    @Test
    fun testAdPolicyRulesForFreeAndMemberUsers() {
        // Free users see ads on non-critical screens
        assertTrue(
            AdPolicy.isAdAllowed(
                isMember = false,
                placement = AdPlacement.DASHBOARD,
            )
        )

        // Free users DO NOT see ads during active generation or editing
        assertFalse(
            AdPolicy.isAdAllowed(
                isMember = false,
                placement = AdPlacement.ACTIVE_GENERATION
            )
        )

        // Members do not see ads
        assertFalse(
            AdPolicy.isAdAllowed(
                isMember = true,
                placement = AdPlacement.DASHBOARD
            )
        )
    }
}