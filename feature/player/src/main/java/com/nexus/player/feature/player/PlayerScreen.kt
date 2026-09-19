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
import com.nexus.player.feature.player.component.PlayerErrorOverlay
import com.nexus.player.feature.player.component.PlayerGestureHud
import com.nexus.player.feature.player.component.PlayerGestureSurface
import com.nexus.player.feature.player.component.PlayerLoadingOverlay
import com.nexus.player.feature.player.component.PlayerVideoSurface
import com.nexus.player.feature.player.orientation.rememberPlayerOrientationController
import com.nexus.player.feature.player.panel.PlayerPanelState
import com.nexus.player.feature.player.panel.rememberPlayerPanelState
import kotlin.math.roundToInt

/**
 * Root Composable for the Video Player destination.
 *
 * Implements:
 * - VLC-style direct player actions and controls
 * - Isolated gesture surface with volume, brightness, seek, and boost gestures
 * - Visual gesture HUD feedback overlays
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

    val playerState by viewModel.player.state.collectAsStateWithLifecycle()
    val isLandscapeVideo = playerState.isLandscapeVideo

    var showAudioSubtitlesSheet by remember { mutableStateOf(false) }
    var hudState by remember { mutableStateOf<GestureHudState>(GestureHudState.None) }

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
            kotlinx.coroutines.delay(1500L)
            hudState = GestureHudState.None
        }
    }

    // Enter fullscreen immediately when video starts and adapt orientation
    LaunchedEffect(isLandscapeVideo) {
        orientationController.enterFullscreen(isLandscapeVideo)
        viewModel.setFullscreen(true)
    }

    // Intercept back button when audio/subtitles sheet is open
    BackHandler(enabled = showAudioSubtitlesSheet) {
        showAudioSubtitlesSheet = false
    }

    // Intercept back button when panel is open
    BackHandler(enabled = panelState.isOpen && !showAudioSubtitlesSheet) {
        if (!panelState.navigateBack()) {
            panelState.close()
            viewModel.setPanelOpen(false)
        }
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

    val handleBackClick: () -> Unit = {
        if (showAudioSubtitlesSheet) {
            showAudioSubtitlesSheet = false
        } else if (panelState.isOpen) {
            panelState.close()
            viewModel.setPanelOpen(false)
        } else {
            orientationController.resetToAppDefault()
            onBackClick()
        }
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
        hudState = GestureHudState.Brightness(percent)
    }

    val onVolumeDelta: (Float) -> Unit = { delta ->
        val newVol = (currentVolume + (delta * maxVolume)).coerceIn(0f, maxVolume.toFloat())
        currentVolume = newVol
        val volInt = newVol.roundToInt()
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volInt, 0)
        val percent = if (maxVolume > 0) ((newVol / maxVolume) * 100).toInt() else 0
        hudState = GestureHudState.Volume(percent)
    }

    val onSeekBackward: () -> Unit = {
        viewModel.seekRelative(-10)
        hudState = GestureHudState.Seek(10, isForward = false)
    }

    val onSeekForward: () -> Unit = {
        viewModel.seekRelative(10)
        hudState = GestureHudState.Seek(10, isForward = true)
    }

    val onSpeedBoost: (Boolean) -> Unit = { boosting ->
        viewModel.setTemporarySpeedBoost(boosting)
        hudState = if (boosting) GestureHudState.SpeedBoost(2.0f) else GestureHudState.None
    }

    val onShareClick: () -> Unit = {
        val uriToShare = video?.mediaUri ?: (uiState as? PlayerUiState.Ready)?.videoId
        if (uriToShare != null) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, Uri.parse(uriToShare))
                putExtra(Intent.EXTRA_TEXT, video?.title ?: "Video")
                type = "video/*"
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
        onToggleControls = { viewModel.toggleControls() },
        onBackClick = handleBackClick,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        onSeek = { positionMs -> viewModel.seekTo(positionMs) },
        onSeekBackward = onSeekBackward,
        onSeekForward = onSeekForward,
        onBrightnessDelta = onBrightnessDelta,
        onVolumeDelta = onVolumeDelta,
        onSpeedBoost = onSpeedBoost,
        onOpenAudioSubtitles = { showAudioSubtitlesSheet = true },
        onDismissAudioSubtitles = { showAudioSubtitlesSheet = false },
        onSpeedSelected = { speed -> viewModel.setPlaybackSpeed(speed) },
        onCycleCropMode = { viewModel.cycleVideoScaleMode() },
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
        subtitleAppearance = playerState.subtitleAppearance,
        onSubtitleAppearanceChange = { viewModel.setSubtitleAppearance(it) },
        onAddExternalSubtitleClick = {
            subtitlePickerLauncher.launch(
                arrayOf("text/*", "application/x-subrip", "application/octet-stream", "*/*")
            )
        },
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
    onToggleControls: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onSeekBackward: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    onBrightnessDelta: (Float) -> Unit = {},
    onVolumeDelta: (Float) -> Unit = {},
    onSpeedBoost: (Boolean) -> Unit = {},
    onOpenAudioSubtitles: () -> Unit = {},
    onDismissAudioSubtitles: () -> Unit = {},
    onSpeedSelected: (Float) -> Unit = {},
    onCycleCropMode: () -> Unit = {},
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
    onAddExternalSubtitleClick: () -> Unit = {}
) {
    val playerState by player.state.collectAsStateWithLifecycle()

    PlayerLayout(
        panelState = panelState,
        uiState = uiState,
        video = video,
        currentDecoderMode = currentDecoderMode,
        onDecoderModeSelected = onDecoderModeSelected,
        onShareClick = onShareClick,
        videoContent = { videoModifier ->
            Box(
                modifier = videoModifier
                    .background(Color.Black)
                    .testTag("player_screen_root")
            ) {
                // Video Surface (rendered at base)
                PlayerVideoSurface(
                    player = player,
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
                            onSeek = onSeek,
                            onOpenAudioSubtitles = onOpenAudioSubtitles,
                            onSpeedSelected = onSpeedSelected,
                            onCycleCropMode = onCycleCropMode,
                            onToggleOrientationLock = onToggleOrientationLock,
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
            onDismissRequest = onDismissAudioSubtitles
        )
    }
}
