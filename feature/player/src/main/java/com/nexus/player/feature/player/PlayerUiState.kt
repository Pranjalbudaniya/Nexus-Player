package com.nexus.player.feature.player

import androidx.compose.runtime.Immutable
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.PlaybackStatus

/**
 * UI state hierarchy for the Player destination.
 */
sealed interface PlayerUiState {

    /**
     * Preparing media and setting up the engine.
     */
    @Immutable
    data class Loading(
        val videoTitle: String? = null
    ) : PlayerUiState

    /**
     * Media is prepared and active.
     */
    @Immutable
    data class Ready(
        val videoId: String,
        val videoTitle: String,
        val isPlaying: Boolean = false,
        val playbackStatus: PlaybackStatus = PlaybackStatus.Ready,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val bufferedPositionMs: Long = 0L,
        val isSeekable: Boolean = true,
        val controlsVisible: Boolean = true,
        val playbackSpeed: Float = 1.0f,
        val isMuted: Boolean = false,
        val resizeMode: Int = 0,
        val scaleMode: com.nexus.player.core.playback.model.VideoScaleMode = com.nexus.player.core.playback.model.VideoScaleMode.Fit,
        val isOrientationLocked: Boolean = false,
        val isFullscreen: Boolean = false,
        val isPanelOpen: Boolean = false,
        val seekDurationSeconds: Int = 10,
        val isAutoNextEnabled: Boolean = false,
        val audioBoostPercent: Int = 100,
        val isEqualizerEnabled: Boolean = false,
        val equalizerPreset: String = "Flat",
        val sleepTimerRemainingSeconds: Long? = null,
        val volumePercent: Int = 100,
        val brightnessPercent: Int = 50,
        val zoom: Float = 1.0f,
        val panOffsetX: Float = 0f,
        val panOffsetY: Float = 0f
    ) : PlayerUiState {
        val isBuffering: Boolean
            get() = playbackStatus == PlaybackStatus.Buffering

        val isEnded: Boolean
            get() = playbackStatus == PlaybackStatus.Ended

        val progressFraction: Float
            get() = if (durationMs > 0L) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

        val bufferedFraction: Float
            get() = if (durationMs > 0L) {
                (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    /**
     * Playback encountered an error.
     */
    @Immutable
    data class Error(
        val category: ErrorCategory,
        val userMessage: String,
        val technicalDetail: String? = null,
        val canRetry: Boolean = true
    ) : PlayerUiState
}
