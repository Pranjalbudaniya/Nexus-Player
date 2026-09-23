package com.nexus.player.feature.player.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.SubtitleAppearance

/**
 * Responsive subtitle customization drawer / sheet.
 *
 * In Landscape:
 * - Left side: Live video preview with subtitle styling
 * - Right side: Interactive subtitle appearance controls
 *
 * In Portrait:
 * - Top side: Live video preview with subtitle styling
 * - Bottom side: Interactive subtitle appearance controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleCustomizationDrawer(
    subtitleAppearance: SubtitleAppearance,
    onSubtitleAppearanceChange: (SubtitleAppearance) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.testTag("subtitle_customization_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NexusTheme.spacing.medium)
                .padding(bottom = NexusTheme.spacing.large)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Customize Subtitles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("subtitle_drawer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

            if (isLandscape) {
                // Responsive Landscape: Left = Live Preview, Right = Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SubtitlePreviewBox(appearance = subtitleAppearance)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SubtitleControlsContent(
                            subtitleAppearance = subtitleAppearance,
                            onSubtitleAppearanceChange = onSubtitleAppearanceChange
                        )
                    }
                }
            } else {
                // Portrait: Top = Live Preview, Bottom = Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.medium)
                ) {
                    Column {
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SubtitlePreviewBox(appearance = subtitleAppearance)
                    }

                    SubtitleControlsContent(
                        subtitleAppearance = subtitleAppearance,
                        onSubtitleAppearanceChange = onSubtitleAppearanceChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleControlsContent(
    subtitleAppearance: SubtitleAppearance,
    onSubtitleAppearanceChange: (SubtitleAppearance) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
    ) {
        // 1. Text Size
        Text(
            text = "Size",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            SubtitleTextSize.entries.forEach { size ->
                FilterChip(
                    selected = subtitleAppearance.textSize == size,
                    onClick = { onSubtitleAppearanceChange(subtitleAppearance.copy(textSize = size)) },
                    label = { Text(size.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Text Color
        Text(
            text = "Text Color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            SubtitleTextColor.entries.forEach { color ->
                FilterChip(
                    selected = subtitleAppearance.textColor == color,
                    onClick = { onSubtitleAppearanceChange(subtitleAppearance.copy(textColor = color)) },
                    label = { Text(color.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Background & Outline
        Text(
            text = "Background & Outline",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            SubtitleBackgroundStyle.entries.forEach { style ->
                FilterChip(
                    selected = subtitleAppearance.backgroundStyle == style,
                    onClick = { onSubtitleAppearanceChange(subtitleAppearance.copy(backgroundStyle = style)) },
                    label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Background Opacity Slider (if Box)
        if (subtitleAppearance.backgroundStyle == SubtitleBackgroundStyle.Box) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Opacity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(subtitleAppearance.backgroundOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = subtitleAppearance.backgroundOpacity,
                onValueChange = { onSubtitleAppearanceChange(subtitleAppearance.copy(backgroundOpacity = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth().testTag("subtitles_drawer_opacity_slider")
            )
        }

        // 4. Subtitle Position
        Text(
            text = "Screen Position",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            SubtitlePosition.entries.forEach { pos ->
                FilterChip(
                    selected = subtitleAppearance.position == pos,
                    onClick = { onSubtitleAppearanceChange(subtitleAppearance.copy(position = pos)) },
                    label = { Text(pos.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
