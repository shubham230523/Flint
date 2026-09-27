package com.shubhamthorat.flint.presentation.navigation

import com.shubhamthorat.flint.core.FlintLogger
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

    init {
        FlintLogger.i("NavigationManager", "Initialized with initialScreen: ${initialScreen.title}")
    }

    fun navigateTo(screen: FlintScreen, clearBackstack: Boolean = false) {
        if (clearBackstack) {
            FlintLogger.d("NavigationManager", "Clearing backstack prior to navigating to ${screen.title}")
            backstack.clear()
        }
        backstack.add(screen)
        _currentScreen.value = screen
        FlintLogger.i("NavigationManager", "Navigated to: ${screen.title} (route: ${screen.route}) | Backstack depth: ${backstack.size}")
    }

    fun pop(): Boolean {
        if (backstack.size > 1) {
            val popped = backstack.removeAt(backstack.size - 1)
            val topScreen = backstack.last()
            _currentScreen.value = topScreen
            FlintLogger.i("NavigationManager", "Popped screen: ${popped.title} | Active screen: ${topScreen.title} | Backstack depth: ${backstack.size}")
            return true
        }
        FlintLogger.w("NavigationManager", "Cannot pop root navigation screen: ${_currentScreen.value.title}")
        return false
    }
}
