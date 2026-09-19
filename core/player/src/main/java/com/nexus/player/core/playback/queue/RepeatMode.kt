package com.nexus.player.core.playback.queue

/**
 * Repeat modes supported by the Nexus Player playback session.
 */
enum class RepeatMode {
    /**
     * Stop after playback reaches the final item in the queue.
     */
    OFF,

    /**
     * Replay the currently playing video after completion.
     */
    REPEAT_ONE,

    /**
     * After the final queue item completes, loop back to the first item in the active playback order.
     */
    REPEAT_ALL;

    /**
     * Cycles through repeat modes in standard order: OFF -> REPEAT_ALL -> REPEAT_ONE -> OFF.
     */
    fun next(): RepeatMode = when (this) {
        OFF -> REPEAT_ALL
        REPEAT_ALL -> REPEAT_ONE
        REPEAT_ONE -> OFF
    }
}
