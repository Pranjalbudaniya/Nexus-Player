package com.nexus.player.feature.player.panel.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
fun PlaybackSettingsView(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("playback_settings_view")
    ) {
        Text(
            text = "Playback Speed",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        // Speed presets rendered in 4-column rows
        val chunked = SPEED_PRESETS.chunked(4)
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
            ) {
                for (speed in row) {
                    val label = if (speed == 1.0f) "Normal" else "${speed}×"
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
            listOf("5s", "10s", "15s", "30s").forEach { step ->
                FilterChip(
                    selected = step == "10s",
                    onClick = {},
                    label = { Text(step, style = MaterialTheme.typography.labelMedium) },
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

        Text(
            text = "Sleep Timer",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            listOf("Off", "15m", "30m", "45m", "60m").forEach { time ->
                FilterChip(
                    selected = time == "Off",
                    onClick = {},
                    label = { Text(time, style = MaterialTheme.typography.labelSmall) },
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
