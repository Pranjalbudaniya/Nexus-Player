package com.nexus.player.feature.library.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.NexusBadge
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Grid folder card for Nexus Player Library.
 * Displays representative thumbnail (or folder placeholder), folder badge,
 * folder name, and video count badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryFolderCard(
    folder: VideoFolder,
    thumbnailLoader: ThumbnailLoader,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .combinedClickable(
                onClick = onClick,
                role = Role.Button
            )
            .padding(spacing.extraSmall)
    ) {
        // Thumbnail Container (16:9 Aspect Ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shapes.card)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            val previewMediaUri = folder.previewMediaUri
            if (previewMediaUri != null) {
                NexusAsyncThumbnail(
                    mediaUri = previewMediaUri,
                    thumbnailLoader = thumbnailLoader,
                    contentDescription = folder.folderName,
                    modifier = Modifier.matchParentSize()
                )
                // Folder Tag at Top-Left
                NexusBadge(
                    text = "FOLDER",
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(spacing.small)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Video Count Badge at Bottom-Right
            val countText = if (folder.videoCount == 1) "1 video" else "${folder.videoCount} videos"
            NexusBadge(
                text = countText,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.small)
            )
        }

        VerticalSpacer(spacing.small)

        // Folder Title
        Text(
            text = folder.folderName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        VerticalSpacer(spacing.extraSmall)

        // Video count subtitle
        val countText = if (folder.videoCount == 1) "1 video" else "${folder.videoCount} videos"
        Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
