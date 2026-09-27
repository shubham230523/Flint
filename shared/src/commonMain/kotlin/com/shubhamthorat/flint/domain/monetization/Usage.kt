package com.shubhamthorat.flint.domain.monetization

data class UserUsage(
    val aiGenerationsCount: Int = 0,
    val projectsCount: Int = 0,
    val campaignsCount: Int = 0,
    val mediaMinutesCount: Int = 0
)

object FreePlanLimits {
    const val MAX_AI_GENERATIONS = 10
    const val MAX_PROJECTS = 3
    const val MAX_CAMPAIGNS = 2
    const val MAX_MEDIA_MINUTES = 15
}

object MemberPlanLimits {
    const val MAX_AI_GENERATIONS = 500
    const val MAX_PROJECTS = 100
    const val MAX_CAMPAIGNS = 50
    const val MAX_MEDIA_MINUTES = 300
}

class QuotaCalculator(
    private val usage: UserUsage,
    private val isMember: Boolean
) {
    val maxGenerations: Int = if (isMember) MemberPlanLimits.MAX_AI_GENERATIONS else FreePlanLimits.MAX_AI_GENERATIONS

    fun canGenerateAi(): Boolean {
        return usage.aiGenerationsCount < maxGenerations
    }

    fun remainingAiGenerations(): Int {
        return (maxGenerations - usage.aiGenerationsCount).coerceAtLeast(0)
    }

    fun aiQuotaUsedPercentage(): Int {
        if (maxGenerations == 0) return 100
        return ((usage.aiGenerationsCount.toDouble() / maxGenerations) * 100).toInt().coerceIn(0, 100)
    }

    fun getQuotaReachedInfo(): String {
        return "Used ${usage.aiGenerationsCount}/$maxGenerations free AI generations. Upgrade to Flint Member for higher limits."
    }
}
