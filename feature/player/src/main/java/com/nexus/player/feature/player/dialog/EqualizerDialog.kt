package com.nexus.player.feature.player.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexus.player.core.playback.audio.EqualizerPreset
import kotlin.math.roundToInt

private fun formatFrequency(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val kHz = freqHz / 1000f
        if (kHz % 1f == 0f) "${kHz.toInt()}kHz" else "${kHz}kHz"
    } else {
        "${freqHz}Hz"
    }
}

@Composable
fun EqualizerDialog(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    currentPreset: String,
    onSelectPreset: (String) -> Unit,
    bandLevels: Map<Int, Int>,
    onBandLevelChange: (bandIndex: Int, levelmB: Int) -> Unit,
    bandFrequencies: List<Int> = listOf(60, 230, 910, 3600, 14000),
    bandLevelRange: IntRange = -1500..1500,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("equalizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header + Enable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Equalizer",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isEnabled) "Active ($currentPreset)" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier
                            .testTag("equalizer_enable_switch")
                            .semantics {
                                contentDescription = if (isEnabled) "Disable Equalizer" else "Enable Equalizer"
                            }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Presets row
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EqualizerPreset.PRESETS) { preset ->
                        val isSelected = isEnabled && currentPreset.equals(preset.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isEnabled) onToggleEnabled(true)
                                onSelectPreset(preset.name)
                            },
                            label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("equalizer_preset_${preset.name}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Band sliders
                Text(
                    text = "Frequency Bands",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                val minLevel = bandLevelRange.first.toFloat()
                val maxLevel = bandLevelRange.last.toFloat()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bandFrequencies.take(5).forEachIndexed { index, freq ->
                        val currentLevelmB = bandLevels[index] ?: 0
                        val gainDb = currentLevelmB / 100f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatFrequency(freq),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(60.dp)
                            )

                            Slider(
                                value = currentLevelmB.toFloat(),
                                onValueChange = { newmB ->
                                    onBandLevelChange(index, newmB.roundToInt())
                                },
                                valueRange = minLevel..maxLevel,
                                enabled = isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = "${formatFrequency(freq)} band gain: ${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()} dB"
                                    }
                                    .testTag("equalizer_band_$index")
                            )

                            Text(
                                text = "${if (gainDb > 0) "+" else ""}${gainDb.roundToInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .width(52.dp)
                                    .padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
