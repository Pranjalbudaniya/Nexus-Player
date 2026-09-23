package com.nexus.player.feature.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.component.AudioSubtitlesSheet
import com.nexus.player.feature.player.component.GestureHudState
import com.nexus.player.feature.player.component.PlayerControls
import com.nexus.player.feature.player.component.SubtitleCustomizationDrawer
import com.nexus.player.feature.player.component.PlayerErrorOverlay
import com.nexus.player.feature.player.component.PlayerGestureHud
import com.nexus.player.feature.player.component.PlayerGestureSurface
import com.nexus.player.feature.player.component.PlayerLoadingOverlay
import com.nexus.player.feature.player.component.PlayerVideoSurface
import com.nexus.player.feature.player.dialog.EqualizerDialog
import com.nexus.player.feature.player.dialog.SleepTimerDialog
import com.nexus.player.feature.player.orientation.rememberPlayerOrientationController
import com.nexus.player.feature.player.panel.PlayerPanelState
import com.nexus.player.feature.player.panel.rememberPlayerPanelState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * Root Composable for the Video Player destination.
 *
 * Implements:
 * - VLC-style direct player actions and controls
 * - Isolated gesture surface with volume, brightness, seek, and boost gestures
 * - Visual gesture HUD feedback overlays (Volume, Brightness, Seek, Speed Boost, Screenshot, Sleep Timer, Audio Boost)
 * - Screen brightness restoration on exit
 * - Real audio boost (100% - 200%) and Equalizer foundation dialog
 * - Screenshot capture frame saved to Pictures/NexusPlayer
 * - Sleep timer with countdown, auto-pause, and visual badge
 * - Combined Audio & Subtitles bottom sheet with sync delays
 * - One-tap video aspect ratio cycling (Fit, Fill, Crop, Stretch, Original)
 * - Orientation lock and fullscreen management with auto-rotation fix
 * - Flat More menu for advanced technical metadata and actions
 * - Pure black video letterboxing and responsive layout reflow
 */
