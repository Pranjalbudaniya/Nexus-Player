package com.nexus.player.core.playback.internal

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.net.Uri
import android.util.TypedValue
import androidx.media3.common.C
import androidx.media3.ui.CaptionStyleCompat
import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.ExternalSubtitle
import com.nexus.player.core.playback.model.NexusMediaItem
import com.nexus.player.core.playback.model.PlaybackPosition
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.model.PlayerState
import com.nexus.player.core.playback.model.SubtitleAppearance
import com.nexus.player.core.playback.model.SubtitleBackgroundStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3/ExoPlayer implementation of [NexusPlayer].
 *
 * This class is `internal` — consumers never see it directly.
 * All interaction goes through the [NexusPlayer] interface.
 *
 * ## Position Update Strategy
 *
 * Rather than emitting state on every frame, a coroutine ticker
 * updates position at [POSITION_UPDATE_INTERVAL_MS] intervals while
 * playback is active. This prevents excessive Compose recomposition.
 *
 * ## Decoder Configuration
 *
 * The [ExoPlayer.Builder] uses the default [RenderersFactory][androidx.media3.exoplayer.RenderersFactory]
 * which prefers hardware-accelerated decoders via [android.media.MediaCodec].
 * The decoder mode configuration is isolated to [buildPlayer] so a future
 * FFmpeg-based [RenderersFactory] can be plugged in without restructuring.
 */
@Singleton
internal class Media3PlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NexusPlayer {

    companion object {
        private const val TAG = "Media3PlayerImpl"

        /** Position update interval in milliseconds. */
        private const val POSITION_UPDATE_INTERVAL_MS = 250L
    }

    private val _state = MutableStateFlow(PlayerState.INITIAL)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionTickerJob: Job? = null
    private var released = false
    private var currentDecoderMode: DecoderMode = DecoderMode.Hardware
    private var attachedPlayerView: PlayerView? = null
    private var currentResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var isMuted: Boolean = false
    private var currentNexusMediaItem: NexusMediaItem? = null
    private val currentExternalSubtitles = mutableListOf<ExternalSubtitle>()

    /**
     * The ExoPlayer instance. Created lazily on first [prepare] call
     * to avoid allocating resources before any media is loaded.
     */
    private var exoPlayer: ExoPlayer? = null

    /**
     * Listener forwarding ExoPlayer events to our [PlayerState] flow.
     */
    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateStateFromPlayer()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateStateFromPlayer()
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateStateFromPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            val playbackError = PlayerErrorMapper.mapException(error)
            Log.e(TAG, "Playback error: ${playbackError.technicalDetail}", error)
            _state.update { current ->
                current.copy(
                    playbackState = PlaybackStatus.Error,
                    isPlaying = false,
                    error = playbackError
                )
            }
            stopPositionTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.update { current ->
                current.copy(currentMediaId = mediaItem?.mediaId)
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            updateStateFromPlayer()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val rotated = videoSize.unappliedRotationDegrees == 90 || videoSize.unappliedRotationDegrees == 270
            val width = if (rotated) videoSize.height else videoSize.width
            val height = if (rotated) videoSize.width else videoSize.height
            if (width > 0 && height > 0) {
                _state.update { current ->
                    current.copy(videoWidth = width, videoHeight = height)
                }
            }
        }

        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            val audioList = mutableListOf<com.nexus.player.core.playback.model.PlayerTrack>()
            val subtitleList = mutableListOf<com.nexus.player.core.playback.model.PlayerTrack>()
            var selectedAudioId: String? = null
            var selectedSubId: String? = null

