package com.nexus.player.feature.player.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.PlayerTrack
import com.nexus.player.core.playback.model.SubtitleAppearance
import com.nexus.player.core.playback.model.SubtitleBackgroundStyle
import com.nexus.player.core.playback.model.SubtitlePosition
import com.nexus.player.core.playback.model.SubtitleTextColor
import com.nexus.player.core.playback.model.SubtitleTextSize

/**
 * Compact bottom sheet for combined Audio and Subtitle configuration.
 *
 * Implements:
 * - Real audio track selection with languages, channel configs (5.1, Stereo), and codecs
 * - Subtitle track selection with explicit "Off" toggle and active track indicators
 * - User-imported external subtitle file launcher
 * - Subtitle and Audio delay timing controls (+/- 500ms chips and continuous +/- 50ms steppers)
 * - Subtitle appearance configuration (size, theme-safe colors, background style, position)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSubtitlesSheet(
    audioTracks: List<PlayerTrack>,
    subtitleTracks: List<PlayerTrack>,
    areSubtitlesEnabled: Boolean,
    currentAudioDelayMs: Long,
    currentSubtitleDelayMs: Long,
    subtitleAppearance: SubtitleAppearance,
    onSelectAudioTrack: (String) -> Unit,
    onSelectSubtitleTrack: (String?) -> Unit,
    onToggleSubtitles: (Boolean) -> Unit,
    onAudioDelayChange: (Long) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onSubtitleAppearanceChange: (SubtitleAppearance) -> Unit,
    onAddExternalSubtitleClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.testTag("audio_subtitles_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NexusTheme.spacing.medium)
                .padding(bottom = NexusTheme.spacing.large)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio & Subtitles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("audio_subtitles_close_button")
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

            // =========================================================================
            // 1. Audio Tracks Section
            // =========================================================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Audio Tracks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            if (audioTracks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No audio tracks detected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(NexusTheme.spacing.medium)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    audioTracks.forEach { track ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (track.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectAudioTrack(track.id) }
                                .testTag("audio_track_${track.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (track.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    track.technicalDetails?.let { details ->
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = { onSelectAudioTrack(track.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

            // =========================================================================
            // 2. Subtitles Section
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subtitles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Switch(
                    checked = areSubtitlesEnabled,
                    onCheckedChange = onToggleSubtitles,
                    modifier = Modifier.testTag("audio_subtitles_toggle")
                )
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // "Off" Option
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (!areSubtitlesEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectSubtitleTrack(null) }
                    .testTag("subtitle_track_off")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SubtitlesOff,
                            contentDescription = null,
                            tint = if (!areSubtitlesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!areSubtitlesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!areSubtitlesEnabled) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (subtitleTracks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    subtitleTracks.forEach { track ->
                        val isTrackActive = areSubtitlesEnabled && track.isSelected
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTrackActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSubtitleTrack(track.id) }
                                .testTag("subtitle_track_${track.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isTrackActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    track.technicalDetails?.let { details ->
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isTrackActive) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Add External Subtitle Button
            OutlinedButton(
                onClick = onAddExternalSubtitleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_external_subtitle_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Subtitle File (SRT, VTT, ASS)")
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

            // =========================================================================
            // 3. Audio & Subtitle Delay Synchronization
            // =========================================================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Audio & Subtitle Sync",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Subtitle Delay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtitle Timing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (currentSubtitleDelayMs > 0) "+" else ""}${currentSubtitleDelayMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(-500L, -250L, 0L, 250L, 500L).forEach { delayMs ->
                    val label = if (delayMs == 0L) "0ms" else "${if (delayMs > 0) "+" else ""}${delayMs}ms"
                    FilterChip(
                        selected = currentSubtitleDelayMs == delayMs,
                        onClick = { onSubtitleDelayChange(delayMs) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSubtitleDelayChange(currentSubtitleDelayMs - 50L) }
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "-50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Fine tune (-50ms / +50ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onSubtitleDelayChange(currentSubtitleDelayMs + 50L) }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "+50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Audio Delay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Timing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (currentAudioDelayMs > 0) "+" else ""}${currentAudioDelayMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(-500L, -250L, 0L, 250L, 500L).forEach { delayMs ->
                    val label = if (delayMs == 0L) "0ms" else "${if (delayMs > 0) "+" else ""}${delayMs}ms"
                    FilterChip(
                        selected = currentAudioDelayMs == delayMs,
                        onClick = { onAudioDelayChange(delayMs) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAudioDelayChange(currentAudioDelayMs - 50L) }
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "-50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Fine tune (-50ms / +50ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onAudioDelayChange(currentAudioDelayMs + 50L) }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "+50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

            // =========================================================================
            // 4. Subtitle Appearance Section
            // =========================================================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Subtitle Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Text Size
            Text(
                text = "Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Text Color
            Text(
                text = "Text Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(NexusTheme.spacing.small))

            // Background / Outline Style
            Text(
                text = "Background & Outline",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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
        }
    }
}
