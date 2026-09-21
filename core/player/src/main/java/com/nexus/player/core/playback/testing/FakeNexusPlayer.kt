package com.nexus.player.core.playback.testing

import androidx.media3.ui.PlayerView
import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.NexusMediaItem
import com.nexus.player.core.playback.model.PlaybackError
import com.nexus.player.core.playback.model.PlaybackPosition
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fully controllable fake [NexusPlayer] for unit and UI tests.
 *
 * All state transitions are manually triggered via public helper methods.
 * No Android/ExoPlayer dependencies required.
 */
class FakeNexusPlayer : NexusPlayer {

    private val _state = MutableStateFlow(PlayerState.INITIAL)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Track method calls for verification. */
    var preparedMediaItems = mutableListOf<NexusMediaItem>()
        private set
    var initialPositions = mutableListOf<Long>()
        private set
    var playCount = 0
        private set
    var pauseCount = 0
        private set
    var seekPositions = mutableListOf<Long>()
        private set
    var playbackSpeeds = mutableListOf<Float>()
        private set
    var decoderModes = mutableListOf<DecoderMode>()
        private set
    var isReleased = false
        private set
    var attachedPlayerViews = mutableListOf<PlayerView>()
        private set
    var detachCallCount = 0
        private set
    var resizeModes = mutableListOf<Int>()
        private set
    var mutedHistory = mutableListOf<Boolean>()
        private set
    private var muted = false

    private var currentMediaItem: NexusMediaItem? = null
    private var simulatedDuration: Long = 0L

    // =========================================================================
    // NexusPlayer interface
    // =========================================================================

    override fun prepare(mediaItem: NexusMediaItem, initialPositionMs: Long) {
        if (isReleased) return
        preparedMediaItems.add(mediaItem)
        initialPositions.add(initialPositionMs)
        currentMediaItem = mediaItem

        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Loading,
                isPlaying = false,
                currentMediaId = mediaItem.mediaId,
                currentPosition = initialPositionMs.coerceAtLeast(0L),
                duration = 0L,
                bufferedPosition = 0L,
                error = null
            )
        }
    }

    override fun play() {
        if (isReleased) return
        playCount++
        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Ready,
                isPlaying = true
            )
        }
    }

    override fun pause() {
        if (isReleased) return
        pauseCount++
        _state.update { current ->
            current.copy(isPlaying = false)
        }
    }

    override fun seekTo(positionMs: Long) {
        if (isReleased) return
        val clamped = positionMs.coerceIn(0L, simulatedDuration.coerceAtLeast(0L))
        seekPositions.add(clamped)
        _state.update { current ->
            current.copy(currentPosition = clamped)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (isReleased) return
        if (speed <= 0f) return
        playbackSpeeds.add(speed)
        _state.update { current ->
            current.copy(playbackSpeed = speed)
        }
    }

    override fun setDecoderMode(mode: DecoderMode) {
        decoderModes.add(mode)
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        currentMediaItem = null
        _state.update { PlayerState.INITIAL }
    }

    override fun attachPlayerView(playerView: PlayerView) {
        if (isReleased) return
        attachedPlayerViews.add(playerView)
    }

    override fun detachPlayerView(playerView: PlayerView?) {
        detachCallCount++
    }

    override fun setVideoResizeMode(resizeMode: Int) {
        if (isReleased) return
        resizeModes.add(resizeMode)
        _state.update { it.copy(resizeMode = resizeMode) }
    }

    override fun setVideoScaleMode(scaleMode: com.nexus.player.core.playback.model.VideoScaleMode) {
        if (isReleased) return
        setVideoResizeMode(scaleMode.resizeMode)
        _state.update { it.copy(scaleMode = scaleMode) }
    }

    override fun setMuted(muted: Boolean) {
        if (isReleased) return
        this.muted = muted
        mutedHistory.add(muted)
        _state.update { it.copy(isMuted = muted) }
    }

    override fun selectAudioTrack(trackId: String) {
        if (isReleased) return
        _state.update { current ->
            current.copy(
                selectedAudioTrackId = trackId,
                audioTracks = current.audioTracks.map {
                    it.copy(isSelected = it.id == trackId)
                }
            )
        }
    }

    override fun selectSubtitleTrack(trackId: String?) {
        if (isReleased) return
        _state.update { current ->
            current.copy(
                selectedSubtitleTrackId = trackId,
                areSubtitlesEnabled = trackId != null,
                subtitleTracks = current.subtitleTracks.map {
                    it.copy(isSelected = it.id == trackId)
                }
            )
        }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        if (isReleased) return
        _state.update { it.copy(areSubtitlesEnabled = enabled) }
    }

    override fun setAudioDelayMs(delayMs: Long) {
        if (isReleased) return
        _state.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setSubtitleDelayMs(delayMs: Long) {
        if (isReleased) return
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun addExternalSubtitle(subtitle: com.nexus.player.core.playback.model.ExternalSubtitle) {
        if (isReleased) return
        val currentSubs = _state.value.externalSubtitles.toMutableList()
        if (currentSubs.none { it.uri == subtitle.uri }) {
            currentSubs.add(subtitle)
        }
        val subTrack = com.nexus.player.core.playback.model.PlayerTrack(
            id = subtitle.id,
            label = subtitle.label,
            language = subtitle.language,
            mimeType = subtitle.mimeType,
            isExternal = true
        )
        _state.update { current ->
            current.copy(
                externalSubtitles = currentSubs,
                subtitleTracks = current.subtitleTracks + subTrack
            )
        }
    }

    override fun setSubtitleAppearance(appearance: com.nexus.player.core.playback.model.SubtitleAppearance) {
        if (isReleased) return
        _state.update { it.copy(subtitleAppearance = appearance) }
    }

    override fun getCurrentPlaybackPosition(): PlaybackPosition? {
        if (isReleased) return null
        val mediaId = currentMediaItem?.mediaId ?: return null
        val current = _state.value
        val duration = current.duration
        val percentage = if (duration > 0L) {
            (current.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        return PlaybackPosition(
            mediaId = mediaId,
            positionMs = current.currentPosition,
            durationMs = duration,
            completionPercentage = percentage,
            timestamp = System.currentTimeMillis()
        )
    }

    // =========================================================================
    // Test helpers — manually trigger state transitions
    // =========================================================================

    /**
     * Simulate media becoming ready with a known duration.
     */
    fun simulateReady(durationMs: Long = 120_000L, isSeekable: Boolean = true) {
        simulatedDuration = durationMs
        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Ready,
                duration = durationMs,
                isSeekable = isSeekable
            )
        }
    }

    /**
     * Simulate buffering state.
     */
    fun simulateBuffering(bufferedPosition: Long = 0L) {
        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Buffering,
                isPlaying = false,
                bufferedPosition = if (bufferedPosition > 0L) bufferedPosition else current.bufferedPosition
            )
        }
    }

    /**
     * Simulate position/buffer advance during playback.
     */
    fun simulatePositionUpdate(positionMs: Long, bufferedPositionMs: Long = positionMs) {
        _state.update { current ->
            current.copy(
                currentPosition = positionMs,
                bufferedPosition = bufferedPositionMs
            )
        }
    }

    /**
     * Simulate playback ending.
     */
    fun simulateEnded() {
        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Ended,
                isPlaying = false,
                currentPosition = current.duration
            )
        }
    }

    /**
     * Simulate a playback error.
     */
    fun simulateError(error: PlaybackError) {
        _state.update { current ->
            current.copy(
                playbackState = PlaybackStatus.Error,
                isPlaying = false,
                error = error
            )
        }
    }

    /**
     * Simulate video size change from Media3.
     */
    fun simulateVideoDimensions(width: Int, height: Int) {
        _state.update { current ->
            current.copy(videoWidth = width, videoHeight = height)
        }
    }

    /**
     * Simulate available audio and subtitle tracks.
     */
    fun simulateTracks(
        audioTracks: List<com.nexus.player.core.playback.model.PlayerTrack> = emptyList(),
        subtitleTracks: List<com.nexus.player.core.playback.model.PlayerTrack> = emptyList()
    ) {
        _state.update { current ->
            current.copy(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                selectedAudioTrackId = audioTracks.firstOrNull { it.isSelected }?.id,
                selectedSubtitleTrackId = subtitleTracks.firstOrNull { it.isSelected }?.id,
                areSubtitlesEnabled = subtitleTracks.any { it.isSelected }
            )
        }
    }

    var fakeAudioEffectsController: com.nexus.player.core.playback.audio.AudioEffectsController = FakeAudioEffectsController()
    override val audioEffectsController: com.nexus.player.core.playback.audio.AudioEffectsController get() = fakeAudioEffectsController

    var frameToCapture: android.graphics.Bitmap? = null
    override suspend fun captureFrame(): android.graphics.Bitmap? = frameToCapture
}

