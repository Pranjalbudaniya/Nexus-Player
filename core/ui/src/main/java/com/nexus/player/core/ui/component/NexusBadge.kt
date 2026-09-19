package com.nexus.player.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Reusable Metadata Tag / Badge for media properties (e.g., "4K", "HDR", "1080p", "MKV").
 *
 * Strictly resolves background and foreground tones from Material 3 semantic roles.
 */
@Composable
fun NexusBadge(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = NexusTheme.customShapes.badge,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val spacing = NexusTheme.spacing

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NexusTheme.metadataTypography.metadataSmall,
            color = contentColor
        )
    }
}
