package com.nexus.player.feature.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.panel.PlayerPanelState
import com.nexus.player.feature.player.panel.PlayerSettingsBottomSheet
import com.nexus.player.feature.player.panel.PlayerSettingsSidePanel

/**
 * Responsive orchestrator adapting player regions to available window width.
 *
 * Guarantees:
 * - The video content node remains anchored at the exact same location in the Compose
 *   hierarchy to avoid destroying or recreating the underlying video surface during
 *   orientation changes (portrait <-> landscape).
 * - Letterboxing and player background are strictly pure black ([Color.Black]).
 * - Wide displays (width >= 600dp in landscape) reflow video alongside the More side panel.
 * - Portrait / narrow displays show More actions via modal bottom sheet.
 */
@Composable
fun PlayerLayout(
    panelState: PlayerPanelState,
    uiState: PlayerUiState,
    video: Video?,
    currentDecoderMode: DecoderMode,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    onShareClick: () -> Unit,
    onSeekDurationSelected: (Int) -> Unit = {},
    onAutoNextToggled: (Boolean) -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onAudioBoostSelected: (Int) -> Unit = {},
    videoContent: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_layout_container")
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isWideLayout = totalWidth >= 600.dp && (totalWidth > totalHeight || totalWidth >= 840.dp)

        // Adaptive side-panel width (36% of width, bounded between 320dp and 420dp)
        val targetPanelWidth = if (isWideLayout && panelState.isOpen) {
            (totalWidth * 0.36f).coerceIn(320.dp, 420.dp)
        } else {
            0.dp
        }

        val animatedPanelWidth by animateDpAsState(
            targetValue = targetPanelWidth,
            animationSpec = tween(durationMillis = 250),
            label = "side_panel_width_animation"
        )

        // Update panel transition state
        LaunchedEffect(animatedPanelWidth, targetPanelWidth) {
            if (targetPanelWidth > 0.dp && animatedPanelWidth == targetPanelWidth) {
                panelState.markOpened()
            } else if (targetPanelWidth == 0.dp && animatedPanelWidth == 0.dp) {
                panelState.markClosed()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Video region shrinks horizontally when side panel opens on wide displays
                val videoWidth = if (isWideLayout) {
                    (totalWidth - animatedPanelWidth).coerceAtLeast(0.dp)
                } else {
                    totalWidth
                }

                Box(
                    modifier = Modifier
                        .width(videoWidth)
                        .fillMaxHeight()
                        .background(Color.Black)
                        .testTag("player_video_region")
                ) {
                    videoContent(Modifier.fillMaxSize())
                }

                // Side panel region (only on wide screens when opened)
                if (isWideLayout && animatedPanelWidth > 0.dp && uiState is PlayerUiState.Ready) {
                    PlayerSettingsSidePanel(
                        panelState = panelState,
                        state = uiState,
                        video = video,
                        currentDecoderMode = currentDecoderMode,
                        onDecoderModeSelected = onDecoderModeSelected,
                        onShareClick = onShareClick,
                        onClosePanel = { panelState.close() },
                        onSeekDurationSelected = onSeekDurationSelected,
                        onAutoNextToggled = onAutoNextToggled,
                        onTakeScreenshot = onTakeScreenshot,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onOpenEqualizer = onOpenEqualizer,
                        onAudioBoostSelected = onAudioBoostSelected,
                        modifier = Modifier.width(animatedPanelWidth)
                    )
                }
            }

            // Bottom sheet (only on narrow / portrait screens when opened)
            if (!isWideLayout && panelState.isOpen && uiState is PlayerUiState.Ready) {
                PlayerSettingsBottomSheet(
                    panelState = panelState,
                    state = uiState,
                    video = video,
                    currentDecoderMode = currentDecoderMode,
                    onDecoderModeSelected = onDecoderModeSelected,
                    onShareClick = onShareClick,
                    onClosePanel = { panelState.close() },
                    onSeekDurationSelected = onSeekDurationSelected,
                    onAutoNextToggled = onAutoNextToggled,
                    onTakeScreenshot = onTakeScreenshot,
                    onOpenSleepTimer = onOpenSleepTimer,
                    onOpenEqualizer = onOpenEqualizer,
                    onAudioBoostSelected = onAudioBoostSelected
                )
            }
        }
    }
}
