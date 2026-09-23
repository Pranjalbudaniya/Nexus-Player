package com.nexus.player.feature.more.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusErrorState
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.more.analytics.component.ClearAnalyticsDialog
import com.nexus.player.feature.more.analytics.component.DistributionSection
import com.nexus.player.feature.more.analytics.component.MostPlayedRow
import com.nexus.player.feature.more.analytics.component.StatSummaryCard
import com.nexus.player.feature.more.analytics.component.WeeklyActivityBarChart
import com.nexus.player.feature.more.analytics.model.RecentActivityItem
import com.nexus.player.feature.more.analytics.model.WatchAnalyticsData

@Composable
fun AnalyticsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        uiState = uiState,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateBack = onNavigateBack,
        onNavigateToPlayer = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onClearClick = { viewModel.setClearDialogOpen(true) },
        onConfirmClear = { viewModel.clearAllAnalytics() },
        onDismissClear = { viewModel.setClearDialogOpen(false) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    thumbnailLoader: ThumbnailLoader?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onClearClick: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NexusTopAppBar(
                title = "Playback Analytics",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (uiState is AnalyticsUiState.Success) {
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier.semantics { contentDescription = "Clear Analytics and History" }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    NexusLoadingIndicator(label = "Calculating analytics...")
                }
            }

            is AnalyticsUiState.Empty -> {
                AnalyticsEmptyState(
                    onBrowseClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(spacing.large)
                )
            }

            is AnalyticsUiState.Error -> {
                NexusErrorState(
                    message = uiState.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is AnalyticsUiState.Success -> {
                AnalyticsContent(
                    data = uiState.data,
                    thumbnailLoader = thumbnailLoader,
                    onVideoClick = onNavigateToPlayer,
                    contentPadding = innerPadding
                )

                if (uiState.isClearDialogOpen) {
                    ClearAnalyticsDialog(
                        onConfirm = onConfirmClear,
                        onDismiss = onDismissClear
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    data: WatchAnalyticsData,
    thumbnailLoader: ThumbnailLoader?,
    onVideoClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val spacing = NexusTheme.spacing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            start = spacing.medium,
            end = spacing.medium,
            top = spacing.small,
            bottom = spacing.extraLarge
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // 1. High-Level Summary Metric Cards (2x2 Grid)
        item(key = "summary_row_1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                StatSummaryCard(
                    icon = Icons.Default.Timer,
                    value = data.summary.formattedTotalWatchTime,
                    label = "Total Watch Time",
                    subLabel = "${data.summary.totalVideosPlayed} videos watched",
                    modifier = Modifier.weight(1f)
                )

                StatSummaryCard(
                    icon = Icons.Default.AccessTime,
                    value = data.summary.formattedAverageWatchDuration,
                    label = "Avg Session",
                    subLabel = "Per video playback",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "summary_row_2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                StatSummaryCard(
                    icon = Icons.Default.DoneAll,
                    value = "${data.summary.totalCompletedVideos}",
                    label = "Completed Videos",
                    subLabel = "Watched to ≥95%",
                    modifier = Modifier.weight(1f)
                )

                StatSummaryCard(
                    icon = Icons.Default.CheckCircle,
                    value = data.summary.formattedCompletionRate,
                    label = "Completion Rate",
                    subLabel = "Of all started videos",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Playback Activity (Past 7 Days Bar Chart)
        item(key = "weekly_activity") {
            WeeklyActivityBarChart(activity = data.weeklyActivity)
        }

        // 3. Most-Played Videos Section
        if (data.mostPlayed.isNotEmpty()) {
            item(key = "most_played_header") {
                Text(
                    text = "Most-Played Videos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.small)
                )
            }

            items(
                items = data.mostPlayed,
                key = { "most_played_${it.id}" }
            ) { item ->
                MostPlayedRow(
                    item = item,
                    thumbnailLoader = thumbnailLoader,
                    onClick = { onVideoClick(item.id) }
                )
            }
        }

        // 4. Video Resolutions Breakdown
        if (data.resolutions.isNotEmpty()) {
            item(key = "resolutions_breakdown") {
                DistributionSection(
                    title = "Video Resolutions",
                    metrics = data.resolutions
                )
            }
        }

        // 5. Video Formats & Codecs Breakdown
        if (data.videoFormats.isNotEmpty()) {
            item(key = "video_formats_breakdown") {
                DistributionSection(
                    title = "Video Formats & Codecs",
                    metrics = data.videoFormats
                )
            }
        }

        // 6. Audio Formats & Codecs Breakdown
        if (data.audioFormats.isNotEmpty()) {
            item(key = "audio_formats_breakdown") {
                DistributionSection(
                    title = "Audio Formats & Codecs",
                    metrics = data.audioFormats
                )
            }
        }

        // 7. Recent Activity List
        if (data.recentActivity.isNotEmpty()) {
            item(key = "recent_activity_header") {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.small)
                )
            }

            items(
                items = data.recentActivity.take(5),
                key = { "recent_${it.id}" }
            ) { item ->
                RecentActivityRow(
                    item = item,
                    onClick = { onVideoClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    item: RecentActivityItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                HorizontalSpacer(spacing.small)

                Text(
                    text = item.lastPlayedRelative,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VerticalSpacer(spacing.extraSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.folderName} • ${item.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.isCompleted) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = spacing.extraSmall, vertical = spacing.extraSmall / 2)
                        )
                    }
                } else {
                    Text(
                        text = "${(item.playbackPercentage * 100).toInt()}% watched",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!item.isCompleted && item.playbackPercentage > 0f) {
                VerticalSpacer(spacing.small)
                LinearProgressIndicator(
                    progress = { item.playbackPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
private fun AnalyticsEmptyState(
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexusEmptyState(
        icon = Icons.Default.Analytics,
        title = "No Playback Analytics Yet",
        description = "Watch videos in your local library to view your watch time, completion rates, most-watched videos, and codec distributions.",
        actionText = "Browse Library",
        onActionClick = onBrowseClick,
        modifier = modifier
    )
}
