package com.nexus.player.feature.home

/**
 * UI presentation model for the Continue Watching horizontal carousel.
 */
data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val duration: String,
    val progress: Float,
    val quality: String,
    val playbackPositionMs: Long = 0L,
    val mediaUri: String = ""
)

/**
 * UI presentation model for the Recently Added horizontal section.
 */
data class RecentVideoItem(
    val id: String,
    val title: String,
    val technicalSpecs: String,
    val addedTimeAndFolder: String,
    val duration: String,
    val quality: String,
    val mediaUri: String = ""
)

/**
 * UI presentation model for the Favorites horizontal section.
 */
data class FavoriteVideoItem(
    val id: String,
    val title: String,
    val duration: String,
    val quality: String,
    val mediaUri: String = ""
)

/**
 * UI presentation model for the Folders horizontal section.
 */
data class FolderItem(
    val id: String,
    val name: String,
    val videoCountText: String,
    val previewMediaUri: String? = null
)

/**
 * Comprehensive UI State for the Home screen foundation.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Empty(
        val isScanning: Boolean = false,
        val noAccessibleMedia: Boolean = false
    ) : HomeUiState

    data class Error(
        val message: String
    ) : HomeUiState

    data class Success(
        val continueWatching: List<ContinueWatchingItem> = emptyList(),
        val recentlyAdded: List<RecentVideoItem> = emptyList(),
        val favorites: List<FavoriteVideoItem> = emptyList(),
        val folders: List<FolderItem> = emptyList(),
        val isScanning: Boolean = false,
        val hasAccessibleMedia: Boolean = true
    ) : HomeUiState {
        val isContinueWatchingEmpty: Boolean get() = continueWatching.isEmpty()
        val isRecentlyAddedEmpty: Boolean get() = recentlyAdded.isEmpty()
        val isFavoritesEmpty: Boolean get() = favorites.isEmpty()
        val isFoldersEmpty: Boolean get() = folders.isEmpty()
        val isAllEmpty: Boolean get() = isContinueWatchingEmpty && isRecentlyAddedEmpty && isFavoritesEmpty && isFoldersEmpty
    }
}
