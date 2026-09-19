package com.nexus.player.feature.player.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.PlayerUiState
import com.nexus.player.feature.player.panel.category.MoreSettingsView

/**
 * Clean, flat presentation for the More Actions player panel in Step 17.
 *
 * Adheres to Step 17 requirement 8 & 9:
 * - Flat list of genuinely advanced / less-common actions
 * - Zero nested categories or sub-menus
 * - No duplicated Audio, Subtitle, Crop, Orientation, or Fullscreen controls
 */
@Composable
fun PlayerSettingsContent(
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_settings_content")
    ) {
        // Panel Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = NexusTheme.spacing.medium,
                    vertical = NexusTheme.spacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "More Actions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("panel_title")
            )

            IconButton(
                onClick = onClosePanel,
                modifier = Modifier.testTag("panel_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Flat advanced content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            MoreSettingsView(
                video = video,
                state = state,
                currentDecoderMode = currentDecoderMode,
                onDecoderModeSelected = onDecoderModeSelected,
                onShareClick = onShareClick,
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
