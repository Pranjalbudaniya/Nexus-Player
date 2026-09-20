package com.nexus.player.feature.more.analytics

import com.nexus.player.feature.more.analytics.model.WatchAnalyticsData

/**
 * UI State representation for the Playback Analytics screen.
 */
sealed interface AnalyticsUiState {

    /**
     * Initial loading state while room streams are processed.
     */
    data object Loading : AnalyticsUiState

    /**
     * Displayed when the user has never watched a video or has cleared history.
     */
    data object Empty : AnalyticsUiState

    /**
     * Rich state displaying computed watch statistics, charts, and rankings.
     */
    data class Success(
        val data: WatchAnalyticsData,
        val isClearDialogOpen: Boolean = false
    ) : AnalyticsUiState

    /**
     * Error state if calculation or database reading fails.
     */
    data class Error(val message: String) : AnalyticsUiState
}