@Composable
fun PlayerRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val video by viewModel.video.collectAsStateWithLifecycle()
    val decoderMode by viewModel.decoderMode.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

    val panelState = rememberPlayerPanelState()
    val orientationController = rememberPlayerOrientationController()

    val isLandscapeVideo by remember(viewModel.player) {
        viewModel.player.state
            .map { it.isLandscapeVideo }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val subtitleAppearance by remember(viewModel.player) {
        viewModel.player.state
            .map { it.subtitleAppearance }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = com.nexus.player.core.playback.model.SubtitleAppearance.DEFAULT)

    val seekDurationSeconds by viewModel.seekDurationSeconds.collectAsStateWithLifecycle()
    val isAutoNextEnabled by viewModel.isAutoNextEnabled.collectAsStateWithLifecycle()
    val isPressAndHoldSpeedEnabled by viewModel.isPressAndHoldSpeedEnabled.collectAsStateWithLifecycle(initialValue = true)

    var showAudioSubtitlesSheet by remember { mutableStateOf(false) }
    var showSubtitleCustomizationDrawer by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var hudState by remember { mutableStateOf<GestureHudState>(GestureHudState.None) }

    // Listen to one-off player events
    LaunchedEffect(Unit) {
        viewModel.playerEvents.collect { event ->
            when (event) {
                is PlayerEvent.VideoCompleted -> {
                    // Completion recorded; ready for auto-next if enabled
                }
                is PlayerEvent.ScreenshotSaved -> {
                    hudState = GestureHudState.Screenshot(success = true)
                }
                is PlayerEvent.ScreenshotFailed -> {
                    hudState = GestureHudState.Screenshot(success = false, message = event.error)
                }
                is PlayerEvent.SleepTimerCompleted -> {
                    hudState = GestureHudState.SleepTimer("Sleep timer finished")
                }
            }
        }
    }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember {
        mutableFloatStateOf(
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)).toFloat()
        )
    }

    var currentBrightness by remember {
        val current = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (current in 0f..1f) current else 0.5f)
    }

    val initialScreenBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }

    // Restore screen brightness on exit
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = initialScreenBrightness
                win.attributes = lp
            }
        }
    }

    val subtitlePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.importExternalSubtitle(uri)
        }
    }

    // Synchronize panel state with ViewModel auto-hide
    LaunchedEffect(panelState.isOpen) {
        viewModel.setPanelOpen(panelState.isOpen)
    }

    // Auto-dismiss HUD
    LaunchedEffect(hudState) {
        if (hudState !is GestureHudState.None && hudState !is GestureHudState.SpeedBoost) {
            val durationMs = when (hudState) {
                is GestureHudState.Screenshot,
                is GestureHudState.SleepTimer,
                is GestureHudState.AudioBoost,
                is GestureHudState.ScaleMode -> 1500L
                else -> 1000L
            }
            kotlinx.coroutines.delay(durationMs)
            hudState = GestureHudState.None
        }
    }

    // Enter fullscreen immediately when video starts and adapt orientation
    LaunchedEffect(isLandscapeVideo) {
        orientationController.enterFullscreen(isLandscapeVideo)
        viewModel.setFullscreen(true)
    }

    val handleBackClick: () -> Unit = {
        if (showSubtitleCustomizationDrawer) {
            showSubtitleCustomizationDrawer = false
        } else if (showSleepTimerDialog) {
            showSleepTimerDialog = false
        } else if (showEqualizerDialog) {
            showEqualizerDialog = false
        } else if (showAudioSubtitlesSheet) {
            showAudioSubtitlesSheet = false
        } else if (panelState.isOpen) {
            if (!panelState.navigateBack()) {
                panelState.close()
                viewModel.setPanelOpen(false)
            }
        } else {
            orientationController.resetToAppDefault()
            onBackClick()
        }
    }

    // Intercept back gestures and buttons with unified handler
    BackHandler {
        handleBackClick()
    }

    val window = activity?.window
    DisposableEffect(window) {
        val originalBackground = window?.decorView?.background
        window?.decorView?.setBackgroundColor(android.graphics.Color.BLACK)
        onDispose {
            window?.decorView?.background = originalBackground
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.onPauseLifecycle(activity?.isChangingConfigurations == true)
            } else if (event == Lifecycle.Event.ON_RESUME) {
                orientationController.enterFullscreen(isLandscapeVideo)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onToggleOrientationLock: () -> Unit = {
        orientationController.toggleOrientationLock()
        viewModel.setOrientationLocked(orientationController.isOrientationLocked)
    }

    val onBrightnessDelta: (Float) -> Unit = { delta ->
        val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
        currentBrightness = newBrightness
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = newBrightness
            win.attributes = lp
        }
        val percent = (newBrightness * 100).toInt()
        viewModel.setBrightnessPercent(percent)
        hudState = GestureHudState.Brightness(percent)
    }

    val onVolumeDelta: (Float) -> Unit = { delta ->
        if (delta > 0) {
            if (currentVolume < maxVolume.toFloat()) {
                val newVol = (currentVolume + (delta * maxVolume)).coerceAtMost(maxVolume.toFloat())
                currentVolume = newVol
                val volInt = newVol.roundToInt()
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volInt, 0)
                val percent = if (maxVolume > 0) ((newVol / maxVolume) * 100).toInt() else 0
                viewModel.setVolumePercent(percent)
                hudState = GestureHudState.Volume(percent)
            } else {
                val currentBoost = (uiState as? PlayerUiState.Ready)?.audioBoostPercent ?: 100
                val newBoost = (currentBoost + (delta * 100).roundToInt()).coerceIn(100, 200)
                viewModel.setAudioBoost(newBoost)
                viewModel.setVolumePercent(newBoost)
                hudState = GestureHudState.Volume(newBoost)
            }
        } else if (delta < 0) {
            val currentBoost = (uiState as? PlayerUiState.Ready)?.audioBoostPercent ?: 100
            if (currentBoost > 100) {
                val newBoost = (currentBoost + (delta * 100).roundToInt()).coerceIn(100, 200)
                viewModel.setAudioBoost(newBoost)
                viewModel.setVolumePercent(newBoost)
                hudState = GestureHudState.Volume(newBoost)
            } else {
                val newVol = (currentVolume + (delta * maxVolume)).coerceIn(0f, maxVolume.toFloat())
                currentVolume = newVol
                val volInt = newVol.roundToInt()
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volInt, 0)
                val percent = if (maxVolume > 0) ((newVol / maxVolume) * 100).toInt() else 0
                viewModel.setVolumePercent(percent)
                hudState = GestureHudState.Volume(percent)
            }
        }
    }

    val onSeekBackward: () -> Unit = {
        viewModel.seekRelativeDirection(-1)
        hudState = GestureHudState.Seek(seekDurationSeconds, isForward = false)
    }

    val onSeekForward: () -> Unit = {
        viewModel.seekRelativeDirection(1)
        hudState = GestureHudState.Seek(seekDurationSeconds, isForward = true)
    }

    val onSpeedBoost: (Boolean) -> Unit = { boosting ->
        if (isPressAndHoldSpeedEnabled || !boosting) {
            val boostSpeed = viewModel.setTemporarySpeedBoost(boosting)
            hudState = if (boosting) GestureHudState.SpeedBoost(boostSpeed) else GestureHudState.None
        }
    }

    val onAudioBoostSelected: (Int) -> Unit = { boostPercent ->
        viewModel.setAudioBoost(boostPercent)
        hudState = GestureHudState.AudioBoost(boostPercent)
    }

    val onSetSleepTimerMinutes: (Int) -> Unit = { minutes ->
        viewModel.startSleepTimer(minutes)
        hudState = GestureHudState.SleepTimer("Sleep timer $minutes min")
    }

    val onCancelSleepTimer: () -> Unit = {
        viewModel.cancelSleepTimer()
        hudState = GestureHudState.SleepTimer("Sleep timer off")
    }

    val onTakeScreenshot: () -> Unit = {
        viewModel.takeScreenshot()
    }

    val onShareClick: () -> Unit = {
        val uriToShare = video?.mediaUri ?: (uiState as? PlayerUiState.Ready)?.videoId
        if (uriToShare != null) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                if (uriToShare.startsWith("http://", ignoreCase = true) || uriToShare.startsWith("https://", ignoreCase = true)) {
                    putExtra(Intent.EXTRA_TEXT, uriToShare)
                    type = "text/plain"
                } else {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriToShare))
                    putExtra(Intent.EXTRA_TEXT, video?.title ?: "Video")
                    type = "video/*"
                }
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share video via")
            context.startActivity(shareIntent)
        }
    }

    PlayerScreen(
        uiState = uiState,
        player = viewModel.player,
        video = video,
        currentDecoderMode = decoderMode,
        panelState = panelState,
        hudState = hudState,
        showAudioSubtitlesSheet = showAudioSubtitlesSheet,
        showSubtitleCustomizationDrawer = showSubtitleCustomizationDrawer,
        showSleepTimerDialog = showSleepTimerDialog,
        showEqualizerDialog = showEqualizerDialog,
        isPressAndHoldSpeedEnabled = isPressAndHoldSpeedEnabled,
        onToggleControls = { viewModel.toggleControls() },
        onBackClick = handleBackClick,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        onPreviousClick = { viewModel.onPreviousClick() },
        onNextClick = { viewModel.onNextClick() },
        onCycleRepeatMode = { viewModel.cycleRepeatMode() },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onSeek = { positionMs -> viewModel.seekTo(positionMs) },
        onSeekBackward = onSeekBackward,
        onSeekForward = onSeekForward,
        onBrightnessDelta = onBrightnessDelta,
        onVolumeDelta = onVolumeDelta,
        onSpeedBoost = onSpeedBoost,
        onOpenAudioSubtitles = { showAudioSubtitlesSheet = true },
        onDismissAudioSubtitles = { showAudioSubtitlesSheet = false },
        onOpenSubtitleCustomization = {
            showAudioSubtitlesSheet = false
            showSubtitleCustomizationDrawer = true
        },
        onDismissSubtitleCustomization = { showSubtitleCustomizationDrawer = false },
        onSpeedSelected = { speed -> viewModel.setPlaybackSpeed(speed) },
        onSeekDurationSelected = { sec -> viewModel.setSeekDurationSeconds(sec) },
        onAutoNextToggled = { enabled -> viewModel.setAutoNextEnabled(enabled) },
        onCycleCropMode = {
            val nextMode = viewModel.cycleVideoScaleMode()
            hudState = GestureHudState.ScaleMode(nextMode)
        },
        onZoomChange = { delta -> viewModel.onZoomChange(delta) },
        onPanChange = { dx, dy, w, h -> viewModel.onPanChange(dx, dy, w, h) },
        onResetZoom = { viewModel.resetZoom() },
        onToggleOrientationLock = onToggleOrientationLock,
        onDecoderModeSelected = { mode -> viewModel.setDecoderMode(mode) },
        onShareClick = onShareClick,
        onDraggingChange = { isDragging -> viewModel.setSliderDragging(isDragging) },
        onRetry = { viewModel.retry() },
        onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
        onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
        onToggleSubtitles = { viewModel.setSubtitlesEnabled(it) },
        onAudioDelayChange = { viewModel.setAudioDelayMs(it) },
        onSubtitleDelayChange = { viewModel.setSubtitleDelayMs(it) },
        subtitleAppearance = subtitleAppearance,
        onSubtitleAppearanceChange = { viewModel.setSubtitleAppearance(it) },
        onAddExternalSubtitleClick = {
            subtitlePickerLauncher.launch(
                arrayOf("text/*", "application/x-subrip", "application/octet-stream", "*/*")
            )
        },
        onTakeScreenshot = onTakeScreenshot,
        onOpenSleepTimer = { showSleepTimerDialog = true },
        onDismissSleepTimer = { showSleepTimerDialog = false },
        onSetSleepTimerMinutes = onSetSleepTimerMinutes,
        onCancelSleepTimer = onCancelSleepTimer,
        onOpenEqualizer = { showEqualizerDialog = true },
        onDismissEqualizer = { showEqualizerDialog = false },
        onAudioBoostSelected = onAudioBoostSelected,
        onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
        onSelectEqualizerPreset = { viewModel.setEqualizerPreset(it) },
        onEqualizerBandChange = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
        modifier = modifier
    )
}

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: NexusPlayer,
    modifier: Modifier = Modifier,
    video: Video? = null,
    currentDecoderMode: DecoderMode = DecoderMode.Hardware,
    panelState: PlayerPanelState = rememberPlayerPanelState(),
    hudState: GestureHudState = GestureHudState.None,
    showAudioSubtitlesSheet: Boolean = false,
    showSubtitleCustomizationDrawer: Boolean = false,
    showSleepTimerDialog: Boolean = false,
    showEqualizerDialog: Boolean = false,
    isPressAndHoldSpeedEnabled: Boolean = true,
    onToggleControls: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onSeekBackward: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    onBrightnessDelta: (Float) -> Unit = {},
    onVolumeDelta: (Float) -> Unit = {},
    onSpeedBoost: (Boolean) -> Unit = {},
    onOpenAudioSubtitles: () -> Unit = {},
    onDismissAudioSubtitles: () -> Unit = {},
    onOpenSubtitleCustomization: () -> Unit = {},
    onDismissSubtitleCustomization: () -> Unit = {},
    onSpeedSelected: (Float) -> Unit = {},
    onSeekDurationSelected: (Int) -> Unit = {},
    onAutoNextToggled: (Boolean) -> Unit = {},
    onCycleCropMode: () -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    onPanChange: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onResetZoom: () -> Unit = {},
    onToggleOrientationLock: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onDecoderModeSelected: (DecoderMode) -> Unit = {},
    onShareClick: () -> Unit = {},
    onDraggingChange: (Boolean) -> Unit = {},
    onRetry: () -> Unit = {},
    onSelectAudioTrack: (String) -> Unit = {},
    onSelectSubtitleTrack: (String?) -> Unit = {},
    onToggleSubtitles: (Boolean) -> Unit = {},
    onAudioDelayChange: (Long) -> Unit = {},
    onSubtitleDelayChange: (Long) -> Unit = {},
    subtitleAppearance: com.nexus.player.core.playback.model.SubtitleAppearance = com.nexus.player.core.playback.model.SubtitleAppearance.DEFAULT,
    onSubtitleAppearanceChange: (com.nexus.player.core.playback.model.SubtitleAppearance) -> Unit = {},
    onAddExternalSubtitleClick: () -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onDismissSleepTimer: () -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onDismissEqualizer: () -> Unit = {},
    onAudioBoostSelected: (Int) -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit = {},
    onSelectEqualizerPreset: (String) -> Unit = {},
    onEqualizerBandChange: (Int, Int) -> Unit = { _, _ -> }
) {
    PlayerLayout(
        panelState = panelState,
        uiState = uiState,
        video = video,
        currentDecoderMode = currentDecoderMode,
        onDecoderModeSelected = onDecoderModeSelected,
        onShareClick = onShareClick,
        onSeekDurationSelected = onSeekDurationSelected,
        onAutoNextToggled = onAutoNextToggled,
        onTakeScreenshot = onTakeScreenshot,
        onOpenSleepTimer = onOpenSleepTimer,
        onOpenEqualizer = onOpenEqualizer,
        onAudioBoostSelected = onAudioBoostSelected,
        videoContent = { videoModifier ->
            Box(
                modifier = videoModifier
                    .background(Color.Black)
                    .testTag("player_screen_root")
            ) {
                val readyState = uiState as? PlayerUiState.Ready
                val scaleMode = readyState?.scaleMode ?: com.nexus.player.core.playback.model.VideoScaleMode.Fit
                val zoom = readyState?.zoom ?: 1.0f
                val panOffsetX = readyState?.panOffsetX ?: 0f
                val panOffsetY = readyState?.panOffsetY ?: 0f

                // Video Surface (rendered at base)
                PlayerVideoSurface(
                    player = player,
                    scaleMode = scaleMode,
                    zoom = zoom,
                    panOffsetX = panOffsetX,
                    panOffsetY = panOffsetY,
                    modifier = Modifier.fillMaxSize()
                )

                // Gesture & Tap Layer
                PlayerGestureSurface(
                    onToggleControls = onToggleControls,
                    onSeekBackward = onSeekBackward,
                    onSeekForward = onSeekForward,
                    onBrightnessDelta = onBrightnessDelta,
                    onVolumeDelta = onVolumeDelta,
                    onSpeedBoost = onSpeedBoost,
                    onZoomChange = onZoomChange,
                    onPanChange = onPanChange,
                    onResetZoom = onResetZoom,
                    isZoomed = zoom > 1.01f,
                    isPressAndHoldSpeedEnabled = isPressAndHoldSpeedEnabled,
                    modifier = Modifier.fillMaxSize()
                )

                // Gesture HUD Feedback
                PlayerGestureHud(
                    hudState = hudState,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlays based on state
                when (uiState) {
                    is PlayerUiState.Loading -> {
                        PlayerLoadingOverlay(
                            videoTitle = uiState.videoTitle,
                            onBackClick = onBackClick
                        )
                    }

                    is PlayerUiState.Error -> {
                        PlayerErrorOverlay(
                            state = uiState,
                            onRetry = onRetry,
                            onBackClick = onBackClick
                        )
                    }

                    is PlayerUiState.Ready -> {
                        PlayerControls(
                            state = uiState,
                            visible = uiState.controlsVisible,
                            onBackClick = onBackClick,
                            onPlayPauseClick = onPlayPauseClick,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            onCycleRepeatMode = onCycleRepeatMode,
                            onToggleShuffle = onToggleShuffle,
                            onSeek = onSeek,
                            onOpenAudioSubtitles = onOpenAudioSubtitles,
                            onSpeedSelected = onSpeedSelected,
                            onCycleCropMode = onCycleCropMode,
                            onToggleOrientationLock = onToggleOrientationLock,
                            onTakeScreenshot = onTakeScreenshot,
                            onOpenSleepTimer = onOpenSleepTimer,
                            onOpenSettings = {
                                panelState.open()
                            },
                            onDraggingChange = onDraggingChange
                        )
                    }
                }
            }
        },
        modifier = modifier
    )

    if (showAudioSubtitlesSheet) {
        val playerState by player.state.collectAsStateWithLifecycle()
        AudioSubtitlesSheet(
            audioTracks = playerState.audioTracks,
            subtitleTracks = playerState.subtitleTracks,
            areSubtitlesEnabled = playerState.areSubtitlesEnabled,
            currentAudioDelayMs = playerState.audioDelayMs,
            currentSubtitleDelayMs = playerState.subtitleDelayMs,
            subtitleAppearance = subtitleAppearance,
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectSubtitleTrack = onSelectSubtitleTrack,
            onToggleSubtitles = onToggleSubtitles,
            onAudioDelayChange = onAudioDelayChange,
            onSubtitleDelayChange = onSubtitleDelayChange,
            onSubtitleAppearanceChange = onSubtitleAppearanceChange,
            onAddExternalSubtitleClick = onAddExternalSubtitleClick,
            onOpenSubtitleCustomization = onOpenSubtitleCustomization,
            onDismissRequest = onDismissAudioSubtitles
        )
    }

    if (showSubtitleCustomizationDrawer) {
        SubtitleCustomizationDrawer(
            subtitleAppearance = subtitleAppearance,
            onSubtitleAppearanceChange = onSubtitleAppearanceChange,
            onDismissRequest = onDismissSubtitleCustomization
        )
    }

    if (showSleepTimerDialog) {
        val remainingSeconds = (uiState as? PlayerUiState.Ready)?.sleepTimerRemainingSeconds
        SleepTimerDialog(
            remainingSeconds = remainingSeconds,
            onSetTimerMinutes = onSetSleepTimerMinutes,
            onCancelTimer = onCancelSleepTimer,
            onDismissRequest = onDismissSleepTimer
        )
    }

    if (showEqualizerDialog) {
        val readyState = uiState as? PlayerUiState.Ready
        val isEqEnabled = readyState?.isEqualizerEnabled ?: false
        val eqPreset = readyState?.equalizerPreset ?: "Flat"
        val bandLevels by player.audioEffectsController.bandLevels.collectAsStateWithLifecycle()
        EqualizerDialog(
            isEnabled = isEqEnabled,
            onToggleEnabled = onToggleEqualizer,
            currentPreset = eqPreset,
            onSelectPreset = onSelectEqualizerPreset,
            bandLevels = bandLevels,
            onBandLevelChange = onEqualizerBandChange,
            onDismissRequest = onDismissEqualizer
        )
    }
}
