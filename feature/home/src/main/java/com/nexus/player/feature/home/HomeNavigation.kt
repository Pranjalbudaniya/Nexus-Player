package com.nexus.player.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.HomeRoute

/**
 * Registers the type-safe Home destination on the NavGraph.
 */
fun NavGraphBuilder.homeScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> }
) {
    composable<HomeRoute> {
        HomeRoute(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSearch = onNavigateToSearch,
            onVideoClick = onVideoClick,
            onFolderClick = onFolderClick
        )
    }
}