class FakeAudioEffectsController : com.nexus.player.core.playback.audio.AudioEffectsController {
    override var isBoostSupported: Boolean = true
    val boostFlow = kotlinx.coroutines.flow.MutableStateFlow(100)
    override val boostPercent: kotlinx.coroutines.flow.StateFlow<Int> = boostFlow

    override var isEqualizerSupported: Boolean = true
    val eqEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isEqualizerEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = eqEnabledFlow

    val presetFlow = kotlinx.coroutines.flow.MutableStateFlow("Flat")
    override val currentPreset: kotlinx.coroutines.flow.StateFlow<String> = presetFlow

    val levelsFlow = kotlinx.coroutines.flow.MutableStateFlow<Map<Int, Int>>((0 until 5).associateWith { 0 })
    override val bandLevels: kotlinx.coroutines.flow.StateFlow<Map<Int, Int>> = levelsFlow

    override val bandFrequencies: List<Int> = listOf(60, 230, 910, 3600, 14000)
    override val bandLevelRange: IntRange = -1500..1500

    var attachedAudioSessionId: Int = 0

    override fun attachAudioSession(audioSessionId: Int) {
        attachedAudioSessionId = audioSessionId
    }

    override fun detachAudioSession() {
        attachedAudioSessionId = 0
    }

    override fun setAudioBoost(percent: Int) {
        boostFlow.value = percent.coerceIn(100, 200)
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        eqEnabledFlow.value = enabled
    }

    override fun setEqualizerPreset(presetName: String) {
        presetFlow.value = presetName
        if (!presetName.equals("Custom", ignoreCase = true)) {
            val preset = com.nexus.player.core.playback.audio.EqualizerPreset.fromName(presetName)
            levelsFlow.value = preset.bandGains.mapIndexed { idx, gain -> idx to gain }.toMap()
        }
    }

    override fun setBandLevel(bandIndex: Int, levelmB: Int) {
        val current = levelsFlow.value.toMutableMap()
        current[bandIndex] = levelmB.coerceIn(bandLevelRange)
        levelsFlow.value = current
        presetFlow.value = "Custom"
    }

    override fun setBandLevels(levels: Map<Int, Int>) {
        val current = levelsFlow.value.toMutableMap()
        levels.forEach { (idx, gain) ->
            current[idx] = gain.coerceIn(bandLevelRange)
        }
        levelsFlow.value = current
        presetFlow.value = "Custom"
    }

    override fun release() {
        attachedAudioSessionId = 0
    }
}
