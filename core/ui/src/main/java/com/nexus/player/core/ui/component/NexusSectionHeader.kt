package com.nexus.player.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Standard Section Header for Nexus Player content sections (e.g. "Recently Played", "Folders", "Audio Tracks").
 */
@Composable
fun NexusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val spacing = NexusTheme.spacing
    val dimensions = NexusTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            verticalArrangement = Arrangement.spacedBy(spacing.none)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        if (action != null) {
            Box(
                modifier = Modifier.defaultMinSize(
                    minWidth = dimensions.minTouchTarget,
                    minHeight = dimensions.minTouchTarget
                ),
                contentAlignment = Alignment.CenterEnd
            ) {
                action()
            }
        }
    }
}
