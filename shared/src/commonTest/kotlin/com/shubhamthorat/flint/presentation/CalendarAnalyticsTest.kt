package com.shubhamthorat.flint.presentation

import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.screen.ScheduledPost
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarAnalyticsTest {

    @Test
    fun testScheduledPostModel() {
        val post = ScheduledPost("1", "KMP 2.0 Launch", "LinkedIn", "Tomorrow", "10:00 AM")
        assertEquals("1", post.id)
        assertEquals("KMP 2.0 Launch", post.title)
        assertEquals("LinkedIn", post.platform)
        assertEquals("SCHEDULED", post.status)
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