package com.nexus.player.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Reusable Card foundation for Nexus Player.
 *
 * Implements standard semantic surface container coloring, shapes, and 4dp padding.
 */
@Composable
fun NexusCard(
    modifier: Modifier = Modifier,
    shape: Shape = NexusTheme.customShapes.card,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = NexusTheme.dimensions.elevationLow
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = NexusTheme.dimensions.cardMinHeight),
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Interactive Clickable Nexus Card with accessibility-compliant touch targets.
 */
@Composable
fun NexusClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = NexusTheme.customShapes.card,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = NexusTheme.dimensions.elevationLow
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minHeight = NexusTheme.dimensions.minTouchTarget
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}
