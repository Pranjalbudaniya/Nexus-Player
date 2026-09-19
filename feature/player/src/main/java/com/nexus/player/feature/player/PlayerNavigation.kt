package com.nexus.player.feature.player

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.PlayerRoute

/**
 * Registers the type-safe Player destination on the navigation graph.
 */
fun NavGraphBuilder.playerScreen(
    onBackClick: () -> Unit = {}
) {
    composable<PlayerRoute> {
        PlayerRoute(
            onBackClick = onBackClick
        )
    }
}
