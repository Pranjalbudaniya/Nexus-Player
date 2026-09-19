package com.nexus.player.feature.player.panel.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

@Composable
fun AudioSettingsView(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("audio_settings_view")
    ) {
        Text(
            text = "Volume & Output",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Audio Mute",
            subtitle = if (isMuted) "Audio is currently muted" else "Normal volume output",
            leadingIcon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            trailingValue = if (isMuted) "Muted" else "Active",
            isSelected = isMuted,
            onClick = onToggleMute,
            testTag = "audio_mute_toggle"
        )

        PlayerSettingItem(
            title = "Audio Track",
            subtitle = "Active audio stream",
            leadingIcon = Icons.Filled.Audiotrack,
            trailingValue = "Track 1 (Stereo)",
            onClick = {},
            enabled = false
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Audio Boost (up to 200%)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            for (boost in listOf("100%", "125%", "150%", "200%")) {
                FilterChip(
                    selected = boost == "100%",
                    onClick = {},
                    label = { Text(boost, style = MaterialTheme.typography.labelMedium) },
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
            text = "Audio Delay / Sync",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            for (delay in listOf("-200ms", "-100ms", "0ms", "+100ms", "+200ms")) {
                FilterChip(
                    selected = delay == "0ms",
                    onClick = {},
                    label = { Text(delay, style = MaterialTheme.typography.labelSmall) },
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
            text = "Advanced Audio",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Equalizer",
            subtitle = "Audio frequency balance controls",
            leadingIcon = Icons.Filled.Equalizer,
            onClick = {},
            enabled = false,
            badgeText = "Coming Soon"
        )
    }
}
