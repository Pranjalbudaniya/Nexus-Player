package com.nexus.player.feature.more.analytics

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.feature.more.analytics.domain.GetWatchAnalyticsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakeAnalyticsVideoRepository
    private lateinit var fakePlaybackQueueManager: PlaybackQueueManager
    private lateinit var fakeThumbnailLoader: FakeAnalyticsThumbnailLoader
    private lateinit var getWatchAnalyticsUseCase: GetWatchAnalyticsUseCase
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAnalyticsVideoRepository()
        fakePlaybackQueueManager = PlaybackQueueManagerImpl()
        fakeThumbnailLoader = FakeAnalyticsThumbnailLoader()
        getWatchAnalyticsUseCase = GetWatchAnalyticsUseCase(defaultDispatcher = testDispatcher)

        viewModel = AnalyticsViewModel(
            videoRepository = fakeRepository,
            getWatchAnalyticsUseCase = getWatchAnalyticsUseCase,
            playbackQueueManager = fakePlaybackQueueManager,
            thumbnailLoader = fakeThumbnailLoader,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleVideo(
        id: String,
        title: String,
        durationMs: Long = 100_000L,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0f,
        isCompleted: Boolean = false,
        watchCount: Int = 0,
        lastPlayedAt: Long? = 1000L
    ) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage/Movies",
        sizeBytes = 1_000_000L,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        videoCodec = "H.264",
        audioCodec = "AAC",
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
    fun uiState_emptyHistory_emitsEmptyState() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        fakeRepository.historyVideosFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Empty)
    }

    @Test
    fun uiState_withHistory_emitsSuccessStateWithCalculatedAnalytics() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val v1 = sampleVideo("1", "Video 1", durationMs = 60_000L, playbackPositionMs = 30_000L, playbackPercentage = 0.5f, isCompleted = false)
        val v2 = sampleVideo("2", "Video 2", durationMs = 120_000L, isCompleted = true, watchCount = 2)
        fakeRepository.historyVideosFlow.value = listOf(v1, v2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AnalyticsUiState.Success)
        val success = state as AnalyticsUiState.Success

        assertEquals(2, success.data.summary.totalVideosPlayed)
        assertEquals(1, success.data.summary.totalCompletedVideos)
        assertEquals("50%", success.data.summary.formattedCompletionRate)
        assertEquals(2, success.data.mostPlayed.size)
        assertEquals("Video 2", success.data.mostPlayed[0].title)
    }

    @Test
    fun playVideo_configuresQueueWithHistorySource() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val v1 = sampleVideo("1", "Video 1", watchCount = 3)
        val v2 = sampleVideo("2", "Video 2", watchCount = 1)
        fakeRepository.historyVideosFlow.value = listOf(v1, v2)
        advanceUntilIdle()

        viewModel.playVideo("1")
        advanceUntilIdle()

        val queueState = fakePlaybackQueueManager.queueState.value
        assertEquals("1", queueState.currentVideoId)
        assertEquals(2, queueState.size)
        assertEquals(QueueSource.History, queueState.source)
    }

    @Test
    fun clearAllAnalytics_callsRepositoryAndResetsHistoryAndCounts() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val v1 = sampleVideo("1", "Video 1", watchCount = 5)
        fakeRepository.historyVideosFlow.value = listOf(v1)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AnalyticsUiState.Success)

        viewModel.setClearDialogOpen(true)
        assertTrue(viewModel.isClearDialogOpen.value)

        viewModel.clearAllAnalytics()
        advanceUntilIdle()

        assertTrue(fakeRepository.hasClearedAnalytics)
        assertFalse(viewModel.isClearDialogOpen.value)
        assertTrue(viewModel.uiState.value is AnalyticsUiState.Empty)
    }

    @Test
    fun reactiveUpdates_updatesUiWhenRepositoryEmitsNewData() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        fakeRepository.historyVideosFlow.value = emptyList()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AnalyticsUiState.Empty)

        // New playback happens in database
        val v1 = sampleVideo("1", "Newly Watched", playbackPositionMs = 5000L)
        fakeRepository.historyVideosFlow.value = listOf(v1)
        advanceUntilIdle()

        val updated = viewModel.uiState.value
        assertTrue(updated is AnalyticsUiState.Success)
        assertEquals(1, (updated as AnalyticsUiState.Success).data.summary.totalVideosPlayed)
    }

    // --- Test Doubles ---

    private class FakeAnalyticsVideoRepository : VideoRepository {
        val historyVideosFlow = MutableStateFlow<List<Video>>(emptyList())
        var hasClearedAnalytics = false

        override fun getAllHistoryVideos(): Flow<List<Video>> = historyVideosFlow.asStateFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = historyVideosFlow.asStateFlow()

        override suspend fun clearAllAnalyticsAndHistory() {
            hasClearedAnalytics = true
            historyVideosFlow.value = emptyList()
        }

        override suspend fun clearAllHistory() {
            historyVideosFlow.value = emptyList()
        }

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(emptyList())
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getFavoriteVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = null
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = 0
        override suspend fun insertVideo(video: Video): Long = 1L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }

    private class FakeAnalyticsThumbnailLoader : ThumbnailLoader {
        override suspend fun loadThumbnail(mediaUri: String, targetWidth: Int, targetHeight: Int): android.graphics.Bitmap? = null
        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }
}
