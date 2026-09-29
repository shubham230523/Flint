package com.shubhamthorat.flint.presentation.navigation

/**
 * Navigation destinations across Flint platforms.
 */
sealed class FlintScreen(
    val route: String,
    val title: String,
    val isTopLevel: Boolean = true
) {
    object Onboarding : FlintScreen("onboarding", "Onboarding", isTopLevel = false)
    object Authentication : FlintScreen("auth", "Sign In", isTopLevel = false)
    object Dashboard : FlintScreen("dashboard", "Dashboard")
    object Create : FlintScreen("create", "Create Spark")
    object Projects : FlintScreen("projects", "Projects")
    object ContentLibrary : FlintScreen("library", "Content Library")
    object Campaigns : FlintScreen("campaigns", "Campaigns")
    object Calendar : FlintScreen("calendar", "Calendar")
    object Analytics : FlintScreen("analytics", "Analytics")
    object CreatorDNA : FlintScreen("creator_dna", "Brand DNA")
    object YouTubeWorkspace : FlintScreen("youtube_workspace", "YouTube → Instagram")
    object Settings : FlintScreen("settings", "Settings")
    object Membership : FlintScreen("membership", "Membership")

    companion object {
        val topLevelScreens: List<FlintScreen>
            get() = listOf(
                Dashboard,
                Create,
                Projects,
                ContentLibrary,
                Campaigns,
                Calendar,
                Analytics,
                CreatorDNA,
                Settings,
                Membership
            )
    }
}
