package com.nexus.player.feature.more

/**
 * UI presentation model for a playback history item.
 */
data class HistoryItem(
    val id: String,
    val title: String,
    val duration: String,
    val durationMs: Long,
    val progress: Float,
    val playbackPositionMs: Long,
    val isCompleted: Boolean,
    val quality: String,
    val mediaUri: String,
    val folderName: String,
    val lastPlayedRelative: String,
    val lastPlayedAt: Long,
    val watchCount: Int
)

/**
 * Summary watch-state statistics.
 */
data class WatchStats(
    val totalWatched: Int = 0,
    val completedCount: Int = 0,
    val favoritesCount: Int = 0
)

/**
 * UI state for the More hub and Watch History views.
 */
sealed interface MoreUiState {
    data object Loading : MoreUiState

    data class Success(
        val history: List<HistoryItem> = emptyList(),
        val stats: WatchStats = WatchStats(),
        val isClearAllDialogOpen: Boolean = false
    ) : MoreUiState {
        val isHistoryEmpty: Boolean get() = history.isEmpty()
    }

    data class Error(val message: String) : MoreUiState
}
