package com.nexus.player.feature.player.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.PlayerUiState

/**
 * Side panel container for wide landscape and tablet screen layouts.
 *
 * Sits beside the video surface in the horizontal row layout without
 * obscuring the video.
 */
@Composable
fun PlayerSettingsSidePanel(
    panelState: PlayerPanelState,
    state: PlayerUiState.Ready,
    video: Video?,
    currentDecoderMode: DecoderMode,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    onShareClick: () -> Unit,
    onClosePanel: () -> Unit,
    onSeekDurationSelected: (Int) -> Unit = {},
    onAutoNextToggled: (Boolean) -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onAudioBoostSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxHeight()
            .testTag("player_settings_side_panel")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            PlayerSettingsContent(
                panelState = panelState,
                state = state,
                video = video,
                currentDecoderMode = currentDecoderMode,
                onDecoderModeSelected = onDecoderModeSelected,
                onShareClick = onShareClick,
                onClosePanel = onClosePanel,
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
