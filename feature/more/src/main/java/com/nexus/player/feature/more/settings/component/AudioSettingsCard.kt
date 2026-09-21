package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.common.settings.model.AudioSettings
import com.nexus.player.core.common.settings.model.EqualizerPreset
import com.nexus.player.core.common.settings.model.formatAudioFrequency
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.VerticalSpacer
import kotlin.math.roundToInt

private val AUDIO_BOOST_CHIPS = listOf(100, 110, 125, 150, 175, 200)
private val AUDIO_DELAY_CHIPS = listOf(-500L, -250L, 0L, 250L, 500L)

/**
 * Dedicated Audio and Equalizer configuration card for the Settings screen.
 *
 * Implements:
 * - Audio boost adjustment (100% to 200%) with status badge and quick presets
 * - Equalizer master toggle with preset selector
 * - Interactive compact frequency sliders for hardware bands (60Hz to 14kHz)
 * - Preferred audio stream language selection
 * - Audio sync delay adjustment with steppers and chips
 * - Per-video audio preference memory toggle
 * - ZERO hardcoded hex colors; 100% theme-aware tokens.
 */
@Composable
fun AudioSettingsCard(
    settings: AudioSettings,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerPreset: (String) -> Unit,
    onSetCustomBandLevel: (bandIndex: Int, levelmB: Int) -> Unit,
    onResetCustomBandLevels: () -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onSetAudioDelayMs: (Long) -> Unit,
    onSetRememberPerVideoAudioSettings: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSectionCard(
        title = "Audio",
        subtitle = "Volume boost, hardware equalizer, and sync",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier.testTag("setting_card_audio")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = NexusTheme.spacing.small)
        ) {
            // =========================================================================
            // 1. Audio Boost Section
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audio Boost",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Amplify dialog and quiet audio up to 200%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (settings.audioBoostPercent > 100) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.testTag("audio_boost_status_badge")
                ) {
                    val gainDb = ((settings.audioBoostPercent - 100) / 100f * 10).roundToInt()
                    Text(
                        text = if (settings.audioBoostPercent > 100) {
                            "${settings.audioBoostPercent}% (+$gainDb dB)"
                        } else {
                            "100% (Normal)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (settings.audioBoostPercent > 100) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            VerticalSpacer(NexusTheme.spacing.small)

            Slider(
                value = settings.audioBoostPercent.toFloat(),
                onValueChange = { onSetAudioBoostPercent(it.roundToInt()) },
                valueRange = 100f..200f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Audio boost: ${settings.audioBoostPercent}%"
                    }
                    .testTag("audio_boost_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.extraSmall)
            ) {
                AUDIO_BOOST_CHIPS.forEach { boost ->
                    FilterChip(
                        selected = settings.audioBoostPercent == boost,
                        onClick = { onSetAudioBoostPercent(boost) },
                        label = { Text("${boost}%", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_audio_boost_${boost}")
                    )
                }
            }

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.small)

            // =========================================================================
            // 2. Hardware Equalizer Section
            // =========================================================================
            SettingSwitchRow(
                title = "Hardware Equalizer",
                checked = settings.isEqualizerEnabled,
                onCheckedChange = onSetEqualizerEnabled,
                description = if (settings.isEqualizerEnabled) {
                    "Active: ${settings.equalizerPreset}"
                } else {
                    "Shape frequency response using hardware DSP"
                }
            )

            if (settings.isEqualizerEnabled) {
                VerticalSpacer(NexusTheme.spacing.small)

                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                VerticalSpacer(4.dp)

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
                ) {
                    items(EqualizerPreset.PRESETS) { preset ->
                        val isSelected = settings.equalizerPreset.equals(preset.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetEqualizerPreset(preset.name) },
                            label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("equalizer_preset_${preset.name}")
                        )
                    }
                }

                VerticalSpacer(NexusTheme.spacing.small)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frequency Bands",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = onResetCustomBandLevels,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("button_reset_equalizer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Flat", style = MaterialTheme.typography.labelSmall)
                    }
                }

                VerticalSpacer(4.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EqualizerPreset.STANDARD_FREQUENCIES.forEachIndexed { index, freqHz ->
                        val currentLevelmB = settings.customBandLevels[index] ?: 0
                        val gainDb = currentLevelmB / 100f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatAudioFrequency(freqHz),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(52.dp)
                            )

                            Slider(
                                value = currentLevelmB.toFloat(),
                                onValueChange = { newmB ->
                                    onSetCustomBandLevel(index, newmB.roundToInt())
                                },
                                valueRange = EqualizerPreset.MIN_BAND_LEVEL_MB.toFloat()..EqualizerPreset.MAX_BAND_LEVEL_MB.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = "${formatAudioFrequency(freqHz)} gain: ${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()} dB"
                                    }
                                    .testTag("setting_equalizer_band_$index")
                            )

                            Text(
                                text = "${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(46.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.small)

            // =========================================================================
            // 3. Preferred Audio Stream Language
            // =========================================================================
            SettingSelectRow(
                title = "Preferred Audio Language",
                currentValue = settings.preferredAudioLanguage,
                onClick = onOpenLanguageDialog,
                description = "Default language track when multiple audio streams exist"
            )

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.small)

            // =========================================================================
            // 4. Global Audio Sync Delay
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Default Audio Delay (Sync)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Offset audio relative to video for Bluetooth latency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (settings.audioDelayMs > 0) "+" else ""}${settings.audioDelayMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("audio_delay_readout")
                )
            }

            VerticalSpacer(NexusTheme.spacing.small)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AUDIO_DELAY_CHIPS.forEach { delayMs ->
                    val label = if (delayMs == 0L) "0ms" else "${if (delayMs > 0) "+" else ""}${delayMs}ms"
                    FilterChip(
                        selected = settings.audioDelayMs == delayMs,
                        onClick = { onSetAudioDelayMs(delayMs) },
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
                    onClick = { onSetAudioDelayMs(settings.audioDelayMs - 50L) },
                    modifier = Modifier.testTag("audio_delay_minus_50")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "-50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Fine-tune (±50ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onSetAudioDelayMs(settings.audioDelayMs + 50L) },
                    modifier = Modifier.testTag("audio_delay_plus_50")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "+50ms", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            VerticalSpacer(NexusTheme.spacing.small)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            VerticalSpacer(NexusTheme.spacing.small)

            // =========================================================================
            // 5. Per-Video Audio Settings Memory
            // =========================================================================
            SettingSwitchRow(
                title = "Remember Per-Video Audio Settings",
                checked = settings.rememberPerVideoAudioSettings,
                onCheckedChange = onSetRememberPerVideoAudioSettings,
                description = "Save audio sync delay individually for each video file"
            )
        }
    }
}
