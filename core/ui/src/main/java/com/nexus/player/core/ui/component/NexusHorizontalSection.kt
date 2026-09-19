package com.nexus.player.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Reusable Horizontal Content Section Component.
 *
 * Encapsulates the section header, horizontal LazyRow, 4dp-based spacing,
 * empty state, and loading skeleton states.
 */
@Composable
fun NexusHorizontalSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    isEmpty: Boolean = false,
    emptyMessage: String = "No items available",
    content: LazyListScope.() -> Unit
) {
    val spacing = NexusTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section Header
        NexusSectionHeader(
            title = title,
            subtitle = subtitle,
            action = action
        )

        VerticalSpacer(spacing.extraSmall)

        when {
            isLoading -> {
                // Loading Skeleton Placeholder Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(3) {
                        NexusLoadingCardPlaceholder()
                    }
                }
            }
            isEmpty -> {
                // Empty State Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium)
                        .height(64.dp)
                        .clip(NexusTheme.customShapes.card)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Horizontal Content Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    content = content
                )
            }
        }
    }
}

/**
 * Placeholder card for section loading state.
 */
@Composable
private fun NexusLoadingCardPlaceholder() {
    val shapes = NexusTheme.customShapes
    Box(
        modifier = Modifier
            .width(176.dp)
            .height(136.dp)
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}
