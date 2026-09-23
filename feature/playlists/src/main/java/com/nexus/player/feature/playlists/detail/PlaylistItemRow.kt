package com.nexus.player.feature.playlists.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.player.core.database.model.PlaylistItem
import com.nexus.player.core.designsystem.theme.NexusTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItemRow(
    item: PlaylistItem,
    index: Int,
    totalCount: Int,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes
    var isMenuOpen by remember { mutableStateOf(false) }

    val isAvailable = item.isAvailable
    val video = item.video

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.thumbnail)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                enabled = isAvailable,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = spacing.medium, vertical = spacing.small)
            .then(if (!isAvailable) Modifier.alpha(0.6f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(spacing.smallMedium))

        // Position index
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(spacing.small))

        // Video title and meta
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            Text(
                text = video?.title ?: "Unavailable Video (${item.videoId})",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isAvailable && video != null) {
                Text(
                    text = "${video.resolutionLabel} • ${formatDuration(video.durationMs)} • ${video.folderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "File missing or deleted from storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Action / options
        Box {
            IconButton(onClick = { isMenuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Video options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                if (isAvailable) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onClick()
                        }
                    )
                }

                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onMoveUpClick()
                        }
                    )
                }

                if (index < totalCount - 1) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onMoveDownClick()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Remove from playlist", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        onRemoveClick()
                    }
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMinutes = minutes % 60
        String.format("%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
