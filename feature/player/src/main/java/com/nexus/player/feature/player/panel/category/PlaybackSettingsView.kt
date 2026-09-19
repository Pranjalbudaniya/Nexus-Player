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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
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
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.nexus.player.feature.player.component.formatPlaybackSpeed

val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
fun PlaybackSettingsView(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    currentSeekDurationSeconds: Int = 10,
    onSeekDurationSelected: (Int) -> Unit = {},
    isAutoNextEnabled: Boolean = false,
    onAutoNextToggled: (Boolean) -> Unit = {},
    sleepTimerRemainingSeconds: Long? = null,
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onOpenSleepTimerDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("playback_settings_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NexusTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatPlaybackSpeed(currentSpeed),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Speed preset chips rendered in rows
        val chunked = SPEED_PRESETS.chunked(4)
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
            ) {
                for (speed in row) {
                    val label = if (speed == 1.0f) "1×" else "${speed}×"
                    val isSelected = (currentSpeed - speed) in -0.01f..0.01f

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSpeedSelected(speed) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_preset_${speed}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(NexusTheme.spacing.extraSmall))
        }

        // Custom speed fine tuning
        Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val newSpeed = (Math.round((currentSpeed - 0.05f) * 20f) / 20f).coerceIn(0.25f, 3.0f)
                    onSpeedSelected(newSpeed)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease speed by 0.05"
                )
            }

            Slider(
                value = currentSpeed.coerceIn(0.25f, 3.0f),
                onValueChange = {
                    val stepped = (Math.round(it * 20f) / 20f).coerceIn(0.25f, 3.0f)
                    onSpeedSelected(stepped)
                },
                valueRange = 0.25f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("panel_custom_speed_slider")
            )

            IconButton(
                onClick = {
                    val newSpeed = (Math.round((currentSpeed + 0.05f) * 20f) / 20f).coerceIn(0.25f, 3.0f)
                    onSpeedSelected(newSpeed)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase speed by 0.05"
                )
            }
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Seek Duration",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            listOf(5, 10, 15, 30).forEach { sec ->
                FilterChip(
                    selected = currentSeekDurationSeconds == sec,
                    onClick = { onSeekDurationSelected(sec) },
                    label = { Text("${sec}s", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("panel_seek_duration_${sec}s")
                )
            }
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Repeat Mode",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            listOf("Off", "All", "One").forEach { mode ->
                FilterChip(
                    selected = mode == "Off",
                    onClick = {},
                    label = { Text(mode, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NexusTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (sleepTimerRemainingSeconds != null && sleepTimerRemainingSeconds > 0) {
                val mins = sleepTimerRemainingSeconds / 60
                val secs = sleepTimerRemainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d left", mins, secs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
        ) {
            FilterChip(
                selected = sleepTimerRemainingSeconds == null,
                onClick = onCancelSleepTimer,
                label = { Text("Off", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("panel_sleep_timer_off")
            )
            listOf(15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
                FilterChip(
                    selected = false,
                    onClick = { onSetSleepTimerMinutes(mins) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("panel_sleep_timer_${mins}m")
                )
            }
            FilterChip(
                selected = false,
                onClick = onOpenSleepTimerDialog,
                label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("panel_sleep_timer_custom")
            )
        }

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Other Options",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Resume Playback",
            subtitle = "Restore position when under 95%",
            leadingIcon = Icons.Filled.Replay,
            trailingValue = "Enabled",
            onClick = {},
            enabled = false,
            badgeText = "Always On"
        )

        PlayerSettingItem(
            title = "Auto-play Next",
            subtitle = "Play subsequent video automatically",
            leadingIcon = Icons.Filled.SkipNext,
            onClick = {},
            enabled = false,
            badgeText = "Coming Soon"
        )
    }
}
