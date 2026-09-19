package com.nexus.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nexus.player.core.navigation.FolderRoute
import com.nexus.player.core.navigation.HomeRoute
import com.nexus.player.core.navigation.LibraryRoute
import com.nexus.player.core.navigation.MoreRoute
import com.nexus.player.core.navigation.NexusRoute
import com.nexus.player.core.navigation.OnboardingRoute
import com.nexus.player.core.navigation.PlayerRoute
import com.nexus.player.core.navigation.PlaylistDetailsRoute
import com.nexus.player.core.navigation.PlaylistsRoute
import com.nexus.player.core.navigation.SearchRoute
import com.nexus.player.core.navigation.SettingsRoute
import com.nexus.player.feature.home.homeScreen
import com.nexus.player.feature.library.folder.folderScreen
import com.nexus.player.feature.library.libraryScreen
import com.nexus.player.feature.onboarding.onboardingScreen
import com.nexus.player.feature.player.playerScreen
import com.nexus.player.feature.playlists.navigation.playlistDetailScreen
import com.nexus.player.feature.playlists.navigation.playlistsScreen
import com.nexus.player.feature.search.navigation.searchScreen

/**
 * Top-level Navigation Host for Nexus Player.
 *
 * Configures type-safe destinations for the Onboarding flow, 4 primary tabs, and deep screens.
 */
@Composable
fun NexusNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: NexusRoute = HomeRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding Flow
        onboardingScreen(
            onOnboardingFinished = {
                navController.navigate(HomeRoute) {
                    popUpTo(OnboardingRoute) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )

        // Tab 1: Home
        homeScreen(
            onNavigateToSettings = {
                navController.navigate(SettingsRoute)
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute)
            },
            onVideoClick = { videoId ->
                navController.navigate(PlayerRoute(videoId))
            },
            onFolderClick = { folderPath, folderName ->
                navController.navigate(FolderRoute(folderPath, folderName))
            }
        )

        // Tab 2: Library
        libraryScreen(
            onNavigateToPlayer = { videoId ->
                navController.navigate(PlayerRoute(videoId))
            },
            onNavigateToSearch = {
                navController.navigate(SearchRoute)
            },
            onFolderClick = { folderPath, folderName ->
                navController.navigate(FolderRoute(folderPath, folderName))
            }
        )

        // Deep Destination: Folder Browser
        folderScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToFolder = { folderPath, folderName ->
                navController.navigate(FolderRoute(folderPath, folderName))
            },
            onNavigateToPlayer = { videoId ->
                navController.navigate(PlayerRoute(videoId))
            }
        )

        // Tab 3: Playlists
        playlistsScreen(
            onNavigateToPlaylist = { playlistId ->
                navController.navigate(PlaylistDetailsRoute(playlistId))
            }
        )

        // Playlist Detail Screen
        playlistDetailScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToPlayer = { videoId ->
                navController.navigate(PlayerRoute(videoId))
            },
            onNavigateToLibrary = {
                navController.navigate(LibraryRoute)
            },
            onFolderClick = { folderPath, folderName ->
                navController.navigate(FolderRoute(folderPath, folderName))
            }
        )

        // Tab 4: More
        composable<MoreRoute> {
            NexusPlaceholderScreen(
                title = "More",
                subtitle = "Playback history, analytics, network streams, and tools",
                badgeText = "More Tab"
            )
        }

        // Deep Destination: Settings (Strictly outside bottom navigation)
        composable<SettingsRoute> {
            NexusPlaceholderScreen(
                title = "Settings",
                subtitle = "Appearance, video decoding, audio passthrough, and preferences",
                badgeText = "Deep Screen",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Deep Destination: Player
        playerScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )

        // Deep Destination: Search
        searchScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToPlayer = { videoId ->
                navController.navigate(PlayerRoute(videoId))
            },
            onFolderClick = { folderPath, folderName ->
                navController.navigate(FolderRoute(folderPath, folderName))
            }
        )
    }
}
