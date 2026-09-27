package com.shubhamthorat.flint.presentation

import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationTest {

    @Test
    fun testInitialNavigationState() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)
        assertEquals(FlintScreen.Dashboard, navManager.currentScreen.value)
        assertEquals(1, navManager.backstackSize)
    }

    @Test
    fun testNavigateToNewScreen() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)
        navManager.navigateTo(FlintScreen.Create)

        assertEquals(FlintScreen.Create, navManager.currentScreen.value)
        assertEquals(2, navManager.backstackSize)
    }

    @Test
    fun testPopBackstack() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)
        navManager.navigateTo(FlintScreen.Create)
        navManager.navigateTo(FlintScreen.ContentLibrary)

        assertTrue(navManager.pop())
        assertEquals(FlintScreen.Create, navManager.currentScreen.value)

        assertTrue(navManager.pop())
        assertEquals(FlintScreen.Dashboard, navManager.currentScreen.value)

        // Root cannot be popped
        assertFalse(navManager.pop())
        assertEquals(FlintScreen.Dashboard, navManager.currentScreen.value)
    }

    @Test
    fun testNavigateAndClearTop() {
        val navManager = NavigationManager(initialScreen = FlintScreen.Dashboard)
        navManager.navigateTo(FlintScreen.Create)
        navManager.navigateTo(FlintScreen.Settings)

        navManager.navigateTo(FlintScreen.Dashboard, clearBackstack = true)
        assertEquals(FlintScreen.Dashboard, navManager.currentScreen.value)
        assertEquals(1, navManager.backstackSize)
    }
}