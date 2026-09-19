package com.nexus.player.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.SearchRoute
import com.nexus.player.feature.search.SearchScreen

/**
 * Registers the Global Search screen destination in the navigation graph.
 */
fun NavGraphBuilder.searchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> }
) {
    composable<SearchRoute> {
        SearchScreen(
            onBackClick = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onFolderClick = onFolderClick
        )
    }
}
