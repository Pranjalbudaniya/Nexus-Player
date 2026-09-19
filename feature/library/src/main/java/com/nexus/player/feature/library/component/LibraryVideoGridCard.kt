package com.nexus.player.feature.library.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.NexusBadge
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Grid video card for Nexus Player Library.
 *
 * Media Layout Structure:
 * ┌───────────────────────────┐
 * │ QUALITY (top-left)        │
 * │                           │
 * │     THUMBNAIL CANVAS      │
 * │                           │
 * │        LENGTH (btm-right) │
 * └───────────────────────────┘
 * Video Title
 * Size & Folder
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryVideoGridCard(
    video: MediaMetadata,
    thumbnailLoader: ThumbnailLoader,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            )
            .padding(spacing.extraSmall)
    ) {
        // Thumbnail Container (16:9 Aspect Ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            NexusAsyncThumbnail(
                mediaUri = video.mediaUri,
                thumbnailLoader = thumbnailLoader,
                contentDescription = video.title,
                modifier = Modifier.matchParentSize()
            )

            // Quality Badge at Top-Left
            if (video.resolutionLabel.isNotBlank()) {
                NexusBadge(
                    text = video.resolutionLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(spacing.small)
                )
            }

            // Duration Badge at Bottom-Right
            NexusBadge(
                text = video.formattedDuration,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.small)
            )

            // Playback Progress Indicator if video has been started
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

        VerticalSpacer(spacing.small)

        // Video Title
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        VerticalSpacer(spacing.extraSmall)

        // Secondary Metadata: Size & Date
        Text(
            text = "${video.formattedSize} • ${video.formattedModifiedDate}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
