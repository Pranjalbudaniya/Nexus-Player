package com.nexus.player.feature.player.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

private val SLEEP_TIMER_PRESETS = listOf(
    5 to "5 min",
    10 to "10 min",
    15 to "15 min",
    30 to "30 min",
    45 to "45 min",
    60 to "60 min"
)

private fun formatRemainingTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins >= 60) {
        val hours = mins / 60
        val remMins = mins % 60
        String.format("%d:%02d:%02d", hours, remMins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    remainingSeconds: Long?,
    onSetTimerMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var customMinutes by remember { mutableFloatStateOf(20f) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_sleep_timer")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (remainingSeconds != null && remainingSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = formatRemainingTime(remainingSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turn Off chip
                    FilterChip(
                        selected = remainingSeconds == null,
                        onClick = {
                            onCancelTimer()
                            onDismissRequest()
                        },
                        label = { Text("Off") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("chip_sleep_timer_off")
                    )

                    SLEEP_TIMER_PRESETS.forEach { (mins, label) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onSetTimerMinutes(mins)
                                onDismissRequest()
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("chip_sleep_timer_$mins")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${customMinutes.roundToInt()} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_sleep_timer_custom")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSetTimerMinutes(customMinutes.roundToInt())
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("button_set_sleep_timer")
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