            for (group in tracks.groups) {
                val groupIdx = tracks.groups.indexOf(group)
                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val id = format.id ?: "audio_${groupIdx}_$i"
                            val isSelected = group.isTrackSelected(i)
                            if (isSelected) selectedAudioId = id

                            val label = format.label ?: ""
                            val language = format.language
                            val mimeType = format.sampleMimeType
                            val channelCount = format.channelCount
                            val codec = format.codecs ?: format.sampleMimeType?.substringAfterLast('/')?.uppercase(Locale.ROOT)

                            audioList.add(
                                com.nexus.player.core.playback.model.PlayerTrack(
                                    id = id,
                                    label = label,
                                    language = language,
                                    isSelected = isSelected,
                                    mimeType = mimeType,
                                    channelCount = channelCount,
                                    codec = codec,
                                    isExternal = false
                                )
                            )
                        }
                    }
                    C.TRACK_TYPE_TEXT -> {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val id = format.id ?: "sub_${groupIdx}_$i"
                            val isSelected = group.isTrackSelected(i)
                            if (isSelected) selectedSubId = id

                            val label = format.label ?: ""
                            val language = format.language
                            val mimeType = format.sampleMimeType
                            val isExternal = currentExternalSubtitles.any { it.label == label || it.uri.endsWith(label) }

                            subtitleList.add(
                                com.nexus.player.core.playback.model.PlayerTrack(
                                    id = id,
                                    label = label,
                                    language = language,
                                    isSelected = isSelected,
                                    mimeType = mimeType,
                                    isExternal = isExternal
                                )
                            )
                        }
                    }
                }
            }

            _state.update { current ->
                current.copy(
                    audioTracks = audioList,
                    subtitleTracks = subtitleList,
                    selectedAudioTrackId = selectedAudioId,
                    selectedSubtitleTrackId = selectedSubId,
                    externalSubtitles = currentExternalSubtitles.toList()
                )
            }
        }
    }

    // =========================================================================
    // NexusPlayer interface implementation
    // =========================================================================

    @OptIn(UnstableApi::class)
    override fun prepare(mediaItem: NexusMediaItem, initialPositionMs: Long) {
        released = false
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        currentNexusMediaItem = mediaItem
        currentExternalSubtitles.clear()
        currentExternalSubtitles.addAll(mediaItem.externalSubtitles)

        // Reset error state and transition to Loading
        _state.update {
            PlayerState.INITIAL.copy(
                playbackState = PlaybackStatus.Loading,
                externalSubtitles = currentExternalSubtitles.toList()
            )
        }

        val player = exoPlayer ?: buildPlayer().also {
            exoPlayer = it
            attachedPlayerView?.player = it
        }

        // Stop any existing playback
        player.stop()

        // Build external subtitle configurations if any
        val subtitleConfigs = currentExternalSubtitles.map { ext ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(ext.uri))
                .setMimeType(ext.mimeType)
                .setLanguage(ext.language)
                .setLabel(ext.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }

        // Build the Media3 MediaItem from our abstraction
        val media3Item = MediaItem.Builder()
            .setMediaId(mediaItem.mediaId)
            .setUri(mediaItem.uri)
            .apply {
                mediaItem.mimeType?.let { setMimeType(it) }
                if (subtitleConfigs.isNotEmpty()) {
                    setSubtitleConfigurations(subtitleConfigs)
                }
            }
            .build()

        val safeStartPositionMs = initialPositionMs.coerceAtLeast(0L)
        if (safeStartPositionMs > 0L) {
            player.setMediaItem(media3Item, safeStartPositionMs)
        } else {
            player.setMediaItem(media3Item)
        }
        player.prepare()
    }

    override fun play() {
        if (released) return
        val player = exoPlayer ?: return
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.play()
    }

    override fun pause() {
        if (released) return
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (released) return
        val player = exoPlayer ?: return
        val clampedPosition = if (player.duration > 0L) {
            positionMs.coerceIn(0L, player.duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        player.seekTo(clampedPosition)
        // Immediately update state for responsive UI
        _state.update { current ->
            current.copy(currentPosition = clampedPosition)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (released) return
        if (speed <= 0f) {
            Log.w(TAG, "setPlaybackSpeed() called with invalid speed $speed — ignoring.")
            return
        }
        exoPlayer?.setPlaybackSpeed(speed)
        _state.update { current ->
            current.copy(playbackSpeed = speed)
        }
    }

    override fun setDecoderMode(mode: DecoderMode) {
        currentDecoderMode = mode
        when (mode) {
            DecoderMode.Hardware -> {
                Log.d(TAG, "Decoder mode set to Hardware (default platform path).")
            }
            DecoderMode.Software -> {
                Log.w(
                    TAG,
                    "Decoder mode set to Software — not yet functional. " +
                        "Requires media3-decoder-ffmpeg or equivalent native library. " +
                        "Falling back to default hardware path."
                )
            }
            DecoderMode.Auto -> {
                Log.d(TAG, "Decoder mode set to Auto (currently identical to Hardware).")
            }
        }
        // NOTE: To apply a different RenderersFactory, the player would need to be
        // recreated. This is deferred to the future FFmpeg integration step where
        // buildPlayer() will branch on currentDecoderMode to supply the appropriate
        // RenderersFactory.
    }

    @OptIn(UnstableApi::class)
    override fun attachPlayerView(playerView: PlayerView) {
        attachedPlayerView = playerView
        playerView.useController = false
        playerView.resizeMode = currentResizeMode
        applySubtitleAppearanceToView(playerView, _state.value.subtitleAppearance)
        playerView.player = exoPlayer
    }

    override fun detachPlayerView(playerView: PlayerView?) {
        if (playerView == null || attachedPlayerView === playerView) {
            attachedPlayerView?.player = null
            attachedPlayerView = null
        }
    }

    @OptIn(UnstableApi::class)
    override fun setVideoResizeMode(resizeMode: Int) {
        currentResizeMode = resizeMode
        attachedPlayerView?.resizeMode = resizeMode
        _state.update { it.copy(resizeMode = resizeMode) }
    }

    override fun setVideoScaleMode(scaleMode: com.nexus.player.core.playback.model.VideoScaleMode) {
        setVideoResizeMode(scaleMode.resizeMode)
        _state.update { it.copy(scaleMode = scaleMode) }
    }

    override fun setMuted(muted: Boolean) {
        isMuted = muted
        exoPlayer?.volume = if (muted) 0f else 1f
        _state.update { it.copy(isMuted = muted) }
    }

    override fun selectAudioTrack(trackId: String) {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                val groupIdx = tracks.groups.indexOf(group)
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val id = format.id ?: "audio_${groupIdx}_$i"
                    if (id == trackId) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                androidx.media3.common.TrackSelectionOverride(
                                    group.mediaTrackGroup,
                                    listOf(i)
                                )
                            )
                            .build()
                        _state.update { it.copy(selectedAudioTrackId = trackId) }
                        return
                    }
                }
            }
        }
    }

    override fun selectSubtitleTrack(trackId: String?) {
        val player = exoPlayer ?: return
        if (trackId == null) {
            setSubtitlesEnabled(false)
            return
        }
        val tracks = player.currentTracks
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                val groupIdx = tracks.groups.indexOf(group)
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val id = format.id ?: "sub_${groupIdx}_$i"
                    if (id == trackId) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(
                                androidx.media3.common.TrackSelectionOverride(
                                    group.mediaTrackGroup,
                                    listOf(i)
                                )
                            )
                            .build()
                        _state.update { it.copy(selectedSubtitleTrackId = trackId, areSubtitlesEnabled = true) }
                        return
                    }
                }
            }
        }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        val player = exoPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        _state.update { it.copy(areSubtitlesEnabled = enabled) }
    }

    override fun setAudioDelayMs(delayMs: Long) {
        _state.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setSubtitleDelayMs(delayMs: Long) {
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    @OptIn(UnstableApi::class)
    override fun addExternalSubtitle(subtitle: ExternalSubtitle) {
        if (currentExternalSubtitles.none { it.uri == subtitle.uri }) {
            currentExternalSubtitles.add(subtitle)
        }
        _state.update { it.copy(externalSubtitles = currentExternalSubtitles.toList()) }

        val player = exoPlayer ?: return
        val currentMedia = currentNexusMediaItem ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val subtitleConfigs = currentExternalSubtitles.map { ext ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(ext.uri))
                .setMimeType(ext.mimeType)
                .setLanguage(ext.language)
                .setLabel(ext.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }

        val updatedItem = MediaItem.Builder()
            .setMediaId(currentMedia.mediaId)
            .setUri(currentMedia.uri)
            .apply {
                currentMedia.mimeType?.let { setMimeType(it) }
                if (subtitleConfigs.isNotEmpty()) {
                    setSubtitleConfigurations(subtitleConfigs)
                }
            }
            .build()

        player.setMediaItem(updatedItem, currentPos)
        player.prepare()
        if (wasPlaying) {
            player.play()
        }
    }

    @OptIn(UnstableApi::class)
    override fun setSubtitleAppearance(appearance: SubtitleAppearance) {
        _state.update { it.copy(subtitleAppearance = appearance) }
        applySubtitleAppearanceToView(attachedPlayerView, appearance)
    }

    @OptIn(UnstableApi::class)
    private fun applySubtitleAppearanceToView(view: PlayerView?, appearance: SubtitleAppearance) {
        view ?: return
        val subtitleView = view.subtitleView ?: return

        val edgeType = when (appearance.backgroundStyle) {
            SubtitleBackgroundStyle.None -> CaptionStyleCompat.EDGE_TYPE_NONE
            SubtitleBackgroundStyle.DropShadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            SubtitleBackgroundStyle.Outline -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            SubtitleBackgroundStyle.Box -> CaptionStyleCompat.EDGE_TYPE_NONE
        }

        val backgroundColor = when (appearance.backgroundStyle) {
            SubtitleBackgroundStyle.Box -> 0xB3000000.toInt()
            else -> 0x00000000
        }

        val captionStyle = CaptionStyleCompat(
            /* foregroundColor = */ appearance.textColor.argbColor,
            /* backgroundColor = */ backgroundColor,
            /* windowColor = */ 0x00000000,
            /* edgeType = */ edgeType,
            /* edgeColor = */ 0xFF000000.toInt(),
            /* typeface = */ null
        )

        subtitleView.setStyle(captionStyle)
        subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, appearance.textSize.sizeSp)
        subtitleView.setBottomPaddingFraction(appearance.position.bottomPaddingFraction)
    }

    override fun release() {
        if (released) return
        released = true

        stopPositionTicker()
        attachedPlayerView?.player = null
        attachedPlayerView = null
        exoPlayer?.let { player ->
            player.removeListener(playerListener)
            player.release()
        }
        exoPlayer = null
        _state.update { PlayerState.INITIAL }
        scope.cancel()

        Log.d(TAG, "Player released.")
    }

    override fun getCurrentPlaybackPosition(): PlaybackPosition? {
        if (released) return null
        val player = exoPlayer ?: return null
        val mediaId = player.currentMediaItem?.mediaId ?: return null
        val position = player.currentPosition
        val duration = player.duration.coerceAtLeast(0L)
        val percentage = if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return PlaybackPosition(
            mediaId = mediaId,
            positionMs = position,
            durationMs = duration,
            completionPercentage = percentage,
            timestamp = System.currentTimeMillis()
        )
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Creates an [ExoPlayer] instance with appropriate data source and renderer config.
     *
     * The [DefaultDataSource.Factory] supports:
     * - `content://` URIs (MediaStore, SAF)
     * - `file://` URIs and raw file paths
     * - `http://` and `https://` URLs
     *
     * ## Decoder Architecture
     *
     * Currently uses the default [RenderersFactory] (hardware-accelerated).
     * To add software decoding, replace with a custom RenderersFactory that
     * includes an FFmpeg-based video renderer. The branching point is here.
     */
    @OptIn(UnstableApi::class)
    private fun buildPlayer(): ExoPlayer {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Future: Branch on currentDecoderMode to supply a custom RenderersFactory
        // when DecoderMode.Software or DecoderMode.Auto with FFmpeg is implemented.
        //
        // val renderersFactory = when (currentDecoderMode) {
        //     DecoderMode.Software -> FfmpegRenderersFactory(context)
        //     DecoderMode.Auto -> FallbackRenderersFactory(context)
        //     DecoderMode.Hardware -> DefaultRenderersFactory(context)
        // }

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            // .setRenderersFactory(renderersFactory) // Future: custom renderers
            .build()
            .also { player ->
                player.addListener(playerListener)
                player.playWhenReady = false
            }
    }

    /**
     * Reads current ExoPlayer state and maps it to our [PlayerState].
     */
    private fun updateStateFromPlayer() {
        val player = exoPlayer ?: return
        if (released) return

        val playbackStatus = mapPlaybackState(player.playbackState, player.isPlaying)

        _state.update { current ->
            current.copy(
                playbackState = playbackStatus,
                isPlaying = player.isPlaying,
                currentPosition = player.currentPosition,
                duration = player.duration.coerceAtLeast(0L),
                bufferedPosition = player.bufferedPosition,
                playbackSpeed = player.playbackParameters.speed,
                isSeekable = player.isCurrentMediaItemSeekable,
                currentMediaId = player.currentMediaItem?.mediaId,
                // Clear error when not in error state
                error = if (playbackStatus == PlaybackStatus.Error) current.error else null
            )
        }
    }

    /**
     * Maps ExoPlayer's `Player.STATE_*` constants plus `isPlaying` to our [PlaybackStatus].
     */
    private fun mapPlaybackState(exoState: Int, isPlaying: Boolean): PlaybackStatus {
        return when (exoState) {
            Player.STATE_IDLE -> PlaybackStatus.Idle
            Player.STATE_BUFFERING -> {
                // Distinguish initial loading from mid-playback buffering
                if (_state.value.playbackState == PlaybackStatus.Idle ||
                    _state.value.playbackState == PlaybackStatus.Loading
                ) {
                    PlaybackStatus.Loading
                } else {
                    PlaybackStatus.Buffering
                }
            }
            Player.STATE_READY -> PlaybackStatus.Ready
            Player.STATE_ENDED -> PlaybackStatus.Ended
            else -> PlaybackStatus.Idle
        }
    }

    /**
     * Starts a coroutine ticker that updates position at [POSITION_UPDATE_INTERVAL_MS].
     * Only runs while playback is active.
     */
    private fun startPositionTicker() {
        // Don't start duplicate tickers
        if (positionTickerJob?.isActive == true) return

        positionTickerJob = scope.launch {
            while (isActive) {
                delay(POSITION_UPDATE_INTERVAL_MS)
                val player = exoPlayer ?: break
                if (released || !player.isPlaying) break

                _state.update { current ->
                    current.copy(
                        currentPosition = player.currentPosition,
                        duration = player.duration.coerceAtLeast(0L),
                        bufferedPosition = player.bufferedPosition
                    )
                }
            }
        }
    }

    /**
     * Stops the position update ticker.
     */
    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }
}
