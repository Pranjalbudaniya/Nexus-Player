package com.nexus.player.feature.more.analytics.model

/**
 * High-level summary metrics for video playback.
 */
data class WatchSummaryStats(
    val totalVideosPlayed: Int = 0,
    val totalCompletedVideos: Int = 0,
    val completionRate: Float = 0.0f,
    val formattedCompletionRate: String = "0%",
    val totalWatchTimeMs: Long = 0L,
    val formattedTotalWatchTime: String = "0m",
    val averageWatchDurationMs: Long = 0L,
    val formattedAverageWatchDuration: String = "0m"
)

/**
 * Daily playback metrics for visual activity charts (e.g. 7-day rolling window).
 */
data class DailyPlaybackActivity(
    val dayLabel: String,
    val dateLabel: String,
    val dateMillis: Long,
    val videosPlayedCount: Int,
    val watchTimeMs: Long,
    val formattedWatchTime: String,
    val relativeIntensity: Float,
    val isToday: Boolean
)

/**
 * Metric for most frequently played videos with ranking and playback details.
 */
data class MostPlayedVideo(
    val rank: Int,
    val id: String,
    val title: String,
    val folderName: String,
    val mediaUri: String,
    val watchCount: Int,
    val durationMs: Long,
    val formattedDuration: String,
    val resolutionLabel: String,
    val videoCodec: String?,
    val isCompleted: Boolean,
    val playbackPercentage: Float
)

/**
 * Chronological recent playback entry.
 */
data class RecentActivityItem(
    val id: String,
    val title: String,
    val folderName: String,
    val mediaUri: String,
    val lastPlayedAt: Long,
    val lastPlayedRelative: String,
    val playbackPercentage: Float,
    val isCompleted: Boolean,
    val durationMs: Long,
    val formattedDuration: String
)

/**
 * Categorical breakdown metric (e.g. resolution, video codec, audio codec).
 */
data class DistributionMetric(
    val label: String,
    val count: Int,
    val percentage: Float,
    val formattedPercentage: String
)

/**
 * Complete consolidated analytics payload.
 */
data class WatchAnalyticsData(
    val summary: WatchSummaryStats = WatchSummaryStats(),
    val weeklyActivity: List<DailyPlaybackActivity> = emptyList(),
    val mostPlayed: List<MostPlayedVideo> = emptyList(),
    val recentActivity: List<RecentActivityItem> = emptyList(),
    val resolutions: List<DistributionMetric> = emptyList(),
    val videoFormats: List<DistributionMetric> = emptyList(),
    val audioFormats: List<DistributionMetric> = emptyList()
) {
    val hasHistory: Boolean get() = summary.totalVideosPlayed > 0
}
