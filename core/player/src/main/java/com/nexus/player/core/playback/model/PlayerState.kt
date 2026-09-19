package com.nexus.player.core.playback.model

/**
 * Reactive player state exposed to consumers (ViewModels, UI layers).
 *
 * Updated at a controlled frequency (not per-frame) to prevent
 * excessive Compose recompositions.
 */
data class PlayerState(
    val playbackState: PlaybackStatus = PlaybackStatus.Idle,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isSeekable: Boolean = false,
    val currentMediaId: String? = null,
    val error: PlaybackError? = null,
    val isMuted: Boolean = false,
    val resizeMode: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<PlayerTrack> = emptyList(),
    val subtitleTracks: List<PlayerTrack> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val areSubtitlesEnabled: Boolean = true,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L,
    val scaleMode: VideoScaleMode = VideoScaleMode.Fit,
    val subtitleAppearance: SubtitleAppearance = SubtitleAppearance.DEFAULT,
    val externalSubtitles: List<ExternalSubtitle> = emptyList()
) {
    val isLandscapeVideo: Boolean
        get() = if (videoWidth > 0 && videoHeight > 0) videoWidth >= videoHeight else true

    companion object {
        val INITIAL = PlayerState()
    }
}

/**
 * High-level playback status mapped from Media3 player states.
 */
enum class PlaybackStatus {
    /** Player created but no media loaded. */
    Idle,

    /** Media source is being prepared / opened. */
    Loading,

    /** Playback is stalled, waiting for more data. */
    Buffering,

    /** Media is prepared and ready for playback. */
    Ready,

    /** Playback reached the end of the media. */
    Ended,

    /** An unrecoverable playback error occurred. */
    Error
}
