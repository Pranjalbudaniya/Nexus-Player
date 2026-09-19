package com.nexus.player.core.playback.queue

/**
 * Immutable reactive snapshot of the active playback queue session.
 *
 * Designed to be lightweight and memory-efficient even with thousands of items,
 * storing only stable video IDs rather than full Room entities.
 *
 * @param items Original sequence of video IDs as provided by the source.
 * @param playbackOrder Active sequence of video IDs (original or shuffled).
 * @param currentIndex Zero-based index of the currently active video in [playbackOrder].
 * @param currentVideoId The stable video ID currently loaded or playing.
 * @param previousVideoId ID of the preceding video in [playbackOrder], if one exists.
 * @param nextVideoId ID of the succeeding video in [playbackOrder], if one exists.
 * @param hasPrevious True if navigating to a previous queue item is possible.
 * @param hasNext True if navigating to a subsequent queue item is possible.
 * @param repeatMode The active repeat mode (OFF, REPEAT_ONE, REPEAT_ALL).
 * @param isShuffleEnabled True if the active order is randomized.
 * @param isAutoNextEnabled True if the player automatically advances upon video completion.
 * @param source Originating context from which this queue was created.
 */
data class PlaybackQueueState(
    val items: List<String> = emptyList(),
    val playbackOrder: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val currentVideoId: String? = null,
    val previousVideoId: String? = null,
    val nextVideoId: String? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val isAutoNextEnabled: Boolean = false,
    val source: QueueSource = QueueSource.None
) {
    /**
     * Total number of items in the active queue.
     */
    val size: Int get() = playbackOrder.size

    /**
     * Whether the active queue has zero items.
     */
    val isEmpty: Boolean get() = playbackOrder.isEmpty()
}
