package com.shubhamthorat.flint.presentation

import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarAnalyticsTest {

    @Test
    fun testScheduledAssetModel() {
        val asset = ContentAsset("1", "s1", "KMP 2.0 Launch", "Body text", ContentType.LINKEDIN_POST, ContentStatus.SCHEDULED, "LinkedIn")
        assertEquals("1", asset.id)
        assertEquals("KMP 2.0 Launch", asset.title)
        assertEquals("LinkedIn", asset.platform)
        assertEquals(ContentStatus.SCHEDULED, asset.status)
    }

    @Test
    fun testNavigationManagerCalendarAndAnalyticsRoutes() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)

        navManager.navigateTo(FlintScreen.Calendar)
        assertEquals(FlintScreen.Calendar, navManager.currentScreen.value)

        navManager.navigateTo(FlintScreen.Analytics)
        assertEquals(FlintScreen.Analytics, navManager.currentScreen.value)

        navManager.navigateTo(FlintScreen.Onboarding)
        assertEquals(FlintScreen.Onboarding, navManager.currentScreen.value)
    }
}