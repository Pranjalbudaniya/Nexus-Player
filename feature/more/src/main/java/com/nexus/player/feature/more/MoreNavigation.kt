package com.nexus.player.feature.more

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.AnalyticsRoute as AnalyticsDestination
import com.nexus.player.core.navigation.FavoritesRoute as FavoritesDestination
import com.nexus.player.core.navigation.HistoryRoute as HistoryDestination
import com.nexus.player.core.navigation.MoreRoute as MoreDestination
import com.nexus.player.core.navigation.SettingsRoute as SettingsDestination
import com.nexus.player.core.navigation.StorageLocationsRoute as StorageLocationsDestination
import com.nexus.player.feature.more.analytics.AnalyticsRoute
import com.nexus.player.feature.more.favorites.FavoritesRoute
import com.nexus.player.feature.more.history.HistoryRoute
import com.nexus.player.feature.more.settings.SettingsRoute
import com.nexus.player.feature.more.storage.StorageLocationsRoute

fun NavGraphBuilder.moreScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToStorageLocations: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> }
) {
    composable<MoreDestination> {
        MoreRoute(
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToFavorites = onNavigateToFavorites,
            onNavigateToPlaylists = onNavigateToPlaylists,
            onNavigateToStorageLocations = onNavigateToStorageLocations,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}

fun NavGraphBuilder.historyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit
) {
    composable<HistoryDestination> {
        HistoryRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onFolderClick = onFolderClick
        )
    }
}

fun NavGraphBuilder.analyticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    composable<AnalyticsDestination> {
        AnalyticsRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}

fun NavGraphBuilder.favoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit
) {
    composable<FavoritesDestination> {
        FavoritesRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onFolderClick = onFolderClick
        )
    }
}

fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStorageLocations: () -> Unit = {}
) {
    composable<SettingsDestination> {
        SettingsRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToStorageLocations = onNavigateToStorageLocations
        )
    }
}

fun NavGraphBuilder.storageLocationsScreen(
    onNavigateBack: () -> Unit
) {
    composable<StorageLocationsDestination> {
        StorageLocationsRoute(
            onNavigateBack = onNavigateBack
        )
    }
}
