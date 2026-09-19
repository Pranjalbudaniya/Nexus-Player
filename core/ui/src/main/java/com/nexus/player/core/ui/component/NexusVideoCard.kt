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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Reusable Video Card Foundation for Nexus Player.
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
 * Optional Progress / Subtitle
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NexusVideoCard(
    title: String,
    duration: String,
    quality: String,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 156.dp,
    progress: Float? = null,
    subtitle: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

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
                // Centered Media Placeholder Icon
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

            // Playback Progress Indicator (Optional, e.g. for Continue Watching)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f)
                )
            }
        }

        VerticalSpacer(spacing.extraSmall)

        // Video Title Below the Card
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Optional Subtitle
        if (subtitle != null) {
            VerticalSpacer(spacing.extraSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
