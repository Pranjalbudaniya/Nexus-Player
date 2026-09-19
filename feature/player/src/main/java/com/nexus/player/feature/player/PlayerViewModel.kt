package com.nexus.player.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.NexusMediaItem
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.model.PlayerState
import com.nexus.player.core.playback.model.SubtitleAppearance
import com.nexus.player.core.playback.model.VideoScaleMode
import com.nexus.player.core.playback.repository.SubtitleRepository
import com.nexus.player.feature.player.preferences.PlayerPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One-off player events emitted to the UI/navigation layer.
 */
sealed interface PlayerEvent {
    data class VideoCompleted(val videoId: String) : PlayerEvent
}

/**
 * ViewModel managing the active video playback session.
 *
 * Responsibilities:
 * - Load media by stable ID from [VideoRepository]
 * - Restore saved playback position (<95%) or restart from beginning (>=95%)
 * - Dispatch play/pause/seek/speed/resize/mute commands to [NexusPlayer]
 * - Persist user playback preferences via [PlayerPreferencesRepository]
 * - Throttled persistence of playback progress (every ~3s while playing)
 * - Immediate persistence on pause, seek, and lifecycle backgrounding
 * - Mark completed and remove from Continue Watching when progress reaches >=95%
 * - Coordinate overlay auto-hide with settings panel and timeline interaction
 * - Handle error recovery and retries
 * - Cleanly release player resources without Activity/Context leakage
 */
