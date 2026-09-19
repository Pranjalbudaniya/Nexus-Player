package com.nexus.player.core.playback.model

/**
 * Snapshot of the current playback position for persistence.
 *
 * Exposes enough data for the existing Continue Watching system
 * to record and resume playback progress.
 *
 * @param mediaId Stable identifier matching [NexusMediaItem.mediaId].
 * @param positionMs Current playback position in milliseconds.
 * @param durationMs Total media duration in milliseconds.
 * @param completionPercentage Clamped [0.0, 1.0] ratio of position/duration.
 * @param timestamp Wall-clock epoch millis when this snapshot was captured.
 */
data class PlaybackPosition(
    val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completionPercentage: Float,
    val timestamp: Long
)
