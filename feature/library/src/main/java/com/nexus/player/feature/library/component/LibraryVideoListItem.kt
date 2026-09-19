package com.nexus.player.feature.library.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusBadge
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Compact list item row for Nexus Player Library.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryVideoListItem(
    video: MediaMetadata,
    thumbnailLoader: ThumbnailLoader,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            )
            .padding(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Container (16:9, fixed 120dp width)
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
        ) {
            NexusAsyncThumbnail(
                mediaUri = video.mediaUri,
                thumbnailLoader = thumbnailLoader,
                contentDescription = video.title,
                modifier = Modifier.matchParentSize()
            )

            // Quality Badge Top-Left
            if (video.resolutionLabel.isNotBlank()) {
                NexusBadge(
                    text = video.resolutionLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(spacing.extraSmall)
                )
            }

            // Duration Badge Bottom-Right
            NexusBadge(
                text = video.formattedDuration,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.extraSmall)
            )

            // Progress Indicator
            if (video.playbackPercentage > 0f) {
                LinearProgressIndicator(
                    progress = { video.playbackPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalSpacer(spacing.medium)

        // Metadata Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.extraSmall)

            Text(
                text = "${video.formattedSize} • ${video.resolutionLabel} • ${video.formattedDuration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.extraSmall)

            Text(
                text = "${video.folderName} • ${video.formattedModifiedDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More Options affordance
        IconButton(onClick = onLongClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Video options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
