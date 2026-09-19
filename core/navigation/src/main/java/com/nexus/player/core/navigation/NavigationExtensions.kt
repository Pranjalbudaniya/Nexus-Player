package com.nexus.player.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions

/**
 * Top-level destination navigation helper enforcing tab state preservation,
 * single-top launching, and popping up to the start destination.
 */
fun NavController.navigateToTopLevelDestination(destination: TopLevelDestination) {
    val topLevelNavOptions = navOptions {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    navigate(destination.route, topLevelNavOptions)
}
