package com.nexus.player.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Reusable Folder Card Foundation for the "Folders" horizontal section.
 */
@Composable
fun NexusFolderCard(
    folderName: String,
    videoCountText: String,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 144.dp,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes
    val dimensions = NexusTheme.dimensions

    Box(
        modifier = modifier
            .width(cardWidth)
            .defaultMinSize(minHeight = dimensions.minTouchTarget)
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.smallMedium)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(shapes.thumbnail)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnail != null) {
                        thumbnail()
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(dimensions.iconMedium)
                        )
                    }
                }
            }

            VerticalSpacer(spacing.extraSmall)

            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = videoCountText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
