package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.designsystem.theme.NexusAccentColor
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Compact, accessible accent color selection control.
 * Displays interactive color swatches conforming to the Nexus 4dp spacing grid.
 *
 * ZERO hardcoded hex color values.
 */
@Composable
fun AccentColorPicker(
    selectedAccent: AccentColor,
    onAccentSelected: (AccentColor) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NexusTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Custom Accent Color",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                VerticalSpacer(NexusTheme.spacing.extraSmall)
                Text(
                    text = "Selected: ${selectedAccent.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }

        VerticalSpacer(NexusTheme.spacing.smallMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .padding(vertical = NexusTheme.spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.smallMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentColor.values().forEach { accent ->
                val isSelected = accent == selectedAccent
                AccentSwatchItem(
                    accent = accent,
                    isSelected = isSelected,
                    enabled = enabled,
                    onClick = { onAccentSelected(accent) }
                )
            }
        }
    }
}

@Composable
private fun AccentSwatchItem(
    accent: AccentColor,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchColor = try {
        NexusAccentColor.valueOf(accent.name).swatchColor
    } catch (_: IllegalArgumentException) {
        MaterialTheme.colorScheme.primary
    }

    // Touch target is minimum 48dp for full accessibility compliance
    Box(
        modifier = modifier
            .size(NexusTheme.dimensions.minTouchTarget)
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                contentDescription = "${accent.label} accent color option"
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer selection indicator ring
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(NexusTheme.dimensions.iconExtraLarge - NexusTheme.spacing.small)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }

        // Inner color swatch
        Box(
            modifier = Modifier
                .size(NexusTheme.dimensions.iconLarge)
                .clip(CircleShape)
                .background(
                    if (accent == AccentColor.DEFAULT) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        swatchColor
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (accent == AccentColor.DEFAULT) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(NexusTheme.dimensions.iconSmall)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(NexusTheme.dimensions.iconSmall)
                )
            }
        }
    }
}
