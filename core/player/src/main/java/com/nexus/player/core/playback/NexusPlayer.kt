package com.nexus.player.core.playback

import androidx.media3.ui.PlayerView
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.NexusMediaItem
import com.nexus.player.core.playback.model.PlaybackPosition
import com.nexus.player.core.playback.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Nexus Player playback abstraction.
 *
 * All consumers (ViewModels, feature modules) interact with the player
 * exclusively through this interface. The underlying ExoPlayer instance
 * is never exposed.
 *
 * ## Threading
 *
 * All methods must be called from the **main thread** unless documented otherwise.
 * The [state] flow may be collected from any thread.
 *
 * ## Lifecycle
 *
 * Calling [release] frees player resources for the current session.
 */
interface NexusPlayer {

    /**
     * Reactive stream of the current player state.
     *
     * Position updates are emitted at a controlled interval (≈250ms) while
     * playing, not per-frame, to avoid excessive Compose recomposition.
     */
    val state: StateFlow<PlayerState>

    /**
     * Prepares the player with the given media item and begins loading.
     *
     * Transitions state: Idle → Loading → Ready (or Error).
     * If called while another media is loaded, the previous media is replaced.
     *
     * @param mediaItem The media to load for playback.
     * @param initialPositionMs Starting position in milliseconds (e.g. for resume).
     */
    fun prepare(mediaItem: NexusMediaItem, initialPositionMs: Long = 0L)

    /**
     * Starts or resumes playback.
     * No-op if the player is not in Ready or Paused state.
     */
    fun play()

    /**
     * Pauses playback.
     * No-op if the player is not currently playing.
     */
    fun pause()

    /**
     * Seeks to the specified position.
     *
     * @param positionMs Target position in milliseconds. Clamped to valid range.
     */
    fun seekTo(positionMs: Long)

    /**
     * Sets the playback speed.
     *
     * @param speed Playback speed multiplier (e.g., 0.5, 1.0, 1.5, 2.0).
     *              Values ≤ 0 are ignored.
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Configures the decoder selection mode.
     *
     * Currently only [DecoderMode.Hardware] is functional.
     * [DecoderMode.Software] and [DecoderMode.Auto] are accepted but
     * behave identically to Hardware until a native decoder backend is integrated.
     *
     * @param mode The desired decoder mode.
     * @see DecoderMode for platform limitations.
     */
    fun setDecoderMode(mode: DecoderMode)

    /**
     * Releases all player resources.
     *
     * After calling this method, the player instance must not be reused.
     * Cancels the position update ticker and releases the underlying ExoPlayer.
     */
    fun release()

    /**
     * Attaches an AndroidX Media3 [PlayerView] for video rendering.
     *
     * Automatically configures [PlayerView.setUseController] to false and
     * [PlayerView.setResizeMode] to [androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT].
     *
     * @param playerView The View to bind to the active playback engine.
     */
    fun attachPlayerView(playerView: PlayerView)

    /**
     * Detaches the given [playerView] (or the currently attached view if null), clearing its player reference.
     */
    fun detachPlayerView(playerView: PlayerView? = null)

    /**
     * Sets the video surface resize mode (e.g. FIT, ZOOM/FILL, STRETCH).
     *
     * @param resizeMode One of [androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT],
     *                   [androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM],
     *                   [androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL].
     */
    fun setVideoResizeMode(resizeMode: Int)

    /**
     * Sets the video scaling mode using the domain [com.nexus.player.core.playback.model.VideoScaleMode].
     */
    fun setVideoScaleMode(scaleMode: com.nexus.player.core.playback.model.VideoScaleMode)

    /**
     * Mutes or unmutes the audio output.
     *
     * @param muted True to mute, false to restore normal volume.
     */
    fun setMuted(muted: Boolean)

    /**
     * Selects an audio track by its track ID.
     */
    fun selectAudioTrack(trackId: String)

    /**
     * Selects a subtitle track by its track ID, or null to disable subtitles.
     */
    fun selectSubtitleTrack(trackId: String?)

    /**
     * Enables or disables subtitle display.
     */
    fun setSubtitlesEnabled(enabled: Boolean)

    /**
     * Sets an audio delay offset in milliseconds for audio/video sync.
     */
    fun setAudioDelayMs(delayMs: Long)

    /**
     * Sets a subtitle delay offset in milliseconds for subtitle/video sync.
     */
    fun setSubtitleDelayMs(delayMs: Long)

    /**
     * Adds an external subtitle track to the currently loaded media.
     */
    fun addExternalSubtitle(subtitle: com.nexus.player.core.playback.model.ExternalSubtitle)

    /**
     * Updates subtitle visual appearance styling (size, color, background, padding).
     */
    fun setSubtitleAppearance(appearance: com.nexus.player.core.playback.model.SubtitleAppearance)

    /**
     * Captures a snapshot of the current playback position.
     *
     * Returns null if no media is loaded or if the player has been released.
     * This is the hook for the Continue Watching system to persist progress.
     */
    fun getCurrentPlaybackPosition(): PlaybackPosition?
}
