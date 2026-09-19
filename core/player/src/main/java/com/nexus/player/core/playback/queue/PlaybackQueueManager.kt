package com.nexus.player.core.playback.queue

import kotlinx.coroutines.flow.StateFlow

/**
 * Result returned when executing the Previous playback action.
 */
sealed interface PreviousResult {
    /**
     * Video has played past the restart threshold; seek back to 0:00.
     */
    data object RestartCurrent : PreviousResult

    /**
     * Video is near beginning or user triggered previous within threshold; navigate to video ID.
     */
    data class PlayVideo(val videoId: String) : PreviousResult

    /**
     * No previous queue item exists and video is already near 0:00.
     */
    data object None : PreviousResult
}

/**
 * Manages the ordered playback queue session independently of the Media3 player.
 *
 * Responsibilities:
 * - Maintains active queue and separate shuffled order.
 * - Handles Next / Previous actions with sensible restart thresholds.
 * - Supports repeat modes (Off, Repeat One, Repeat All).
 * - Supports non-destructive shuffle and queue reordering.
 * - Emits reactive state for UI consumption.
 */
interface PlaybackQueueManager {

    companion object {
        /**
         * Playback duration threshold (in milliseconds) after which pressing Previous
         * restarts the current video instead of jumping to the previous queue item.
         */
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3000L
    }

    /**
     * Current reactive snapshot of the playback queue.
     */
    val queueState: StateFlow<PlaybackQueueState>

    /**
     * Replaces the active queue with [items], setting [initialVideoId] as the active item.
     *
     * @param items List of stable video IDs.
     * @param initialVideoId Video ID to start with, or null to start from the first element.
     * @param source Logical source from which the queue was created.
     */
    fun setQueue(
        items: List<String>,
        initialVideoId: String? = null,
        source: QueueSource = QueueSource.None
    )

    /**
     * Advances to the next playable queue item respecting repeat and shuffle modes.
     *
     * @return The next video ID, or null if the end of the queue has been reached.
     */
    fun playNext(): String?

    /**
     * Handles the Previous playback action taking the current playback position into account.
     *
     * @param currentPositionMs The current video playback position in milliseconds.
     * @return [PreviousResult] indicating whether to restart the current video, play a previous video, or do nothing.
     */
    fun playPrevious(currentPositionMs: Long): PreviousResult

    /**
     * Sets the active queue item directly by video ID.
     *
     * @return True if the item was found in the active queue and selected, false otherwise.
     */
    fun playItem(videoId: String): Boolean

    /**
     * Removes an item from the queue by video ID.
     *
     * If the currently playing item is removed, the manager safely selects the next appropriate item.
     *
     * @return The new video ID to load if the active item was removed, or null if the queue is now empty.
     */
    fun removeItem(videoId: String): String?

    /**
     * Clears all items from the active queue session.
     */
    fun clearQueue()

    /**
     * Moves an item in the active playback order from [fromIndex] to [toIndex].
     */
    fun moveItem(fromIndex: Int, toIndex: Int)

    /**
     * Reorders the active playback order to match [newOrder].
     */
    fun reorderQueue(newOrder: List<String>)

    /**
     * Sets the active repeat mode.
     */
    fun setRepeatMode(mode: RepeatMode)

    /**
     * Cycles to the next repeat mode and returns it.
     */
    fun cycleRepeatMode(): RepeatMode

    /**
     * Enables or disables shuffle mode without destroying the original queue ordering.
     */
    fun setShuffleEnabled(enabled: Boolean)

    /**
     * Toggles shuffle mode and returns the new enabled state.
     */
    fun toggleShuffle(): Boolean

    /**
     * Sets whether auto-next playback is enabled upon video completion.
     */
    fun setAutoNextEnabled(enabled: Boolean)
}
