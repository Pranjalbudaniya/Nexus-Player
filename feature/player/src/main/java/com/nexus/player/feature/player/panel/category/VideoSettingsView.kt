package com.nexus.player.feature.player.panel.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

@Composable
fun VideoSettingsView(
    video: Video?,
    currentDecoderMode: DecoderMode,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("video_settings_view")
    ) {
        Text(
            text = "Decoder Mode",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Hardware Decoder",
            subtitle = "Device GPU accelerated decoding (default)",
            leadingIcon = Icons.Filled.Memory,
            isSelected = currentDecoderMode == DecoderMode.Hardware,
            onClick = { onDecoderModeSelected(DecoderMode.Hardware) },
            testTag = "decoder_hardware"
        )

        PlayerSettingItem(
            title = "Software Decoder",
            subtitle = "CPU fallback decoder",
            leadingIcon = Icons.Filled.SettingsSystemDaydream,
            isSelected = currentDecoderMode == DecoderMode.Software,
            onClick = { onDecoderModeSelected(DecoderMode.Software) },
            testTag = "decoder_software"
        )

        PlayerSettingItem(
            title = "Auto Selection",
            subtitle = "Automatic hardware with software fallback",
            leadingIcon = Icons.Filled.Videocam,
            isSelected = currentDecoderMode == DecoderMode.Auto,
            onClick = { onDecoderModeSelected(DecoderMode.Auto) },
            testTag = "decoder_auto"
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Video Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        val resolutionText = if (video != null && video.width > 0 && video.height > 0) {
            "${video.width} × ${video.height} (${video.resolutionLabel})"
        } else {
            video?.resolutionLabel ?: "Standard"
        }

        PlayerSettingItem(
            title = "Resolution",
            subtitle = "Display pixel dimensions",
            leadingIcon = Icons.Filled.Info,
            trailingValue = resolutionText,
            onClick = {},
            enabled = false
        )

        PlayerSettingItem(
            title = "Video Track",
            subtitle = "Stream index #0",
            leadingIcon = Icons.Filled.Videocam,
            trailingValue = "Track 1",
            onClick = {},
            enabled = false
        )
    }
}
