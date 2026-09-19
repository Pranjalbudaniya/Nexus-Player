package com.nexus.player.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations represented in the Nexus Player bottom navigation bar.
 *
 * NOTE: Settings is strictly NOT a top-level destination in the bottom navigation.
 */
enum class TopLevelDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: NexusRoute
) {
    HOME(
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = HomeRoute
    ),
    LIBRARY(
        title = "Library",
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
        route = LibraryRoute
    ),
    PLAYLISTS(
        title = "Playlists",
        selectedIcon = Icons.Filled.Subscriptions,
        unselectedIcon = Icons.Outlined.Subscriptions,
        route = PlaylistsRoute
    ),
    MORE(
        title = "More",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        route = MoreRoute
    )
}
