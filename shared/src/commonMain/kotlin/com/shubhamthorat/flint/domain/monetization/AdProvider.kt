package com.shubhamthorat.flint.domain.monetization

enum class AdPlacement {
    DASHBOARD,
    CONTENT_LIBRARY,
    DISCOVERY,
    EMPTY_STATE,
    ACTIVE_GENERATION,
    ACTIVE_EDITING,
    PAYMENT_FLOW,
    AUTHENTICATION
}

object AdPolicy {
    fun isAdAllowed(isMember: Boolean, placement: AdPlacement): Boolean {
        if (isMember) return false

        return when (placement) {
            AdPlacement.DASHBOARD,
            AdPlacement.CONTENT_LIBRARY,
            AdPlacement.DISCOVERY,
            AdPlacement.EMPTY_STATE -> true

            AdPlacement.ACTIVE_GENERATION,
            AdPlacement.ACTIVE_EDITING,
            AdPlacement.PAYMENT_FLOW,
            AdPlacement.AUTHENTICATION -> false
        }
    }
}

interface AdProvider {
    fun isAdAvailable(placement: AdPlacement): Boolean
    fun showBannerAd(placement: AdPlacement)
}
