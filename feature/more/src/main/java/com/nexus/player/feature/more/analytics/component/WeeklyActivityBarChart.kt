package com.nexus.player.feature.more.analytics.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.more.analytics.model.DailyPlaybackActivity

/**
 * Lightweight, accessible 7-day watch activity bar visualization.
 * Uses Material 3 semantic theme tokens and works seamlessly across light and dark themes.
 */
@Composable
fun WeeklyActivityBarChart(
    activity: List<DailyPlaybackActivity>,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Past 7 Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VerticalSpacer(spacing.medium)

            // Bar Chart Columns Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                activity.forEach { day ->
                    DayBarColumn(
                        day = day,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBarColumn(
    day: DailyPlaybackActivity,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    val animatedHeightFraction by animateFloatAsState(
        targetValue = day.relativeIntensity.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "BarHeightAnimation"
    )

    val barColor = if (day.isToday) {
        MaterialTheme.colorScheme.primary
    } else if (day.watchTimeMs > 0L) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val trackColor = if (day.isToday) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .semantics {
                contentDescription = "${day.dayLabel}, ${day.dateLabel}: " +
                        if (day.watchTimeMs > 0L) "${day.formattedWatchTime} watched, ${day.videosPlayedCount} videos"
                        else "No playback activity"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Daily Metric Value
        Text(
            text = if (day.watchTimeMs > 0L) day.formattedWatchTime else "",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        VerticalSpacer(spacing.extraSmall)

        // Bar Track & Fill
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(trackColor),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (animatedHeightFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = animatedHeightFraction)
                        .clip(RoundedCornerShape(8.dp))
                        .background(barColor)
                )
            }
        }

        VerticalSpacer(spacing.extraSmall)

        // Day Label ("Mon", "Today", etc.)
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Date Label ("Sep 20")
        Text(
            text = day.dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}
