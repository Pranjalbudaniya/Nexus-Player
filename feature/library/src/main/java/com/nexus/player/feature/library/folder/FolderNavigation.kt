package com.nexus.player.feature.library.folder

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.FolderRoute

/**
 * Registers the type-safe Folder destination on the NavGraph.
 */
fun NavGraphBuilder.folderScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToFolder: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    onNavigateToPlayer: (videoId: String) -> Unit = {}
) {
    composable<FolderRoute> {
        FolderRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToFolder = onNavigateToFolder,
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}
