package com.nexus.player.feature.player.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.playback.model.VideoScaleMode
import com.nexus.player.feature.player.PlayerUiState

/**
 * VLC-style powerful video player controls overlay for Step 17.
 *
 * Layout:
 * - Top bar: Back navigation, video title, and More actions button
 * - Center: Primary Play/Pause toggle + Buffering spinner
 * - Bottom:
 *   - Seek timeline slider with timestamps
 *   - Direct player actions row:
 *     - [Audio + Subtitles] combined button
 *     - [Playback Speed] direct pill
 *     - [Crop / Video Mode] 1-tap cycle button (Fit, Fill, Crop, Stretch, Original)
 *     - [Orientation Lock] toggle button
 *     - [Fullscreen] toggle button
 */
@Composable
fun PlayerControls(
    state: PlayerUiState.Ready,
    visible: Boolean,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenAudioSubtitles: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCycleCropMode: () -> Unit,
    onToggleOrientationLock: () -> Unit,
    onToggleFullscreen: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onDraggingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val displayPositionMs = if (isDraggingSlider) dragPositionMs else state.currentPositionMs

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_controls_overlay")
        ) {
            // Top Gradient Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = NexusTheme.spacing.medium, vertical = NexusTheme.spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(NexusTheme.spacing.small))

                        Text(
                            text = state.videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("player_video_title")
                        )
                    }

                    // More Menu (Flat list of advanced actions)
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("player_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More player actions",
                            tint = Color.White
                        )
                    }
                }
            }

            // Center Play / Pause Button
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_buffering_indicator"),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_play_pause_button")
                            .clickable(onClick = onPlayPauseClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    state.isEnded -> Icons.Filled.Replay
                                    state.isPlaying -> Icons.Filled.Pause
                                    else -> Icons.Filled.PlayArrow
                                },
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Bottom VLC-Style Control Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = NexusTheme.spacing.medium, vertical = NexusTheme.spacing.small)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
                ) {
                    // Seek Slider
                    Slider(
                        value = displayPositionMs.toFloat(),
                        onValueChange = { newPos ->
                            isDraggingSlider = true
                            onDraggingChange(true)
                            dragPositionMs = newPos.toLong()
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onDraggingChange(false)
                            onSeek(dragPositionMs)
                        },
                        valueRange = 0f..(state.durationMs.toFloat().coerceAtLeast(1f)),
                        enabled = state.isSeekable && state.durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider")
                    )

                    // Timestamps & Direct VLC Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Combined Audio + Subtitles Button + Timestamps
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
                        ) {
                            // Audio + Subtitle dedicated button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .testTag("player_audio_subtitles_button")
                                    .clickable(onClick = onOpenAudioSubtitles)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Audiotrack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Subtitles,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (state.isPanelOpen) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                            }

                            // Current / Total time
                            Text(
                                text = "${formatDuration(displayPositionMs)} / ${formatDuration(state.durationMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.testTag("player_current_time")
                            )
                        }

                        // Right: Playback Speed, 1-Tap Crop, Orientation Lock, Fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Speed Pill
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .testTag("player_speed_button")
                                    .clickable { showSpeedDialog = true }
                            ) {
                                Text(
                                    text = "${state.playbackSpeed}×",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // 1-Tap Crop / Video Mode Button
                            IconButton(
                                onClick = onCycleCropMode,
                                modifier = Modifier.testTag("player_crop_mode_button")
                            ) {
                                val icon = when (state.scaleMode) {
                                    VideoScaleMode.Fit -> Icons.Filled.AspectRatio
                                    VideoScaleMode.Fill -> Icons.Filled.FitScreen
                                    VideoScaleMode.Crop -> Icons.Filled.Crop
                                    VideoScaleMode.Stretch -> Icons.Filled.ZoomOutMap
                                    VideoScaleMode.Original -> Icons.Filled.PhotoSizeSelectActual
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Video Mode: ${state.scaleMode.label}",
                                    tint = Color.White
                                )
                            }

                            // Orientation Lock Button
                            IconButton(
                                onClick = onToggleOrientationLock,
                                modifier = Modifier.testTag("player_orientation_lock_button")
                            ) {
                                Icon(
                                    imageVector = if (state.isOrientationLocked) {
                                        Icons.Filled.ScreenLockRotation
                                    } else {
                                        Icons.Filled.ScreenRotation
                                    },
                                    contentDescription = if (state.isOrientationLocked) "Orientation Locked" else "Orientation Unlocked",
                                    tint = if (state.isOrientationLocked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White
                                    }
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("player_speed_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback Speed",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${state.playbackSpeed}×",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Speed preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f).forEach { speed ->
                            FilterChip(
                                selected = state.playbackSpeed == speed,
                                onClick = {
                                    onSpeedSelected(speed)
                                    showSpeedDialog = false
                                },
                                label = { Text("${speed}×", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1.5f, 1.75f, 2.0f).forEach { speed ->
                            FilterChip(
                                selected = state.playbackSpeed == speed,
                                onClick = {
                                    onSpeedSelected(speed)
                                    showSpeedDialog = false
                                },
                                label = { Text("${speed}×", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Custom slider
                    Slider(
                        value = state.playbackSpeed,
                        onValueChange = { onSpeedSelected((it * 4).toInt() / 4f) },
                        valueRange = 0.25f..2.5f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}
