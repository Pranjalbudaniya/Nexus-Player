package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleSettings
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Dedicated Subtitles configuration card for the Settings screen.
 *
 * Implements:
 * - Live interactive preview of subtitle styling
 * - Subtitle enable toggle & language preference
 * - Default track selection behavior
 * - Size, text color, background style, background opacity, and vertical position controls
 * - Default subtitle sync delay adjustment
 * - ZERO hardcoded hex colors; 100% theme-aware tokens.
 */
@Composable
fun SubtitleSettingsCard(
    settings: SubtitleSettings,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSetSubtitlesEnabled: (Boolean) -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onOpenTrackBehaviorDialog: () -> Unit,
    onSetSubtitleTextSize: (SubtitleTextSize) -> Unit,
    onSetSubtitleTextColor: (SubtitleTextColor) -> Unit,
    onSetSubtitleBackgroundStyle: (SubtitleBackgroundStyle) -> Unit,
    onSetSubtitleBackgroundOpacity: (Float) -> Unit,
    onSetSubtitlePosition: (SubtitlePosition) -> Unit,
    onSetSubtitleDelayMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSectionCard(
        title = "Subtitles",
        subtitle = "Appearance, text styling, and track behavior",
        icon = Icons.Default.Subtitles,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier.testTag("setting_card_subtitles")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = NexusTheme.spacing.small)
        ) {
            // 1. Live Subtitle Preview Box
            SubtitlePreviewBox(
                textSize = settings.textSize,
                textColor = settings.textColor,
                backgroundStyle = settings.backgroundStyle,
                backgroundOpacity = settings.backgroundOpacity,
                position = settings.position,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NexusTheme.spacing.small)
            )

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.small)

            // 2. Enable / Disable toggle
            SettingSwitchRow(
                title = "Enable Subtitles by Default",
                checked = settings.areSubtitlesEnabled,
                onCheckedChange = onSetSubtitlesEnabled,
                description = "Automatically load and display subtitles when available"
            )

            // 3. Preferred Subtitle Language
            SettingSelectRow(
                title = "Preferred Subtitle Language",
                currentValue = settings.preferredSubtitleLanguage,
                onClick = onOpenLanguageDialog,
                description = "Primary language to select automatically"
            )

            // 4. Default Track Selection Behavior
            SettingSelectRow(
                title = "Default Track Behavior",
                currentValue = settings.defaultTrackBehavior.label,
                onClick = onOpenTrackBehaviorDialog,
                description = "Track selection fallback rule"
            )

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.smallMedium)

            // 5. Appearance Header
            Text(
                text = "Appearance & Styling",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = NexusTheme.spacing.extraSmall)
            )

            // Subtitle Size
            Text(
                text = "Size",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(NexusTheme.spacing.extraSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
            ) {
                SubtitleTextSize.entries.forEach { size ->
                    val isSelected = settings.textSize == size
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetSubtitleTextSize(size) },
                        label = { Text(size.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_sub_size_${size.name}")
                    )
                }
            }

            VerticalSpacer(NexusTheme.spacing.smallMedium)

            // Text Color
            Text(
                text = "Text Color",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(NexusTheme.spacing.extraSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
            ) {
                SubtitleTextColor.entries.forEach { color ->
                    val isSelected = settings.textColor == color
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetSubtitleTextColor(color) },
                        label = { Text(color.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_sub_color_${color.name}")
                    )
                }
            }

            VerticalSpacer(NexusTheme.spacing.smallMedium)

            // Background Style
            Text(
                text = "Background Style",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(NexusTheme.spacing.extraSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
            ) {
                SubtitleBackgroundStyle.entries.forEach { style ->
                    val isSelected = settings.backgroundStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetSubtitleBackgroundStyle(style) },
                        label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_sub_bg_${style.name}")
                    )
                }
            }

            // Background Opacity Slider (visible when Box style)
            if (settings.backgroundStyle == SubtitleBackgroundStyle.Box) {
                VerticalSpacer(NexusTheme.spacing.smallMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Background Opacity",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(settings.backgroundOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = settings.backgroundOpacity,
                    onValueChange = onSetSubtitleBackgroundOpacity,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_opacity_slider")
                        .semantics {
                            contentDescription = "Background opacity: ${(settings.backgroundOpacity * 100).toInt()}%"
                        }
                )
            }

            VerticalSpacer(NexusTheme.spacing.smallMedium)

            // Vertical Position
            Text(
                text = "Vertical Position",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(NexusTheme.spacing.extraSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
            ) {
                SubtitlePosition.entries.forEach { position ->
                    val isSelected = settings.position == position
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetSubtitlePosition(position) },
                        label = { Text(position.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_sub_pos_${position.name}")
                    )
                }
            }

            VerticalSpacer(NexusTheme.spacing.smallMedium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.smallMedium)

            // 6. Subtitle Delay (Default Sync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subtitle Delay (Default Sync)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Global default offset for subtitle timing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (settings.subtitleDelayMs > 0) "+" else ""}${settings.subtitleDelayMs} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VerticalSpacer(NexusTheme.spacing.extraSmall)

            // Quick delay preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(-500L, -250L, 0L, 250L, 500L).forEach { delayMs ->
                    val label = if (delayMs == 0L) "0ms" else "${if (delayMs > 0) "+" else ""}${delayMs}ms"
                    FilterChip(
                        selected = settings.subtitleDelayMs == delayMs,
                        onClick = { onSetSubtitleDelayMs(delayMs) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_sub_delay_$delayMs")
                    )
                }
            }

            // Fine-tune buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSetSubtitleDelayMs(settings.subtitleDelayMs - 50L) },
                    modifier = Modifier.testTag("sub_delay_decrease")
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "-50ms",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Fine tune (-50ms / +50ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onSetSubtitleDelayMs(settings.subtitleDelayMs + 50L) },
                    modifier = Modifier.testTag("sub_delay_increase")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "+50ms",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
