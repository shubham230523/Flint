package com.shubhamthorat.flint.domain.monetization

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow

enum class MembershipPlan {
    FREE, PRO_MONTHLY, PRO_ANNUAL
}

data class MembershipStatus(
    val plan: MembershipPlan = MembershipPlan.FREE,
    val isActive: Boolean = false,
    val expirationTimestamp: Long = 0L
)

interface BillingProvider {
    val membershipStatusFlow: Flow<MembershipStatus>
    suspend fun getMembershipStatus(): FlintResult<MembershipStatus, AppError>
    suspend fun purchasePlan(plan: MembershipPlan): FlintResult<MembershipStatus, AppError>
    suspend fun restorePurchases(): FlintResult<MembershipStatus, AppError>
}
