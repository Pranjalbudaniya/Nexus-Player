package com.nexus.player.feature.more.analytics.domain

import com.nexus.player.core.database.model.Video
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class GetWatchAnalyticsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: GetWatchAnalyticsUseCase

    @Before
    fun setUp() {
        useCase = GetWatchAnalyticsUseCase(defaultDispatcher = testDispatcher)
    }

    private fun sampleVideo(
        id: String,
        title: String,
        durationMs: Long = 100_000L,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0f,
        isCompleted: Boolean = false,
        watchCount: Int = 0,
        lastPlayedAt: Long? = 1000L,
        resolutionLabel: String = "1080p",
        videoCodec: String? = "H.264",
        audioCodec: String? = "AAC",
        fileName: String = "$title.mp4"
    ) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/Movies/$fileName",
        fileName = fileName,
        title = title,
        folderName = "Movies",
        folderPath = "/storage/Movies",
        sizeBytes = 1_000_000L,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = resolutionLabel,
        videoCodec = videoCodec,
        audioCodec = audioCodec,
        dateAdded = 1000L,
        lastModified = 1000L,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = false,
        isCompleted = isCompleted,
        watchCount = watchCount
    )

    @Test
    fun emptyHistory_returnsEmptyAnalytics() = runTest(testDispatcher) {
        val result = useCase(emptyList())

        assertFalse(result.hasHistory)
        assertEquals(0, result.summary.totalVideosPlayed)
        assertEquals(0, result.summary.totalCompletedVideos)
        assertEquals(0L, result.summary.totalWatchTimeMs)
        assertEquals("0%", result.summary.formattedCompletionRate)
        assertTrue(result.mostPlayed.isEmpty())
        assertTrue(result.recentActivity.isEmpty())
        assertTrue(result.resolutions.isEmpty())
        assertTrue(result.videoFormats.isEmpty())
        assertEquals(7, result.weeklyActivity.size)
    }

    @Test
    fun singleVideo_calculatesCorrectWatchTimeAndCompletion() = runTest(testDispatcher) {
        val video = sampleVideo(
            id = "v1",
            title = "Movie 1",
            durationMs = 60_000L,
            playbackPositionMs = 30_000L,
            playbackPercentage = 0.5f,
            isCompleted = false,
            watchCount = 0
        )

        val result = useCase(listOf(video))

        assertTrue(result.hasHistory)
        assertEquals(1, result.summary.totalVideosPlayed)
        assertEquals(0, result.summary.totalCompletedVideos)
        assertEquals("0%", result.summary.formattedCompletionRate)
        assertEquals(30_000L, result.summary.totalWatchTimeMs)
        assertEquals(30_000L, result.summary.averageWatchDurationMs)

        assertEquals(1, result.mostPlayed.size)
        assertEquals("Movie 1", result.mostPlayed[0].title)
        assertEquals(1, result.mostPlayed[0].rank)

        assertEquals(1, result.resolutions.size)
        assertEquals("1080p", result.resolutions[0].label)
        assertEquals("100%", result.resolutions[0].formattedPercentage)
    }

    @Test
    fun multipleVideos_calculatesTotalAndAverageWatchDuration() = runTest(testDispatcher) {
        // v1: in progress (40s)
        val v1 = sampleVideo("v1", "V1", durationMs = 100_000L, playbackPositionMs = 40_000L, isCompleted = false, watchCount = 0)
        // v2: completed once (120s)
        val v2 = sampleVideo("v2", "V2", durationMs = 120_000L, playbackPositionMs = 120_000L, isCompleted = true, watchCount = 1)
        // v3: completed once and restarted, watched 20s (60s + 20s = 80s)
        val v3 = sampleVideo("v3", "V3", durationMs = 60_000L, playbackPositionMs = 20_000L, isCompleted = false, watchCount = 2)

        val result = useCase(listOf(v1, v2, v3))

        assertEquals(3, result.summary.totalVideosPlayed)
        assertEquals(1, result.summary.totalCompletedVideos) // only v2 is currently completed
        // Total time: 40_000 + 120_000 + (1 * 60_000 + 20_000) = 240_000L (4m)
        assertEquals(240_000L, result.summary.totalWatchTimeMs)
        assertEquals(80_000L, result.summary.averageWatchDurationMs)
        assertEquals("4m", result.summary.formattedTotalWatchTime)
    }

    @Test
    fun completionRate_calculatesAccuratePercentage() = runTest(testDispatcher) {
        val v1 = sampleVideo("v1", "V1", isCompleted = true, playbackPercentage = 1.0f)
        val v2 = sampleVideo("v2", "V2", isCompleted = false, playbackPercentage = 0.96f) // >= 0.949 counts as completed
        val v3 = sampleVideo("v3", "V3", isCompleted = false, playbackPercentage = 0.30f)
        val v4 = sampleVideo("v4", "V4", isCompleted = false, playbackPercentage = 0.50f)

        val result = useCase(listOf(v1, v2, v3, v4))

        assertEquals(4, result.summary.totalVideosPlayed)
        assertEquals(2, result.summary.totalCompletedVideos)
        assertEquals(0.5f, result.summary.completionRate)
        assertEquals("50%", result.summary.formattedCompletionRate)
    }

    @Test
    fun mostPlayed_ordersByWatchCountAndPercentage() = runTest(testDispatcher) {
        val v1 = sampleVideo("v1", "V1", watchCount = 1, playbackPercentage = 0.5f)
        val v2 = sampleVideo("v2", "V2", watchCount = 5, playbackPercentage = 1.0f)
        val v3 = sampleVideo("v3", "V3", watchCount = 3, playbackPercentage = 0.8f)

        val result = useCase(listOf(v1, v2, v3))

        assertEquals(3, result.mostPlayed.size)
        assertEquals("V2", result.mostPlayed[0].title)
        assertEquals(1, result.mostPlayed[0].rank)
        assertEquals(5, result.mostPlayed[0].watchCount)

        assertEquals("V3", result.mostPlayed[1].title)
        assertEquals(2, result.mostPlayed[1].rank)

        assertEquals("V1", result.mostPlayed[2].title)
        assertEquals(3, result.mostPlayed[2].rank)
    }

    @Test
    fun activityByDay_correctlyBinsPast7Days() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val todayTime = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -2)
        val twoDaysAgoTime = cal.timeInMillis

        val vToday = sampleVideo("v1", "Today Video", lastPlayedAt = todayTime, durationMs = 60_000L, isCompleted = true, watchCount = 1)
        val vPast = sampleVideo("v2", "2 Days Ago Video", lastPlayedAt = twoDaysAgoTime, durationMs = 120_000L, isCompleted = true, watchCount = 1)

        val result = useCase(listOf(vToday, vPast), nowMillis = now)

        assertEquals(7, result.weeklyActivity.size)
        val todayItem = result.weeklyActivity.last()
        assertTrue(todayItem.isToday)
        assertEquals(1, todayItem.videosPlayedCount)
        assertEquals(60_000L, todayItem.watchTimeMs)

        val twoDaysAgoItem = result.weeklyActivity[4] // index 6 is today, 5 is yesterday, 4 is 2 days ago
        assertEquals(1, twoDaysAgoItem.videosPlayedCount)
        assertEquals(120_000L, twoDaysAgoItem.watchTimeMs)
    }

    @Test
    fun resolutionsAndCodecs_calculatesDistributionsAccurately() = runTest(testDispatcher) {
        val v1 = sampleVideo("1", "A", resolutionLabel = "1080p", videoCodec = "H.264", audioCodec = "AAC")
        val v2 = sampleVideo("2", "B", resolutionLabel = "1080p", videoCodec = "H.264", audioCodec = "AAC")
        val v3 = sampleVideo("3", "C", resolutionLabel = "4K", videoCodec = "HEVC", audioCodec = "AC3")
        val v4 = sampleVideo("4", "D", resolutionLabel = "720p", videoCodec = "VP9", audioCodec = "Opus")

        val result = useCase(listOf(v1, v2, v3, v4))

        // Resolutions
        assertEquals("1080p", result.resolutions[0].label)
        assertEquals(2, result.resolutions[0].count)
        assertEquals("50%", result.resolutions[0].formattedPercentage)

        // Video Formats
        assertEquals("H.264", result.videoFormats[0].label)
        assertEquals(2, result.videoFormats[0].count)
        assertEquals("50%", result.videoFormats[0].formattedPercentage)

        // Audio Formats
        assertEquals("AAC", result.audioFormats[0].label)
        assertEquals(2, result.audioFormats[0].count)
        assertEquals("50%", result.audioFormats[0].formattedPercentage)
    }

    @Test
    fun edgeCases_missingDurationOrNegativePosition_handledSafely() = runTest(testDispatcher) {
        val vBroken = sampleVideo(
            id = "broken",
            title = "Broken Video",
            durationMs = 0L,
            playbackPositionMs = -500L,
            playbackPercentage = 0f,
            isCompleted = false,
            watchCount = -1,
            lastPlayedAt = 1000L
        )

        val result = useCase(listOf(vBroken))

        assertTrue(result.hasHistory)
        assertEquals(1, result.summary.totalVideosPlayed)
        assertEquals(0L, result.summary.totalWatchTimeMs)
        assertEquals("0m", result.summary.formattedTotalWatchTime)
    }
}