@HiltViewModel
class PlayerViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    val player: NexusPlayer,
    private val playerPreferencesRepository: PlayerPreferencesRepository,
    private val subtitleRepository: SubtitleRepository,
    private val ioDispatcher: CoroutineDispatcher,
    internal var timeProvider: () -> Long
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        videoRepository: VideoRepository,
        player: NexusPlayer,
        playerPreferencesRepository: PlayerPreferencesRepository,
        subtitleRepository: SubtitleRepository,
        @Dispatcher(NexusDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ) : this(
        savedStateHandle = savedStateHandle,
        videoRepository = videoRepository,
        player = player,
        playerPreferencesRepository = playerPreferencesRepository,
        subtitleRepository = subtitleRepository,
        ioDispatcher = ioDispatcher,
        timeProvider = { System.currentTimeMillis() }
    )

    companion object {
        const val PROGRESS_DEBOUNCE_MS = 3000L
        const val CONTROLS_AUTO_HIDE_MS = 3000L
        const val COMPLETION_THRESHOLD = 0.95f
    }

    private val videoIdFromNav: String? = savedStateHandle["videoId"]

    private val _video = MutableStateFlow<Video?>(null)
    val video: StateFlow<Video?> = _video.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    private val _customError = MutableStateFlow<PlayerUiState.Error?>(null)
    private val _isPanelOpen = MutableStateFlow(false)
    private val _isDraggingSlider = MutableStateFlow(false)
    private val _isOrientationLocked = MutableStateFlow(false)
    private val _isFullscreen = MutableStateFlow(true)
    private val _decoderMode = MutableStateFlow(DecoderMode.Hardware)
    val decoderMode: StateFlow<DecoderMode> = _decoderMode.asStateFlow()

    private var autoHideJob: Job? = null
    private var lastPersistWallTimeMs: Long = 0L
    private var lastPersistedPositionMs: Long = -1L
    private var hasMarkedCompletedForSession = false
    private var currentLoadedId: String? = null
    private var preBoostSpeed: Float = 1.0f

    val seekDurationSeconds: StateFlow<Int> = playerPreferencesRepository.seekDurationSeconds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 10
        )

    val isAutoNextEnabled: StateFlow<Boolean> = playerPreferencesRepository.isAutoNextEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val _playerEvents = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 8)
    val playerEvents: SharedFlow<PlayerEvent> = _playerEvents.asSharedFlow()

    private data class UiFlags(
        val controlsVisible: Boolean,
        val isPanelOpen: Boolean,
        val isOrientationLocked: Boolean,
        val isFullscreen: Boolean,
        val seekDurationSeconds: Int,
        val isAutoNextEnabled: Boolean
    )

    private val _playerSettingsFlags = combine(
        seekDurationSeconds,
        isAutoNextEnabled
    ) { seekDuration, autoNext ->
        seekDuration to autoNext
    }

    private val _uiFlags = combine(
        _controlsVisible,
        _isPanelOpen,
        _isOrientationLocked,
        _isFullscreen,
        _playerSettingsFlags
    ) { controlsVisible, isPanelOpen, isOrientationLocked, isFullscreen, settingsFlags ->
        UiFlags(
            controlsVisible = controlsVisible,
            isPanelOpen = isPanelOpen,
            isOrientationLocked = isOrientationLocked,
            isFullscreen = isFullscreen,
            seekDurationSeconds = settingsFlags.first,
            isAutoNextEnabled = settingsFlags.second
        )
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        player.state,
        _video,
        _customError,
        _uiFlags
    ) { playerState, video, customError, flags ->
        when {
            customError != null -> customError
            playerState.playbackState == PlaybackStatus.Error && playerState.error != null -> {
                val error = playerState.error!!
                val canRetry = error.category in setOf(
                    ErrorCategory.NetworkFailure,
                    ErrorCategory.DecoderInitFailure,
                    ErrorCategory.Unknown
                )
                PlayerUiState.Error(
                    category = error.category,
                    userMessage = error.userMessage,
                    technicalDetail = error.technicalDetail,
                    canRetry = canRetry
                )
            }
            playerState.playbackState == PlaybackStatus.Loading && video == null -> {
                PlayerUiState.Loading(videoTitle = null)
            }
            playerState.playbackState == PlaybackStatus.Loading && video != null -> {
                PlayerUiState.Loading(videoTitle = video.title)
            }
            else -> {
                val title = video?.title ?: playerState.currentMediaId ?: "Video"
                val resolvedDuration = if (playerState.duration > 0L) {
                    playerState.duration
                } else {
                    video?.durationMs ?: 0L
                }
                PlayerUiState.Ready(
                    videoId = video?.id ?: playerState.currentMediaId ?: "",
                    videoTitle = title,
                    isPlaying = playerState.isPlaying,
                    playbackStatus = playerState.playbackState,
                    currentPositionMs = playerState.currentPosition,
                    durationMs = resolvedDuration,
                    bufferedPositionMs = playerState.bufferedPosition,
                    isSeekable = playerState.isSeekable,
                    controlsVisible = flags.controlsVisible,
                    playbackSpeed = playerState.playbackSpeed,
                    isMuted = playerState.isMuted,
                    resizeMode = playerState.resizeMode,
                    scaleMode = playerState.scaleMode,
                    isOrientationLocked = flags.isOrientationLocked,
                    isFullscreen = flags.isFullscreen,
                    isPanelOpen = flags.isPanelOpen,
                    seekDurationSeconds = flags.seekDurationSeconds,
                    isAutoNextEnabled = flags.isAutoNextEnabled
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState.Loading()
    )

    init {
        videoIdFromNav?.let { loadMedia(it) }
        observePlaybackPositionForPersistence()
        observeTrackPreferences()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            playerPreferencesRepository.playbackSpeed.first().let { speed ->
                if (speed > 0f) player.setPlaybackSpeed(speed)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.resizeMode.first().let { mode ->
                player.setVideoResizeMode(mode)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.decoderMode.first().let { mode ->
                _decoderMode.value = mode
                player.setDecoderMode(mode)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.audioDelayMs.first().let { delay ->
                player.setAudioDelayMs(delay)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.subtitleDelayMs.first().let { delay ->
                player.setSubtitleDelayMs(delay)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.subtitleAppearance.first().let { appearance ->
                player.setSubtitleAppearance(appearance)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.areSubtitlesEnabled.first().let { enabled ->
                player.setSubtitlesEnabled(enabled)
            }
        }
    }

    private fun observeTrackPreferences() {
        viewModelScope.launch {
            combine(
                player.state.map { it.audioTracks }.distinctUntilChanged(),
                playerPreferencesRepository.preferredAudioLanguage
            ) { tracks, preferredLang ->
                if (tracks.isNotEmpty() && preferredLang != null) {
                    val matchingTrack = tracks.firstOrNull {
                        it.language.equals(preferredLang, ignoreCase = true) ||
                            it.resolvedLanguageName.equals(preferredLang, ignoreCase = true) ||
                            it.label.equals(preferredLang, ignoreCase = true)
                    }
                    if (matchingTrack != null && !matchingTrack.isSelected) {
                        player.selectAudioTrack(matchingTrack.id)
                    }
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                player.state.map { it.subtitleTracks }.distinctUntilChanged(),
                playerPreferencesRepository.preferredSubtitleLanguage,
                playerPreferencesRepository.areSubtitlesEnabled
            ) { tracks, preferredLang, enabled ->
                if (!enabled) {
                    player.setSubtitlesEnabled(false)
                } else if (tracks.isNotEmpty() && preferredLang != null) {
                    val matchingTrack = tracks.firstOrNull {
                        it.language.equals(preferredLang, ignoreCase = true) ||
                            it.resolvedLanguageName.equals(preferredLang, ignoreCase = true) ||
                            it.label.equals(preferredLang, ignoreCase = true)
                    }
                    if (matchingTrack != null && !matchingTrack.isSelected) {
                        player.selectSubtitleTrack(matchingTrack.id)
                    }
                }
            }.collect()
        }
    }

    fun loadMedia(id: String) {
        currentLoadedId = id
        _customError.value = null
        hasMarkedCompletedForSession = false
        lastPersistWallTimeMs = timeProvider()
        viewModelScope.launch {
            val savedExternalSubs = withContext(ioDispatcher) {
                playerPreferencesRepository.getExternalSubtitles(id).first()
            }

            val video = withContext(ioDispatcher) {
                videoRepository.getVideoById(id)
            }

            if (video != null) {
                _video.value = video
                // Resume position rule: resume if <95% completed and has valid position, else restart from 0
                val startPosition = if (video.playbackPercentage < COMPLETION_THRESHOLD && video.playbackPositionMs > 0L) {
                    video.playbackPositionMs
                } else {
                    0L
                }

                val mediaItem = NexusMediaItem(
                    mediaId = video.id,
                    uri = video.mediaUri,
                    title = video.title,
                    externalSubtitles = savedExternalSubs
                )
                player.prepare(mediaItem, startPosition)
                player.play()
                resetAutoHideTimer()
            } else {
                // Support direct media URIs passed to the player
                val isUri = id.startsWith("content://") || id.startsWith("file://") ||
                    id.startsWith("http://") || id.startsWith("https://")
                if (isUri) {
                    val fallbackTitle = id.substringAfterLast('/')
                    val mediaItem = NexusMediaItem(
                        mediaId = id,
                        uri = id,
                        title = fallbackTitle,
                        externalSubtitles = savedExternalSubs
                    )
                    player.prepare(mediaItem, 0L)
                    player.play()
                    resetAutoHideTimer()
                } else {
                    _customError.value = PlayerUiState.Error(
                        category = ErrorCategory.MissingFile,
                        userMessage = "Video file not found or inaccessible.",
                        technicalDetail = "Video with ID '$id' was not found in the media library.",
                        canRetry = false
                    )
                }
            }
        }
    }

    fun play() {
        player.play()
        resetAutoHideTimer()
    }

    fun pause() {
        player.pause()
        autoHideJob?.cancel()
        _controlsVisible.value = true
        persistCurrentProgressImmediately()
    }

    fun togglePlayPause() {
        if (player.state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = player.state.value.duration
        val maxDuration = if (duration > 0L) duration else Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, maxDuration)
        player.seekTo(clamped)
        persistCurrentProgressImmediately()
        resetAutoHideTimer()
    }

    fun setControlsVisible(visible: Boolean) {
        _controlsVisible.value = visible
        if (visible && player.state.value.isPlaying && !_isPanelOpen.value && !_isDraggingSlider.value) {
            resetAutoHideTimer()
        } else if (!visible) {
            autoHideJob?.cancel()
        }
    }

    fun toggleControls() {
        setControlsVisible(!_controlsVisible.value)
    }

    fun validateSpeed(speed: Float): Float {
        if (speed <= 0f || speed.isNaN() || speed.isInfinite()) return 1.0f
        return speed.coerceIn(0.25f, 3.0f)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = validateSpeed(speed)
        player.setPlaybackSpeed(clamped)
        viewModelScope.launch {
            playerPreferencesRepository.setPlaybackSpeed(clamped)
        }
    }

    fun setTemporarySpeedBoost(boost: Boolean): Float {
        return if (boost) {
            preBoostSpeed = player.state.value.playbackSpeed
            val boostSpeed = if (preBoostSpeed < 2.0f) 2.0f else (preBoostSpeed + 0.5f).coerceAtMost(3.0f)
            player.setPlaybackSpeed(boostSpeed)
            boostSpeed
        } else {
            player.setPlaybackSpeed(preBoostSpeed)
            preBoostSpeed
        }
    }

    fun setSeekDurationSeconds(duration: Int) {
        val clamped = duration.coerceIn(5, 60)
        viewModelScope.launch {
            playerPreferencesRepository.setSeekDurationSeconds(clamped)
        }
    }

    fun setAutoNextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playerPreferencesRepository.setAutoNextEnabled(enabled)
        }
    }

    fun seekRelative(deltaSeconds: Int) {
        val current = player.state.value.currentPosition
        val duration = player.state.value.duration
        val maxDuration = if (duration > 0L) duration else Long.MAX_VALUE
        val target = (current + deltaSeconds * 1000L).coerceIn(0L, maxDuration)
        seekTo(target)
    }

    fun seekRelativeDirection(direction: Int) {
        val stepSec = seekDurationSeconds.value
        seekRelative(direction * stepSec)
    }

    fun cycleVideoScaleMode(): VideoScaleMode {
        val nextMode = player.state.value.scaleMode.next()
        player.setVideoScaleMode(nextMode)
        return nextMode
    }

    fun setVideoScaleMode(mode: VideoScaleMode) {
        player.setVideoScaleMode(mode)
    }

    fun selectAudioTrack(trackId: String) {
        player.selectAudioTrack(trackId)
        val track = player.state.value.audioTracks.firstOrNull { it.id == trackId }
        val preferred = track?.language?.takeIf { it.isNotBlank() } ?: track?.label
        if (preferred != null) {
            viewModelScope.launch {
                playerPreferencesRepository.setPreferredAudioLanguage(preferred)
            }
        }
    }

    fun selectSubtitleTrack(trackId: String?) {
        player.selectSubtitleTrack(trackId)
        if (trackId != null) {
            val track = player.state.value.subtitleTracks.firstOrNull { it.id == trackId }
            val preferred = track?.language?.takeIf { it.isNotBlank() } ?: track?.label
            viewModelScope.launch {
                playerPreferencesRepository.setPreferredSubtitleLanguage(preferred)
                playerPreferencesRepository.setSubtitlesEnabled(true)
            }
        } else {
            viewModelScope.launch {
                playerPreferencesRepository.setSubtitlesEnabled(false)
            }
        }
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        player.setSubtitlesEnabled(enabled)
        viewModelScope.launch {
            playerPreferencesRepository.setSubtitlesEnabled(enabled)
        }
    }

    fun setAudioDelayMs(delayMs: Long) {
        player.setAudioDelayMs(delayMs)
        viewModelScope.launch {
            playerPreferencesRepository.setAudioDelayMs(delayMs)
        }
    }

    fun setSubtitleDelayMs(delayMs: Long) {
        player.setSubtitleDelayMs(delayMs)
        viewModelScope.launch {
            playerPreferencesRepository.setSubtitleDelayMs(delayMs)
        }
    }

    fun setSubtitleAppearance(appearance: SubtitleAppearance) {
        player.setSubtitleAppearance(appearance)
        viewModelScope.launch {
            playerPreferencesRepository.setSubtitleAppearance(appearance)
        }
    }

    fun importExternalSubtitle(uri: Uri) {
        val externalSub = subtitleRepository.createExternalSubtitle(uri) ?: return
        player.addExternalSubtitle(externalSub)
        currentLoadedId?.let { videoId ->
            viewModelScope.launch {
                playerPreferencesRepository.addExternalSubtitle(videoId, externalSub)
            }
        }
        player.selectSubtitleTrack(externalSub.id)
    }

    fun setOrientationLocked(locked: Boolean) {
        _isOrientationLocked.value = locked
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun setVideoResizeMode(mode: Int) {
        player.setVideoResizeMode(mode)
        viewModelScope.launch {
            playerPreferencesRepository.setResizeMode(mode)
        }
    }

    fun setDecoderMode(mode: DecoderMode) {
        _decoderMode.value = mode
        player.setDecoderMode(mode)
        viewModelScope.launch {
            playerPreferencesRepository.setDecoderMode(mode)
        }
    }

    fun toggleMute() {
        val newMuted = !player.state.value.isMuted
        player.setMuted(newMuted)
    }

    fun setPanelOpen(open: Boolean) {
        _isPanelOpen.value = open
        if (open) {
            _controlsVisible.value = true
            autoHideJob?.cancel()
        } else {
            if (player.state.value.isPlaying && !_isDraggingSlider.value) {
                resetAutoHideTimer()
            }
        }
    }

    fun setSliderDragging(isDragging: Boolean) {
        _isDraggingSlider.value = isDragging
        if (isDragging) {
            autoHideJob?.cancel()
        } else {
            if (player.state.value.isPlaying && !_isPanelOpen.value) {
                resetAutoHideTimer()
            }
        }
    }

    fun retry() {
        currentLoadedId?.let { loadMedia(it) }
    }

    fun onPauseLifecycle(isChangingConfigurations: Boolean) {
        if (!isChangingConfigurations) {
            player.pause()
            persistCurrentProgressImmediately()
        }
    }

    private fun resetAutoHideTimer() {
        if (_isPanelOpen.value || _isDraggingSlider.value) return
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            if (isActive && player.state.value.isPlaying && !_isPanelOpen.value && !_isDraggingSlider.value) {
                _controlsVisible.value = false
            }
        }
    }

    private fun observePlaybackPositionForPersistence() {
        viewModelScope.launch {
            player.state.collect { state ->
                val video = _video.value ?: return@collect
                val duration = if (state.duration > 0L) state.duration else video.durationMs
                if (duration <= 0L) return@collect

                val position = state.currentPosition
                val percentage = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                val now = timeProvider()

                // Rule: >=95% completion immediately marks completed and removes from Continue Watching
                if ((percentage >= COMPLETION_THRESHOLD || state.playbackState == PlaybackStatus.Ended) && !hasMarkedCompletedForSession) {
                    hasMarkedCompletedForSession = true
                    lastPersistWallTimeMs = now
                    lastPersistedPositionMs = position
                    withContext(ioDispatcher) {
                        videoRepository.updatePlaybackProgress(
                            id = video.id,
                            positionMs = position,
                            percentage = percentage,
                            lastPlayedAt = now
                        )
                    }
                    _playerEvents.tryEmit(PlayerEvent.VideoCompleted(video.id))
                } else if (state.isPlaying && (now - lastPersistWallTimeMs >= PROGRESS_DEBOUNCE_MS)) {
                    // Throttled persistence during active playback
                    if (position != lastPersistedPositionMs) {
                        lastPersistWallTimeMs = now
                        lastPersistedPositionMs = position
                        withContext(ioDispatcher) {
                            videoRepository.updatePlaybackProgress(
                                id = video.id,
                                positionMs = position,
                                percentage = percentage,
                                lastPlayedAt = now
                            )
                        }
                    }
                }
            }
        }
    }

    fun persistCurrentProgressImmediately() {
        val video = _video.value ?: return
        val state = player.state.value
        val duration = if (state.duration > 0L) state.duration else video.durationMs
        val position = state.currentPosition
        val percentage = if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val now = timeProvider()
        lastPersistWallTimeMs = now
        lastPersistedPositionMs = position

        viewModelScope.launch(ioDispatcher) {
            videoRepository.updatePlaybackProgress(
                id = video.id,
                positionMs = position,
                percentage = percentage,
                lastPlayedAt = now
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistCurrentProgressImmediately()
        player.detachPlayerView()
        player.release()
    }
}
