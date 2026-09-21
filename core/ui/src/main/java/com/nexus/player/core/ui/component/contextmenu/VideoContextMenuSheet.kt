package com.nexus.player.core.ui.component.contextmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.isNetworkMedia
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Unified context menu bottom sheet for video actions across Nexus Player.
 * Reused consistently across Home, Library, Search, Folder, and Playlist screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoContextMenuSheet(
    video: MediaMetadata,
    onPlay: (String) -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onShowFileInfo: () -> Unit,
    onOpenContainingFolder: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = NexusTheme.spacing
    val isNetwork = video.isNetworkMedia

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.medium)
        ) {
            // Header Preview
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.extraSmall)

            val subtitleText = if (isNetwork) {
                if (video.formattedDuration.isNotBlank() && video.formattedDuration != "00:00") {
                    "${video.formattedDuration} • Network Stream"
                } else {
                    "Network Stream"
                }
            } else {
                "${video.formattedDuration} • ${video.resolutionLabel} • ${video.formattedSize} • ${video.folderName}"
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalSpacer(spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerticalSpacer(spacing.extraSmall)

            // 1. Play
            ContextMenuActionRow(
                icon = Icons.Default.PlayArrow,
                label = "Play",
                tint = MaterialTheme.colorScheme.primary,
                onClick = {
                    onDismissRequest()
                    onPlay(video.id)
                }
            )

            // 2. Add to Playlist
            ContextMenuActionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = "Add to Playlist",
                onClick = {
                    onDismissRequest()
                    onAddToPlaylist()
                }
            )

            // 3. Favorite / Unfavorite
            ContextMenuActionRow(
                icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (video.isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (video.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    onDismissRequest()
                    onToggleFavorite()
                }
            )

            // 4. Share
            ContextMenuActionRow(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {
                    onDismissRequest()
                    onShare()
                }
            )

            // 5. File Information / Stream Information
            ContextMenuActionRow(
                icon = Icons.Default.Info,
                label = if (isNetwork) "Stream Information" else "File Information",
                onClick = {
                    onDismissRequest()
                    onShowFileInfo()
                }
            )

            // Local-only file management actions (strictly excluded for network streams)
            if (!isNetwork) {
                // 6. Open Containing Folder
                ContextMenuActionRow(
                    icon = Icons.Default.FolderOpen,
                    label = "Open Containing Folder",
                    onClick = {
                        onDismissRequest()
                        onOpenContainingFolder()
                    }
                )

                // 7. Rename
                ContextMenuActionRow(
                    icon = Icons.Default.Edit,
                    label = "Rename",
                    onClick = {
                        onDismissRequest()
                        onRename()
                    }
                )

                // 8. Move
                ContextMenuActionRow(
                    icon = Icons.Default.DriveFileMove,
                    label = "Move",
                    onClick = {
                        onDismissRequest()
                        onMove()
                    }
                )

                // 9. Copy
                ContextMenuActionRow(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = {
                        onDismissRequest()
                        onCopy()
                    }
                )

                // 10. Delete
                ContextMenuActionRow(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismissRequest()
                        onDelete()
                    }
                )
            }

            VerticalSpacer(spacing.medium)
        }
    }
}

@Composable
private fun ContextMenuActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val spacing = NexusTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = spacing.small, horizontal = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        HorizontalSpacer(spacing.medium)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor
        )
    }
}
