package com.nexus.player.feature.more.analytics.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.more.analytics.model.DistributionMetric

/**
 * Visual breakdown of video resolutions and formats with segmented distribution bars.
 */
@Composable
fun DistributionSection(
    title: String,
    metrics: List<DistributionMetric>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return

    val spacing = NexusTheme.spacing

    val segmentColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.surfaceContainerHighest
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NexusTheme.customShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            VerticalSpacer(spacing.medium)

            // Segmented Distribution Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                metrics.take(5).forEachIndexed { index, metric ->
                    val color = segmentColors.getOrElse(index) { MaterialTheme.colorScheme.surfaceContainerHighest }
                    if (metric.percentage > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(metric.percentage.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(color)
                        )
                    }
                }
            }

            VerticalSpacer(spacing.medium)

            // Legend / Metric List
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                metrics.take(5).forEachIndexed { index, metric ->
                    val color = segmentColors.getOrElse(index) { MaterialTheme.colorScheme.surfaceContainerHighest }
                    MetricRowItem(metric = metric, indicatorColor = color)
                }
            }
        }
    }
}

@Composable
private fun MetricRowItem(
    metric: DistributionMetric,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(indicatorColor)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )

            HorizontalSpacer(spacing.small)

            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${metric.count} videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalSpacer(spacing.medium)

            Text(
                text = metric.formattedPercentage,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
