package com.nexus.player.core.database.model

/**
 * Clean domain representation of an item in a playlist.
 *
 * If the underlying video file has been deleted, moved, or is missing from
 * the media store, [video] will be null and [isAvailable] will be false.
 * This allows the UI to show an unavailable indicator rather than crashing
 * or discarding the user's curated playlist entry.
 */
data class PlaylistItem(
    val id: Long,
    val playlistId: String,
    val videoId: String,
    val position: Int,
    val addedAt: Long,
    val video: Video?,
    val isAvailable: Boolean = video != null
)
