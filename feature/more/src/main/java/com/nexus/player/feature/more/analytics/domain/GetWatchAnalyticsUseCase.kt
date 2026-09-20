package com.nexus.player.feature.more.analytics.domain

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.feature.more.analytics.model.DailyPlaybackActivity
import com.nexus.player.feature.more.analytics.model.DistributionMetric
import com.nexus.player.feature.more.analytics.model.MostPlayedVideo
import com.nexus.player.feature.more.analytics.model.RecentActivityItem
import com.nexus.player.feature.more.analytics.model.WatchAnalyticsData
import com.nexus.player.feature.more.analytics.model.WatchSummaryStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain UseCase that calculates comprehensive watch metrics, activity distributions,
 * and media format trends from the local playback history database records.
 *
 * All operations are purely local and calculated asynchronously on [NexusDispatchers.Default].
 */
@Singleton
class GetWatchAnalyticsUseCase @Inject constructor(
    @Dispatcher(NexusDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        videos: List<Video>,
        nowMillis: Long = System.currentTimeMillis()
    ): WatchAnalyticsData = withContext(defaultDispatcher) {
        val playedVideos = videos.filter { isPlayedVideo(it) }

        if (playedVideos.isEmpty()) {
            return@withContext WatchAnalyticsData(
                summary = WatchSummaryStats(),
                weeklyActivity = buildEmptyWeeklyActivity(nowMillis),
                mostPlayed = emptyList(),
                recentActivity = emptyList(),
                resolutions = emptyList(),
                videoFormats = emptyList(),
                audioFormats = emptyList()
            )
        }

        // 1. Core Summary Metrics
        val totalPlayed = playedVideos.size
        val totalCompleted = playedVideos.count { it.isCompleted || it.playbackPercentage >= 0.949f }
        val completionRate = if (totalPlayed > 0) totalCompleted.toFloat() / totalPlayed else 0f

        val totalWatchTimeMs = playedVideos.sumOf { computeVideoWatchTime(it) }
        val averageWatchDurationMs = if (totalPlayed > 0) totalWatchTimeMs / totalPlayed else 0L

        val summary = WatchSummaryStats(
            totalVideosPlayed = totalPlayed,
            totalCompletedVideos = totalCompleted,
            completionRate = completionRate,
            formattedCompletionRate = "${(completionRate * 100).toInt()}%",
            totalWatchTimeMs = totalWatchTimeMs,
            formattedTotalWatchTime = formatWatchTime(totalWatchTimeMs),
            averageWatchDurationMs = averageWatchDurationMs,
            formattedAverageWatchDuration = formatWatchTime(averageWatchDurationMs)
        )

        // 2. Playback Activity by Day (Rolling 7-Day Window)
        val weeklyActivity = calculateWeeklyActivity(playedVideos, nowMillis)

        // 3. Most-Played Videos
        val mostPlayed = playedVideos
            .sortedWith(
                compareByDescending<Video> { it.watchCount }
                    .thenByDescending { it.playbackPercentage }
                    .thenByDescending { it.lastPlayedAt ?: 0L }
            )
            .take(10)
            .mapIndexed { index, video ->
                MostPlayedVideo(
                    rank = index + 1,
                    id = video.id,
                    title = video.title,
                    folderName = video.folderName,
                    mediaUri = video.mediaUri,
                    watchCount = maxOf(1, video.watchCount),
                    durationMs = video.durationMs,
                    formattedDuration = formatDuration(video.durationMs),
                    resolutionLabel = video.resolutionLabel.ifBlank { "Unknown" },
                    videoCodec = video.videoCodec,
                    isCompleted = video.isCompleted || video.playbackPercentage >= 0.949f,
                    playbackPercentage = video.playbackPercentage
                )
            }

        // 4. Recently Played Videos
        val recentActivity = playedVideos
            .filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt }
            .take(10)
            .map { video ->
                RecentActivityItem(
                    id = video.id,
                    title = video.title,
                    folderName = video.folderName,
                    mediaUri = video.mediaUri,
                    lastPlayedAt = video.lastPlayedAt ?: 0L,
                    lastPlayedRelative = formatRelativeTime(video.lastPlayedAt ?: 0L, nowMillis),
                    playbackPercentage = video.playbackPercentage,
                    isCompleted = video.isCompleted || video.playbackPercentage >= 0.949f,
                    durationMs = video.durationMs,
                    formattedDuration = formatDuration(video.durationMs)
                )
            }

        // 5. Resolution Distribution
        val resolutions = calculateDistribution(
            items = playedVideos,
            labelExtractor = { it.resolutionLabel.ifBlank { "Other" } }
        )

        // 6. Video Formats / Codecs Distribution
        val videoFormats = calculateDistribution(
            items = playedVideos,
            labelExtractor = {
                it.videoCodec?.takeIf { c -> c.isNotBlank() }
                    ?: it.fileName.substringAfterLast('.', "").uppercase().ifBlank { "Other" }
            }
        )

        // 7. Audio Formats Distribution
        val audioFormats = calculateDistribution(
            items = playedVideos,
            labelExtractor = { it.audioCodec?.takeIf { c -> c.isNotBlank() } ?: "AAC" }
        )

        WatchAnalyticsData(
            summary = summary,
            weeklyActivity = weeklyActivity,
            mostPlayed = mostPlayed,
            recentActivity = recentActivity,
            resolutions = resolutions,
            videoFormats = videoFormats,
            audioFormats = audioFormats
        )
    }

    private fun isPlayedVideo(video: Video): Boolean {
        return video.lastPlayedAt != null || video.watchCount > 0 || video.playbackPositionMs > 0L
    }

    /**
     * Accurately computes watch time for a single video, handling:
     * - In-progress playback (`playbackPositionMs`)
     * - Multi-completion counts (`watchCount * durationMs`)
     * - Unknown or 0 duration fallbacks
     */
    internal fun computeVideoWatchTime(video: Video): Long {
        val effectiveDuration = if (video.durationMs > 0L) {
            video.durationMs
        } else {
            maxOf(0L, video.playbackPositionMs)
        }

        val completedCount = if (video.isCompleted || video.playbackPercentage >= 0.949f) {
            maxOf(1, video.watchCount)
        } else {
            maxOf(0, video.watchCount - 1)
        }

        val inProgressMs = if (!video.isCompleted && video.playbackPercentage < 0.949f) {
            video.playbackPositionMs.coerceIn(0L, if (effectiveDuration > 0L) effectiveDuration else Long.MAX_VALUE)
        } else {
            0L
        }

        return (completedCount * effectiveDuration) + inProgressMs
    }

    private fun calculateWeeklyActivity(
        playedVideos: List<Video>,
        nowMillis: Long
    ): List<DailyPlaybackActivity> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val dayBuckets = mutableListOf<DailyPlaybackActivity>()

        // 7 days ending with Today (offset 6 down to 0)
        for (offset in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)

            val isToday = offset == 0
            val dayLabel = if (isToday) "Today" else if (offset == 1) "Yest" else dayFormat.format(Date(startOfDay))
            val dateLabel = dateFormat.format(Date(startOfDay))

            val dayVideos = playedVideos.filter { v ->
                val playedAt = v.lastPlayedAt ?: 0L
                playedAt in startOfDay..<endOfDay
            }

            val dayCount = dayVideos.size
            val dayWatchTime = dayVideos.sumOf { computeVideoWatchTime(it) }

            dayBuckets.add(
                DailyPlaybackActivity(
                    dayLabel = dayLabel,
                    dateLabel = dateLabel,
                    dateMillis = startOfDay,
                    videosPlayedCount = dayCount,
                    watchTimeMs = dayWatchTime,
                    formattedWatchTime = formatWatchTime(dayWatchTime),
                    relativeIntensity = 0f, // will normalize next
                    isToday = isToday
                )
            )
        }

        val maxWatchTime = dayBuckets.maxOfOrNull { it.watchTimeMs } ?: 0L
        val maxCount = dayBuckets.maxOfOrNull { it.videosPlayedCount } ?: 0

        return dayBuckets.map { item ->
            val ratio = if (maxWatchTime > 0L) {
                (item.watchTimeMs.toFloat() / maxWatchTime).coerceIn(0f, 1f)
            } else if (maxCount > 0) {
                (item.videosPlayedCount.toFloat() / maxCount).coerceIn(0f, 1f)
            } else {
                0f
            }
            val normalizedRatio = if (ratio > 0f) maxOf(0.12f, ratio) else 0f
            item.copy(relativeIntensity = normalizedRatio)
        }
    }

    private fun buildEmptyWeeklyActivity(nowMillis: Long): List<DailyPlaybackActivity> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        return (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val isToday = offset == 0
            val dayLabel = if (isToday) "Today" else if (offset == 1) "Yest" else dayFormat.format(Date(startOfDay))
            val dateLabel = dateFormat.format(Date(startOfDay))

            DailyPlaybackActivity(
                dayLabel = dayLabel,
                dateLabel = dateLabel,
                dateMillis = startOfDay,
                videosPlayedCount = 0,
                watchTimeMs = 0L,
                formattedWatchTime = "0m",
                relativeIntensity = 0f,
                isToday = isToday
            )
        }
    }

    private fun calculateDistribution(
        items: List<Video>,
        labelExtractor: (Video) -> String
    ): List<DistributionMetric> {
        val total = items.size
        if (total == 0) return emptyList()

        return items
            .groupBy(labelExtractor)
            .map { (label, group) ->
                val count = group.size
                val percentage = count.toFloat() / total
                DistributionMetric(
                    label = label,
                    count = count,
                    percentage = percentage,
                    formattedPercentage = "${(percentage * 100).toInt()}%"
                )
            }
            .sortedByDescending { it.count }
    }

    internal fun formatWatchTime(ms: Long): String {
        if (ms <= 0L) return "0m"
        val totalMinutes = ms / (60 * 1000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val seconds = (ms % 60000L) / 1000L

        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    internal fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L) return "Never"
        val diff = maxOf(0L, now - timestamp)
        val minutes = diff / (60 * 1000L)
        val hours = diff / (60 * 60 * 1000L)
        val days = diff / (24 * 60 * 60 * 1000L)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
