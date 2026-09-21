package com.nexus.player.core.navigation

import kotlinx.serialization.Serializable

/**
 * Marker interface for all type-safe navigation routes in Nexus Player.
 */
sealed interface NexusRoute

// =========================================================================
// Top-Level Bottom Navigation Destinations
// =========================================================================

@Serializable
data object HomeRoute : NexusRoute

@Serializable
data object LibraryRoute : NexusRoute

@Serializable
data object PlaylistsRoute : NexusRoute

@Serializable
data object MoreRoute : NexusRoute

// =========================================================================
// Deep Feature Destinations (Architectural Contracts)
// =========================================================================

/**
 * Video Player destination.
 * Note: videoId is mandatory for playback.
 */
@Serializable
data class PlayerRoute(val videoId: String) : NexusRoute

/**
 * Folder browser destination.
 */
@Serializable
data class FolderRoute(val folderPath: String, val folderName: String) : NexusRoute

/**
 * Series / Season video collection destination.
 */
@Serializable
data class SeriesRoute(val seriesId: String) : NexusRoute

/**
 * Video details sheet/screen destination.
 */
@Serializable
data class VideoDetailsRoute(val videoId: String) : NexusRoute

/**
 * Global Search destination.
 */
@Serializable
data object SearchRoute : NexusRoute

/**
 * Application Settings destination.
 * NOTE: Accessed via top-right action, strictly NOT part of bottom navigation.
 */
@Serializable
data object SettingsRoute : NexusRoute

/**
 * Storage and scan locations management destination.
 */
@Serializable
data object StorageLocationsRoute : NexusRoute

/**
 * Playlist detail view destination.
 */
@Serializable
data class PlaylistDetailsRoute(val playlistId: String) : NexusRoute

/**
 * Favorites collection destination.
 */
@Serializable
data object FavoritesRoute : NexusRoute

/**
 * Playback History destination.
 */
@Serializable
data object HistoryRoute : NexusRoute

/**
 * Playback Analytics destination.
 */
@Serializable
data object AnalyticsRoute : NexusRoute

/**
 * First-run Onboarding destination.
 */
@Serializable
data object OnboardingRoute : NexusRoute
