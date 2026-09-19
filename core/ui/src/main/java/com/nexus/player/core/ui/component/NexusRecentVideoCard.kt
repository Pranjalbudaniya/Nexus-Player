package com.nexus.player.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.sp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Three-Line Information Presentation Card for the "Recently Added" section.
 *
 * Information Hierarchy:
 * - Line 1: Video Title (Medium-emphasis primary label)
 * - Line 2: Technical Specifications (Resolution • Codec • File Size)
 * - Line 3: Ingestion Metadata (Time Added • Source Folder)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NexusRecentVideoCard(
    title: String,
    technicalSpecs: String,
    addedTimeAndFolder: String,
    duration: String,
    quality: String,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 176.dp,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes
    val metadataTypography = NexusTheme.metadataTypography

    Column(
        modifier = modifier
            .width(cardWidth)
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
                .clip(shapes.thumbnail)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (thumbnail != null) {
                thumbnail()
            } else {
                // Media Placeholder Icon
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxSize(0.45f)
                        .align(Alignment.Center)
                )
            }

            // Quality Indicator at Top-Left
            NexusBadge(
                text = quality,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.extraSmall)
            )

            // Duration Indicator at Bottom-Right
            NexusBadge(
                text = duration,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.extraSmall)
            )
        }

        VerticalSpacer(spacing.extraSmall)

        // Line 1: Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        VerticalSpacer(spacing.extraSmall)

        // Line 2: Technical Specs (e.g., "HEVC • 4.2 GB")
        if (technicalSpecs.isNotBlank()) {
            Text(
                text = technicalSpecs,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Line 3: Date only (e.g., "Sep 19, 2026")
        if (addedTimeAndFolder.isNotBlank()) {
            Text(
                text = addedTimeAndFolder,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
