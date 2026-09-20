package com.nexus.player.feature.more

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nexus.player.core.navigation.AnalyticsRoute
import com.nexus.player.core.navigation.HistoryRoute
import com.nexus.player.core.navigation.MoreRoute
import com.nexus.player.core.navigation.SettingsRoute
import com.nexus.player.feature.more.analytics.AnalyticsRoute
import com.nexus.player.feature.more.history.HistoryRoute
import com.nexus.player.feature.more.settings.SettingsRoute

fun NavGraphBuilder.moreScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit
) {
    composable<MoreRoute> {
        MoreRoute(
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToSettings = onNavigateToSettings,
            onFolderClick = onFolderClick
        )
    }
}

fun NavGraphBuilder.historyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit
) {
    composable<HistoryRoute> {
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
    composable<AnalyticsRoute> {
        AnalyticsRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}

fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit
) {
    composable<SettingsRoute> {
        SettingsRoute(
            onNavigateBack = onNavigateBack
        )
    }
}

