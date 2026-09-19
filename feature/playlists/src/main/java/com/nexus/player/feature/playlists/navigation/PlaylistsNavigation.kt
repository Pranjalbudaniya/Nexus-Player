package com.nexus.player.feature.playlists.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.PlaylistDetailsRoute
import com.nexus.player.core.navigation.PlaylistsRoute
import com.nexus.player.feature.playlists.PlaylistsScreen
import com.nexus.player.feature.playlists.detail.PlaylistDetailRoute

fun NavController.navigateToPlaylists(navOptions: NavOptions? = null) {
    navigate(PlaylistsRoute, navOptions)
}

fun NavController.navigateToPlaylistDetail(playlistId: String, navOptions: NavOptions? = null) {
    navigate(PlaylistDetailsRoute(playlistId), navOptions)
}

fun NavGraphBuilder.playlistsScreen(
    onNavigateToPlaylist: (String) -> Unit
) {
    composable<PlaylistsRoute> {
        PlaylistsScreen(
            onNavigateToPlaylist = onNavigateToPlaylist
        )
    }
}

fun NavGraphBuilder.playlistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> }
) {
    composable<PlaylistDetailsRoute> {
        PlaylistDetailRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToLibrary = onNavigateToLibrary,
            onFolderClick = onFolderClick
        )
    }
}
