package com.nexus.player.feature.library

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.LibraryRoute

/**
 * Registers the type-safe Library destination on the NavGraph.
 */
fun NavGraphBuilder.libraryScreen(
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> }
) {
    composable<LibraryRoute> {
        LibraryRoute(
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToSearch = onNavigateToSearch,
            onFolderClick = onFolderClick
        )
    }
}
