package com.shubhamthorat.flint.presentation

import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import kotlin.test.Test
import kotlin.test.assertEquals

class UiScreensTest {

    @Test
    fun testScreenRoutesAndTitles() {
        assertEquals("dashboard", FlintScreen.Dashboard.route)
        assertEquals("Dashboard", FlintScreen.Dashboard.title)

        assertEquals("create", FlintScreen.Create.route)
        assertEquals("Create Spark", FlintScreen.Create.title)

        assertEquals("library", FlintScreen.ContentLibrary.route)
        assertEquals("Content Library", FlintScreen.ContentLibrary.title)

        assertEquals("campaigns", FlintScreen.Campaigns.route)
        assertEquals("Campaigns", FlintScreen.Campaigns.title)

        assertEquals("creator_dna", FlintScreen.CreatorDNA.route)
        assertEquals("Creator DNA", FlintScreen.CreatorDNA.title)

        assertEquals("settings", FlintScreen.Settings.route)
        assertEquals("Settings", FlintScreen.Settings.title)
    }

    @Test
    fun testNavigationManagerTransitionsAcrossAllScreens() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)

        FlintScreen.topLevelScreens.forEach { screen ->
            navManager.navigateTo(screen)
            assertEquals(screen, navManager.currentScreen.value)
        }
    }
}