package com.shubhamthorat.flint.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationManager(
    initialScreen: FlintScreen = FlintScreen.Dashboard
) {
    private val backstack = mutableListOf<FlintScreen>(initialScreen)

    private val _currentScreen = MutableStateFlow<FlintScreen>(initialScreen)
    val currentScreen: StateFlow<FlintScreen> = _currentScreen.asStateFlow()

    val backstackSize: Int
        get() = backstack.size

    fun navigateTo(screen: FlintScreen, clearBackstack: Boolean = false) {
        if (clearBackstack) {
            backstack.clear()
        }
        backstack.add(screen)
        _currentScreen.value = screen
    }

    fun pop(): Boolean {
        if (backstack.size > 1) {
            backstack.removeAt(backstack.size - 1)
            _currentScreen.value = backstack.last()
            return true
        }
        return false
    }
}
