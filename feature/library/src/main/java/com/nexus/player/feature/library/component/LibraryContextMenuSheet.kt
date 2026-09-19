package com.nexus.player.feature.library.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Context menu bottom sheet entry point triggered on long-press of a video item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContextMenuSheet(
    video: MediaMetadata,
    onPlay: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = NexusTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .navigationBarsPadding()
        ) {
            // Header with Video Metadata Preview
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.extraSmall)

            Text(
                text = "${video.formattedDuration} • ${video.resolutionLabel} • ${video.formattedSize} • ${video.folderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.medium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerticalSpacer(spacing.small)

            // Play Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            onDismissRequest()
                            onPlay(video.id)
                        }
                    )
                    .padding(vertical = spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                HorizontalSpacer(spacing.medium)
                Text(
                    text = "Play Video",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Info Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = onDismissRequest
                    )
                    .padding(vertical = spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalSpacer(spacing.medium)
                Text(
                    text = "Video Details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Close Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = onDismissRequest
                    )
                    .padding(vertical = spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalSpacer(spacing.medium)
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            VerticalSpacer(spacing.medium)
        }
    }
}
