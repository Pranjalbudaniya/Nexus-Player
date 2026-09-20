package com.nexus.player.feature.player

import android.content.Context
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
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.PlaybackQueueState
import com.nexus.player.core.playback.queue.PreviousResult
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.core.playback.queue.RepeatMode
import com.nexus.player.core.playback.repository.SubtitleRepository
import com.nexus.player.feature.player.preferences.PlayerPreferencesRepository
import com.nexus.player.feature.player.screenshot.ScreenshotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    data class ScreenshotSaved(val uri: Uri) : PlayerEvent
    data class ScreenshotFailed(val error: String) : PlayerEvent
    data class SleepTimerCompleted(val videoId: String) : PlayerEvent
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
    @ApplicationContext private val appContext: Context? = null,
    val playbackQueueManager: PlaybackQueueManager = PlaybackQueueManagerImpl(),
    internal var timeProvider: () -> Long
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        videoRepository: VideoRepository,
        player: NexusPlayer,
        playerPreferencesRepository: PlayerPreferencesRepository,
        subtitleRepository: SubtitleRepository,
        playbackQueueManager: PlaybackQueueManager,
        @Dispatcher(NexusDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationContext appContext: Context
    ) : this(
        savedStateHandle = savedStateHandle,
        videoRepository = videoRepository,
        player = player,
        playerPreferencesRepository = playerPreferencesRepository,
        subtitleRepository = subtitleRepository,
        ioDispatcher = ioDispatcher,
        appContext = appContext,
        playbackQueueManager = playbackQueueManager,
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
    private var hasHandledEndedForSession = false
    private var consecutiveSkipCount = 0
    private var currentLoadedId: String? = null
    private var preBoostSpeed: Float = 1.0f

    val queueState: StateFlow<PlaybackQueueState> = playbackQueueManager.queueState

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

    val audioBoostPercent: StateFlow<Int> = playerPreferencesRepository.audioBoostPercent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 100
        )

    val isEqualizerEnabled: StateFlow<Boolean> = playerPreferencesRepository.isEqualizerEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val equalizerPreset: StateFlow<String> = playerPreferencesRepository.equalizerPreset
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "Flat"
        )

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _volumePercent = MutableStateFlow(100)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _brightnessPercent = MutableStateFlow(50)
    val brightnessPercent: StateFlow<Int> = _brightnessPercent.asStateFlow()

    private val _zoom = MutableStateFlow(1.0f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    private val _panOffsetX = MutableStateFlow(0f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(0f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    private val _playerEvents = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 8)
    val playerEvents: SharedFlow<PlayerEvent> = _playerEvents.asSharedFlow()

    private data class UiFlags(
        val controlsVisible: Boolean,
        val isPanelOpen: Boolean,
        val isOrientationLocked: Boolean,
        val isFullscreen: Boolean,
        val seekDurationSeconds: Int,
        val isAutoNextEnabled: Boolean,
        val audioBoostPercent: Int,
        val isEqualizerEnabled: Boolean,
        val equalizerPreset: String,
        val sleepTimerRemainingSeconds: Long?,
        val volumePercent: Int,
        val brightnessPercent: Int,
        val zoom: Float,
        val panOffsetX: Float,
        val panOffsetY: Float
    )

    private data class SettingsAndUtilitiesFlags(
        val seekDuration: Int,
        val autoNext: Boolean,
        val audioBoost: Int,
        val eqEnabled: Boolean,
        val eqPreset: String,
        val sleepTimer: Long?,
        val volume: Int,
        val brightness: Int,
        val zoom: Float,
        val panOffsetX: Float,
        val panOffsetY: Float
    )

    private val _settingsAndUtilitiesFlags = combine(
        combine(seekDurationSeconds, isAutoNextEnabled, audioBoostPercent) { seek, autoNext, boost ->
            Triple(seek, autoNext, boost)
        },
        combine(isEqualizerEnabled, equalizerPreset, _sleepTimerRemainingSeconds) { eqEnabled, eqPreset, timer ->
            Triple(eqEnabled, eqPreset, timer)
        },
        combine(_volumePercent, _brightnessPercent) { vol, bright ->
            vol to bright
        },
        combine(_zoom, _panOffsetX, _panOffsetY) { z, px, py ->
            Triple(z, px, py)
        }
    ) { (seek, autoNext, boost), (eqEnabled, eqPreset, timer), (vol, bright), (z, px, py) ->
        SettingsAndUtilitiesFlags(
            seekDuration = seek,
            autoNext = autoNext,
            audioBoost = boost,
            eqEnabled = eqEnabled,
            eqPreset = eqPreset,
            sleepTimer = timer,
            volume = vol,
            brightness = bright,
            zoom = z,
            panOffsetX = px,
            panOffsetY = py
        )
    }

    private val _uiFlags = combine(
        _controlsVisible,
        _isPanelOpen,
        _isOrientationLocked,
        _isFullscreen,
        _settingsAndUtilitiesFlags
    ) { controlsVisible, isPanelOpen, isOrientationLocked, isFullscreen, extra ->
        UiFlags(
            controlsVisible = controlsVisible,
            isPanelOpen = isPanelOpen,
            isOrientationLocked = isOrientationLocked,
            isFullscreen = isFullscreen,
            seekDurationSeconds = extra.seekDuration,
            isAutoNextEnabled = extra.autoNext,
            audioBoostPercent = extra.audioBoost,
            isEqualizerEnabled = extra.eqEnabled,
            equalizerPreset = extra.eqPreset,
            sleepTimerRemainingSeconds = extra.sleepTimer,
            volumePercent = extra.volume,
            brightnessPercent = extra.brightness,
            zoom = extra.zoom,
            panOffsetX = extra.panOffsetX,
            panOffsetY = extra.panOffsetY
        )
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        player.state,
        _video,
        _customError,
        _uiFlags,
        queueState
    ) { playerState, video, customError, flags, queue ->
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
                    isAutoNextEnabled = flags.isAutoNextEnabled,
                    audioBoostPercent = flags.audioBoostPercent,
                    isEqualizerEnabled = flags.isEqualizerEnabled,
                    equalizerPreset = flags.equalizerPreset,
                    sleepTimerRemainingSeconds = flags.sleepTimerRemainingSeconds,
                    volumePercent = flags.volumePercent,
                    brightnessPercent = flags.brightnessPercent,
                    zoom = flags.zoom,
                    panOffsetX = flags.panOffsetX,
                    panOffsetY = flags.panOffsetY,
                    hasPrevious = queue.hasPrevious,
                    hasNext = queue.hasNext,
                    repeatMode = queue.repeatMode,
                    isShuffleEnabled = queue.isShuffleEnabled,
                    queueSize = queue.size,
                    queueIndex = queue.currentIndex
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState.Loading()
    )

    init {
        videoIdFromNav?.let { id ->
            if (playbackQueueManager.queueState.value.isEmpty ||
                playbackQueueManager.queueState.value.currentVideoId != id
            ) {
                val found = playbackQueueManager.playItem(id)
                if (!found) {
                    playbackQueueManager.setQueue(listOf(id), id, QueueSource.Manual)
                }
            }
            loadMedia(id)
        }
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
            if (currentLoadedId == null) {
                playerPreferencesRepository.resizeMode.first().let { mode ->
                    player.setVideoResizeMode(mode)
                }
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
        viewModelScope.launch {
            playerPreferencesRepository.audioBoostPercent.first().let { boost ->
                player.audioEffectsController.setAudioBoost(boost)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.isEqualizerEnabled.first().let { enabled ->
                player.audioEffectsController.setEqualizerEnabled(enabled)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.equalizerPreset.first().let { preset ->
                player.audioEffectsController.setEqualizerPreset(preset)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.repeatMode.collect { mode ->
                playbackQueueManager.setRepeatMode(mode)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.isShuffleEnabled.collect { enabled ->
                playbackQueueManager.setShuffleEnabled(enabled)
            }
        }
        viewModelScope.launch {
            playerPreferencesRepository.isAutoNextEnabled.collect { enabled ->
                playbackQueueManager.setAutoNextEnabled(enabled)
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
        playbackQueueManager.playItem(id)
        _customError.value = null
        hasMarkedCompletedForSession = false
        hasHandledEndedForSession = false
        lastPersistWallTimeMs = timeProvider()
        resetZoom()
        viewModelScope.launch {
            val savedScaleMode = playerPreferencesRepository.getVideoScaleMode(id).first()
            player.setVideoScaleMode(savedScaleMode)

            val savedExternalSubs = withContext(ioDispatcher) {
                playerPreferencesRepository.getExternalSubtitles(id).first()
            }

            val video = withContext(ioDispatcher) {
                videoRepository.getVideoById(id)
            }

            if (video != null) {
                consecutiveSkipCount = 0
                _video.value = video
                // Resume position rule: resume if not marked completed, <95% completed, has valid position, and not near the end
                val isNearEnd = video.durationMs > 0L && (video.durationMs - video.playbackPositionMs) < 2000L
                val isAlreadyCompleted = video.isCompleted || video.playbackPercentage >= COMPLETION_THRESHOLD || isNearEnd
                val startPosition = if (!isAlreadyCompleted && video.playbackPositionMs > 0L && (video.durationMs <= 0L || video.playbackPositionMs < video.durationMs)) {
                    video.playbackPositionMs
                } else {
                    0L
                }

                if (isAlreadyCompleted) {
                    val restartTime = timeProvider()
                    withContext(ioDispatcher) {
                        videoRepository.restartPlayback(video.id, restartTime)
                    }
                    _video.value = video.copy(
                        isCompleted = false,
                        playbackPositionMs = 0L,
                        playbackPercentage = 0.0f,
                        lastPlayedAt = restartTime,
                        watchCount = video.watchCount + 1
                    )
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
                    consecutiveSkipCount = 0
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
                    val queue = playbackQueueManager.queueState.value
                    if (isAutoNextEnabled.value && queue.size > 1 && consecutiveSkipCount < queue.size) {
                        consecutiveSkipCount++
                        val nextId = playbackQueueManager.playNext()
                        if (nextId != null && nextId != id) {
                            loadMedia(nextId)
                            return@launch
                        }
                    }
                    consecutiveSkipCount = 0
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
        playbackQueueManager.setAutoNextEnabled(enabled)
        viewModelScope.launch {
            playerPreferencesRepository.setAutoNextEnabled(enabled)
        }
    }

    fun onPreviousClick() {
        val currentPosition = player.state.value.currentPosition
        when (val result = playbackQueueManager.playPrevious(currentPosition)) {
            is PreviousResult.RestartCurrent -> {
                seekTo(0L)
                play()
            }
            is PreviousResult.PlayVideo -> {
                loadMedia(result.videoId)
            }
            is PreviousResult.None -> Unit
        }
    }

    fun onNextClick() {
        val nextId = playbackQueueManager.playNext()
        if (nextId != null) {
            loadMedia(nextId)
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        val next = playbackQueueManager.cycleRepeatMode()
        viewModelScope.launch {
            playerPreferencesRepository.setRepeatMode(next)
        }
        return next
    }

    fun setRepeatMode(mode: RepeatMode) {
        playbackQueueManager.setRepeatMode(mode)
        viewModelScope.launch {
            playerPreferencesRepository.setRepeatMode(mode)
        }
    }

    fun toggleShuffle(): Boolean {
        val enabled = playbackQueueManager.toggleShuffle()
        viewModelScope.launch {
            playerPreferencesRepository.setShuffleEnabled(enabled)
        }
        return enabled
    }

    fun setShuffleEnabled(enabled: Boolean) {
        playbackQueueManager.setShuffleEnabled(enabled)
        viewModelScope.launch {
            playerPreferencesRepository.setShuffleEnabled(enabled)
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

    fun onZoomChange(zoomDelta: Float) {
        val current = _zoom.value
        val newZoom = (current * zoomDelta).coerceIn(1.0f, 4.0f)
        _zoom.value = newZoom
        if (newZoom <= 1.01f) {
            _panOffsetX.value = 0f
            _panOffsetY.value = 0f
        }
    }

    fun onPanChange(panDeltaX: Float, panDeltaY: Float, containerWidth: Float = 0f, containerHeight: Float = 0f) {
        val currentZoom = _zoom.value
        if (currentZoom > 1.01f) {
            val newX = _panOffsetX.value + panDeltaX
            val newY = _panOffsetY.value + panDeltaY
            if (containerWidth > 0f && containerHeight > 0f) {
                val maxPanX = (containerWidth * (currentZoom - 1f)) / 2f
                val maxPanY = (containerHeight * (currentZoom - 1f)) / 2f
                _panOffsetX.value = newX.coerceIn(-maxPanX, maxPanX)
                _panOffsetY.value = newY.coerceIn(-maxPanY, maxPanY)
            } else {
                _panOffsetX.value = newX
                _panOffsetY.value = newY
            }
        }
    }

    fun resetZoom() {
        _zoom.value = 1.0f
        _panOffsetX.value = 0f
        _panOffsetY.value = 0f
    }

    fun cycleVideoScaleMode(): VideoScaleMode {
        val nextMode = player.state.value.scaleMode.next()
        setVideoScaleMode(nextMode)
        return nextMode
    }

    fun setVideoScaleMode(mode: VideoScaleMode) {
        player.setVideoScaleMode(mode)
        val id = currentLoadedId
        if (!id.isNullOrBlank()) {
            viewModelScope.launch {
                playerPreferencesRepository.setVideoScaleMode(id, mode)
            }
        }
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

    fun setAudioBoost(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        player.audioEffectsController.setAudioBoost(clamped)
        viewModelScope.launch {
            playerPreferencesRepository.setAudioBoost(clamped)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        player.audioEffectsController.setEqualizerEnabled(enabled)
        viewModelScope.launch {
            playerPreferencesRepository.setEqualizerEnabled(enabled)
        }
    }

    fun setEqualizerPreset(preset: String) {
        player.audioEffectsController.setEqualizerPreset(preset)
        viewModelScope.launch {
            playerPreferencesRepository.setEqualizerPreset(preset)
        }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelmB: Int) {
        player.audioEffectsController.setBandLevel(bandIndex, levelmB)
    }

    fun setVolumePercent(percent: Int) {
        _volumePercent.value = percent.coerceIn(0, 100)
    }

    fun setBrightnessPercent(percent: Int) {
        _brightnessPercent.value = percent.coerceIn(0, 100)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive && remaining == 0L) {
                player.pause()
                persistCurrentProgressImmediately()
                _sleepTimerRemainingSeconds.value = null
                val vid = _video.value?.id ?: currentLoadedId ?: ""
                _playerEvents.emit(PlayerEvent.SleepTimerCompleted(vid))
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
    }

    fun takeScreenshot(context: Context? = null) {
        val targetContext = context ?: appContext
        viewModelScope.launch {
            if (targetContext == null) {
                _playerEvents.emit(PlayerEvent.ScreenshotFailed("Application context not available"))
                return@launch
            }
            val title = _video.value?.title ?: "Video"
            val bitmap = player.captureFrame()
            if (bitmap == null) {
                _playerEvents.emit(PlayerEvent.ScreenshotFailed("Failed to capture video frame"))
                return@launch
            }
            val result = ScreenshotManager.saveScreenshot(targetContext, bitmap, title)
            result.onSuccess { uri ->
                _playerEvents.emit(PlayerEvent.ScreenshotSaved(uri))
            }.onFailure { err ->
                _playerEvents.emit(PlayerEvent.ScreenshotFailed(err.message ?: "Screenshot error"))
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
                val isCompleted = percentage >= COMPLETION_THRESHOLD || state.playbackState == PlaybackStatus.Ended
                if (isCompleted && !hasMarkedCompletedForSession) {
                    hasMarkedCompletedForSession = true
                    lastPersistWallTimeMs = now
                    lastPersistedPositionMs = position
                    withContext(ioDispatcher) {
                        videoRepository.updatePlaybackProgress(
                            id = video.id,
                            positionMs = position,
                            percentage = percentage,
                            lastPlayedAt = now,
                            isCompleted = true
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
                                lastPlayedAt = now,
                                isCompleted = false
                            )
                        }
                    }
                }

                // Handle auto-next and repeat modes when playback ends
                if (state.playbackState == PlaybackStatus.Ended && !hasHandledEndedForSession) {
                    hasHandledEndedForSession = true
                    handlePlaybackEnded()
                }
            }
        }
    }

    private fun handlePlaybackEnded() {
        val queue = playbackQueueManager.queueState.value
        when (queue.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                seekTo(0L)
                play()
            }
            RepeatMode.REPEAT_ALL -> {
                val nextId = playbackQueueManager.playNext()
                if (nextId != null) {
                    if (nextId == currentLoadedId) {
                        seekTo(0L)
                        play()
                    } else {
                        loadMedia(nextId)
                    }
                } else {
                    seekTo(0L)
                    play()
                }
            }
            RepeatMode.OFF -> {
                if (isAutoNextEnabled.value) {
                    val nextId = playbackQueueManager.playNext()
                    if (nextId != null) {
                        loadMedia(nextId)
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
        val isCompleted = hasMarkedCompletedForSession || percentage >= COMPLETION_THRESHOLD || state.playbackState == PlaybackStatus.Ended
        val now = timeProvider()
        lastPersistWallTimeMs = now
        lastPersistedPositionMs = position

        viewModelScope.launch(ioDispatcher) {
            videoRepository.updatePlaybackProgress(
                id = video.id,
                positionMs = position,
                percentage = percentage,
                lastPlayedAt = now,
                isCompleted = isCompleted
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
