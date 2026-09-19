package com.nexus.player.feature.player.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomOutMap
import com.nexus.player.core.playback.model.VideoScaleMode
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * State representing active gesture HUD visual feedback.
 */
sealed interface GestureHudState {
    data object None : GestureHudState
    data class Seek(val deltaSeconds: Int, val isForward: Boolean) : GestureHudState
    data class Brightness(val percent: Int) : GestureHudState
    data class Volume(val percent: Int) : GestureHudState
    data class SpeedBoost(val speedMultiplier: Float) : GestureHudState
    data class Screenshot(val success: Boolean, val message: String = if (success) "Screenshot saved" else "Screenshot failed") : GestureHudState
    data class SleepTimer(val message: String) : GestureHudState
    data class AudioBoost(val percent: Int) : GestureHudState
    data class ScaleMode(val mode: VideoScaleMode) : GestureHudState
}

/**
 * Non-intrusive lightweight HUD overlays for player interactions:
 * - Seek forward / backward pills on left / right
 * - Brightness vertical indicator in center
 * - Volume vertical indicator in center
 * - Temporary speed boost pill at top center
 * - Screenshot confirmation pill in center
 * - Sleep timer pill in center
 */
@Composable
fun PlayerGestureHud(
    hudState: GestureHudState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_gesture_hud")
    ) {
        // Speed Boost HUD (top center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.SpeedBoost,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 48.dp)
        ) {
            if (hudState is GestureHudState.SpeedBoost) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${formatPlaybackSpeed(hudState.speedMultiplier)} Fast Forward",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Seek HUD (Left / Right)
        AnimatedVisibility(
            visible = hudState is GestureHudState.Seek,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(
                if ((hudState as? GestureHudState.Seek)?.isForward == true) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            )
        ) {
            if (hudState is GestureHudState.Seek) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hudState.isForward) Icons.Filled.Forward10 else Icons.Filled.Replay10,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${if (hudState.isForward) "+" else "-"}${hudState.deltaSeconds}s",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Brightness HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.Brightness,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.Brightness) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hudState.percent < 50) Icons.Filled.BrightnessLow else Icons.Filled.BrightnessMedium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        LinearProgressIndicator(
                            progress = { hudState.percent / 100f },
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        )
                        Text(
                            text = "${hudState.percent}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Volume HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.Volume,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.Volume) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val icon = when {
                            hudState.percent == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                            hudState.percent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        LinearProgressIndicator(
                            progress = { hudState.percent / 100f },
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        )
                        Text(
                            text = "${hudState.percent}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Screenshot Feedback HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.Screenshot,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.Screenshot) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = hudState.message,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Sleep Timer Feedback HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.SleepTimer,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.SleepTimer) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = hudState.message,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Audio Boost Feedback HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.AudioBoost,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.AudioBoost) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Audio Boost: ${hudState.percent}%",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Scale / Display Mode Feedback HUD (Center)
        AnimatedVisibility(
            visible = hudState is GestureHudState.ScaleMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (hudState is GestureHudState.ScaleMode) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = when (hudState.mode) {
                            VideoScaleMode.Fit -> Icons.Filled.AspectRatio
                            VideoScaleMode.Fill -> Icons.Filled.FitScreen
                            VideoScaleMode.Crop -> Icons.Filled.Crop
                            VideoScaleMode.Stretch -> Icons.Filled.ZoomOutMap
                            VideoScaleMode.Original -> Icons.Filled.PhotoSizeSelectActual
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = hudState.mode.label,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
