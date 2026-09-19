package com.nexus.player.core.ui.component.contextmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Compact Material 3 dialog presenting cached media metadata without redundant filesystem/extractor work.
 */
@Composable
fun VideoFileInfoDialog(
    video: MediaMetadata,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "File Information",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                VerticalSpacer(spacing.extraSmall)

                InfoRow(label = "Filename", value = video.fileName)
                InfoRow(label = "Location", value = video.filePath ?: video.mediaUri)
                InfoRow(
                    label = "File Size",
                    value = "${video.formattedSize} (${video.sizeBytes} bytes)"
                )
                InfoRow(label = "Duration", value = video.formattedDuration)
                InfoRow(
                    label = "Resolution",
                    value = "${video.resolutionLabel} (${video.dimensionsLabel})"
                )
                InfoRow(label = "Video Codec", value = video.videoCodec)
                InfoRow(
                    label = "Audio Codec",
                    value = if (video.audioTrackCount > 1) "${video.audioCodec} (${video.audioTrackCount} tracks)" else video.audioCodec
                )
                InfoRow(label = "Frame Rate", value = video.formattedFps)
                InfoRow(label = "Bitrate", value = video.formattedBitrate)
                InfoRow(label = "Date Added", value = video.formattedModifiedDate)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
