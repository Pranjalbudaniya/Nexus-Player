package com.nexus.player.feature.player.panel.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.media.model.formatFileSize
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.PlayerUiState
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Equalizer

/**
 * Flat list of genuinely advanced player actions and technical metadata for Step 17 & 20.
 *
 * Excludes standard playback bar items while offering fast access to audio utilities,
 * sleep timer, equalizer, screenshots, and hardware settings.
 */
@Composable
fun MoreSettingsView(
    video: Video?,
    state: PlayerUiState.Ready,
    currentDecoderMode: DecoderMode,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    onShareClick: () -> Unit,
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
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("more_settings_view")
    ) {
        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Text(
            text = "Seek Duration",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = NexusTheme.spacing.extraSmall, bottom = NexusTheme.spacing.extraSmall)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            listOf(5, 10, 15, 30).forEach { sec ->
                FilterChip(
                    selected = state.seekDurationSeconds == sec,
                    onClick = { onSeekDurationSelected(sec) },
                    label = { Text("${sec}s", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seek_duration_${sec}s")
                )
            }
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        if (state.queueSize > 1) {
            PlayerSettingItem(
                title = "Auto-play Next",
                subtitle = "Play subsequent video automatically",
                leadingIcon = Icons.Filled.SkipNext,
                trailingValue = if (state.isAutoNextEnabled) "On" else "Off",
                onClick = { onAutoNextToggled(!state.isAutoNextEnabled) },
                testTag = "setting_auto_next"
            )
            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        Text(
            text = "Audio & Utilities",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Sleep Timer",
            subtitle = if (state.sleepTimerRemainingSeconds != null && state.sleepTimerRemainingSeconds > 0) {
                val mins = state.sleepTimerRemainingSeconds / 60
                val secs = state.sleepTimerRemainingSeconds % 60
                String.format(java.util.Locale.ROOT, "%02d:%02d remaining", mins, secs)
            } else {
                "Turn off playback automatically"
            },
            leadingIcon = Icons.Filled.Timer,
            trailingValue = if (state.sleepTimerRemainingSeconds != null && state.sleepTimerRemainingSeconds > 0) "Active" else "Off",
            onClick = onOpenSleepTimer,
            testTag = "action_sleep_timer"
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        PlayerSettingItem(
            title = "Equalizer",
            subtitle = if (state.isEqualizerEnabled) "Active: ${state.equalizerPreset}" else "Disabled",
            leadingIcon = Icons.Filled.Equalizer,
            trailingValue = if (state.isEqualizerEnabled) state.equalizerPreset else "Off",
            onClick = onOpenEqualizer,
            testTag = "action_equalizer"
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Take Screenshot",
            subtitle = "Capture current video frame to Pictures",
            leadingIcon = Icons.Filled.CameraAlt,
            onClick = onTakeScreenshot,
            testTag = "action_take_screenshot"
        )

        PlayerSettingItem(
            title = "Share Video",
            subtitle = "Share video via system apps",
            leadingIcon = Icons.Filled.Share,
            onClick = onShareClick,
            testTag = "action_share_video"
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        // Decoder mode selection
        Text(
            text = "Hardware Acceleration",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(DecoderMode.Hardware, DecoderMode.Software, DecoderMode.Auto).forEach { mode ->
                FilterChip(
                    selected = currentDecoderMode == mode,
                    onClick = { onDecoderModeSelected(mode) },
                    label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        Text(
            text = if (state.isNetworkMedia) "Stream Information" else "File Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = if (state.isNetworkMedia) "Stream Title" else "File Name",
            subtitle = video?.fileName ?: state.videoTitle,
            leadingIcon = Icons.Filled.Info,
            onClick = {},
            enabled = false
        )

        if (state.isNetworkMedia) {
            PlayerSettingItem(
                title = "Stream URL",
                subtitle = state.videoId,
                leadingIcon = Icons.Filled.Language,
                onClick = {},
                enabled = false
            )
        } else {
            if (video?.folderPath != null) {
                PlayerSettingItem(
                    title = "Folder Location",
                    subtitle = video.folderPath,
                    leadingIcon = Icons.Filled.Folder,
                    onClick = {},
                    enabled = false
                )
            }

            if (video?.sizeBytes != null && video.sizeBytes > 0L) {
                PlayerSettingItem(
                    title = "File Size",
                    subtitle = formatFileSize(video.sizeBytes),
                    leadingIcon = Icons.Filled.SdCard,
                    onClick = {},
                    enabled = false
                )
            }
        }

        PlayerSettingItem(
            title = "Duration",
            subtitle = if (state.durationMs > 0L) formatDuration(state.durationMs) else if (state.isNetworkMedia) "Live / Unknown" else "00:00",
            leadingIcon = Icons.Filled.Timer,
            onClick = {},
            enabled = false
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

        Text(
            text = "Playback Statistics",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Current Position",
            subtitle = "${formatDuration(state.currentPositionMs)} (${(state.progressFraction * 100).toInt()}%)",
            leadingIcon = Icons.Filled.Analytics,
            onClick = {},
            enabled = false
        )

        PlayerSettingItem(
            title = "Buffered Buffer",
            subtitle = "${formatDuration(state.bufferedPositionMs)} (${(state.bufferedFraction * 100).toInt()}%)",
            leadingIcon = Icons.Filled.Analytics,
            onClick = {},
            enabled = false
        )
    }
}
