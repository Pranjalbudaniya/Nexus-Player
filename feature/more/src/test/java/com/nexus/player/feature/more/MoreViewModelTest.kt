package com.nexus.player.feature.more

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.operations.VideoFileOperationsManager
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
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
class MoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakeMoreVideoRepository
    private lateinit var fakePlaybackQueueManager: PlaybackQueueManager
    private lateinit var fakeThumbnailLoader: FakeThumbnailLoader
    private lateinit var fakeFileOperationsManager: FakeVideoFileOperationsManager
    private lateinit var viewModel: MoreViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMoreVideoRepository()
        fakePlaybackQueueManager = PlaybackQueueManagerImpl()
        fakeThumbnailLoader = FakeThumbnailLoader()
        fakeFileOperationsManager = FakeVideoFileOperationsManager()

        viewModel = MoreViewModel(
            videoRepository = fakeRepository,
            playbackQueueManager = fakePlaybackQueueManager,
            thumbnailLoader = fakeThumbnailLoader,
            fileOperationsManager = fakeFileOperationsManager,
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
        lastPlayedAt: Long? = 1000L,
        playbackPositionMs: Long = 20000L,
        playbackPercentage: Float = 0.2f,
        isCompleted: Boolean = false,
        isFavorite: Boolean = false,
        watchCount: Int = 1
    ) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage/Movies",
        sizeBytes = 1000000L,
        durationMs = 100000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        dateAdded = 1000L,
        lastModified = 1000L,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = isFavorite,
        isCompleted = isCompleted,
        watchCount = watchCount
    )

    @Test
    fun uiState_loadsHistoryAndCalculatesWatchStats() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val v1 = sampleVideo("1", "In Progress Video", playbackPercentage = 0.3f, isCompleted = false)
        val v2 = sampleVideo("2", "Completed Video", playbackPercentage = 1.0f, isCompleted = true)
        val v3 = sampleVideo("3", "Near End Video", playbackPercentage = 0.96f, isCompleted = false)
        fakeRepository.historyVideosFlow.value = listOf(v3, v2, v1)
        fakeRepository.favoriteVideosFlow.value = listOf(v1, v2)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MoreUiState.Success)
        val success = state as MoreUiState.Success

        assertEquals(3, success.history.size)
        assertEquals("Near End Video", success.history[0].title)
        assertTrue(success.history[0].isCompleted)
        assertTrue(success.history[1].isCompleted)
        assertFalse(success.history[2].isCompleted)

        assertEquals(3, success.stats.totalWatched)
        assertEquals(2, success.stats.completedCount)
        assertEquals(2, success.stats.favoritesCount)
    }

    @Test
    fun playVideo_configuresQueueWithHistorySource() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val v1 = sampleVideo("1", "Video 1")
        val v2 = sampleVideo("2", "Video 2")
        fakeRepository.historyVideosFlow.value = listOf(v1, v2)
        advanceUntilIdle()

        viewModel.playVideo("2")
        advanceUntilIdle()

        val queueState = fakePlaybackQueueManager.queueState.value
        assertEquals("2", queueState.currentVideoId)
        assertEquals(2, queueState.size)
        assertEquals(QueueSource.History, queueState.source)
    }

    @Test
    fun clearHistoryItem_delegatesToRepository() = testScope.runTest {
        val v1 = sampleVideo("1", "Video 1")
        fakeRepository.historyVideosFlow.value = listOf(v1)
        advanceUntilIdle()

        viewModel.clearHistoryItem("1")
        advanceUntilIdle()

        assertTrue(fakeRepository.clearedVideoIds.contains("1"))
    }

    @Test
    fun clearAllHistory_delegatesToRepositoryAndDismissesDialog() = testScope.runTest {
        viewModel.setClearAllDialogOpen(true)
        assertTrue(viewModel.isClearAllDialogOpen.value)

        viewModel.clearAllHistory()
        advanceUntilIdle()

        assertTrue(fakeRepository.hasClearedAll)
        assertFalse(viewModel.isClearAllDialogOpen.value)
    }

    @Test
    fun toggleFavorite_updatesRepository() = testScope.runTest {
        val v = sampleVideo("fav_1", "Favorite Test", isFavorite = false)
        fakeRepository.videosMap["fav_1"] = v

        val metadata = MediaMetadata(
            id = "fav_1",
            mediaUri = v.mediaUri,
            filePath = v.filePath,
            fileName = v.fileName,
            title = v.title,
            folderName = v.folderName,
            folderPath = v.folderPath,
            formattedDuration = "01:40",
            durationMs = v.durationMs,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H.264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "4.0 Mbps",
            videoBitrate = 4000000L,
            formattedSize = "1 MB",
            sizeBytes = 1000000L,
            formattedModifiedDate = "Jan 1, 1970",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        viewModel.onToggleFavorite(metadata)
        advanceUntilIdle()

        assertTrue(fakeRepository.favoriteUpdates.containsKey("fav_1"))
        assertTrue(fakeRepository.favoriteUpdates["fav_1"] == true)
    }

    @Test
    fun favoriteVideos_emitsFromRepository() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favoriteVideos.collect {}
        }

        val v1 = sampleVideo("fav_1", "Favorite 1", isFavorite = true)
        val v2 = sampleVideo("fav_2", "Favorite 2", isFavorite = true)
        fakeRepository.favoriteVideosFlow.value = listOf(v1, v2)
        advanceUntilIdle()

        assertEquals(2, viewModel.favoriteVideos.value.size)
        assertEquals("fav_1", viewModel.favoriteVideos.value[0].id)
    }

    @Test
    fun playFavoriteVideo_configuresQueueWithFavoritesSource() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favoriteVideos.collect {}
        }

        val v1 = sampleVideo("fav_1", "Favorite 1", isFavorite = true)
        val v2 = sampleVideo("fav_2", "Favorite 2", isFavorite = true)
        fakeRepository.favoriteVideosFlow.value = listOf(v1, v2)
        advanceUntilIdle()

        viewModel.playFavoriteVideo("fav_2")
        advanceUntilIdle()

        val queueState = fakePlaybackQueueManager.queueState.value
        assertEquals("fav_2", queueState.currentVideoId)
        assertEquals(2, queueState.size)
        assertEquals(QueueSource.Favorites, queueState.source)
    }

    @Test
    fun recentVideoForInfo_emitsHistoryOrAllVideos() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.recentVideoForInfo.collect {}
        }

        // Case 1: Empty initially
        advanceUntilIdle()
        assertEquals(null, viewModel.recentVideoForInfo.value)

        // Case 2: Only allVideos available
        val vLibrary = sampleVideo("lib_1", "Library Video")
        fakeRepository.allVideosFlow.value = listOf(vLibrary)
        advanceUntilIdle()
        assertEquals("lib_1", viewModel.recentVideoForInfo.value?.id)

        // Case 3: History video takes priority
        val vHistory = sampleVideo("hist_1", "History Video")
        fakeRepository.historyVideosFlow.value = listOf(vHistory)
        advanceUntilIdle()
        assertEquals("hist_1", viewModel.recentVideoForInfo.value?.id)
    }

    @Test
    fun clearHistoryItem_undoRestoresProgress() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        val video = sampleVideo("v1", "Test Video").copy(
            lastPlayedAt = 1000L,
            playbackPositionMs = 5000L,
            playbackPercentage = 0.5f
        )
        fakeRepository.videosMap["v1"] = video
        fakeRepository.historyVideosFlow.value = listOf(video)
        advanceUntilIdle()

        var clearedTitle: String? = null
        viewModel.clearHistoryItem("v1") { item ->
            clearedTitle = item.title
        }
        advanceUntilIdle()

        assertEquals("Test Video", clearedTitle)
        assertTrue(fakeRepository.clearedVideoIds.contains("v1"))

        var restoredTitle: String? = null
        viewModel.undoClearHistoryItem { item ->
            restoredTitle = item.title
        }
        advanceUntilIdle()

        assertEquals("Test Video", restoredTitle)
        assertTrue(fakeRepository.restoredProgressUpdates.containsKey("v1"))
    }

    @Test
    fun onDeleteConfirm_withCallback_reportsSuccess() = testScope.runTest {
        val video = sampleVideo("v_del", "Delete Video")
        var deleteResult: Result<Unit>? = null

        viewModel.onDeleteConfirm(video.toMediaMetadata()) { result ->
            deleteResult = result
        }
        advanceUntilIdle()

        assertTrue(deleteResult?.isSuccess == true)
        assertFalse(fakePlaybackQueueManager.queueState.value.items.contains("v_del"))
    }

    @Test
    fun onToggleFavorite_withCallback_reportsNewState() = testScope.runTest {
        val video = sampleVideo("v_fav", "Favorite Video").copy(isFavorite = false)
        var reportedFavoriteState: Boolean? = null

        viewModel.onToggleFavorite(video.toMediaMetadata()) { newState ->
            reportedFavoriteState = newState
        }
        advanceUntilIdle()

        assertEquals(true, reportedFavoriteState)
        assertEquals(true, fakeRepository.favoriteUpdates["v_fav"])
    }

    @Test
    fun restoreDeletedVideo_callsFileOperationsManager() = testScope.runTest {
        var restoredTitle: String? = null
        viewModel.restoreDeletedVideo("v_del") { result ->
            restoredTitle = result.getOrNull()?.title
        }
        advanceUntilIdle()

        assertEquals("Restored", restoredTitle)
    }

    // --- Test Doubles ---

    private class FakeMoreVideoRepository : VideoRepository {
        val historyVideosFlow = MutableStateFlow<List<Video>>(emptyList())
        val favoriteVideosFlow = MutableStateFlow<List<Video>>(emptyList())
        val allVideosFlow = MutableStateFlow<List<Video>>(emptyList())
        val clearedVideoIds = mutableListOf<String>()
        val restoredProgressUpdates = mutableMapOf<String, Long>()
        var hasClearedAll = false
        val favoriteUpdates = mutableMapOf<String, Boolean>()
        val videosMap = mutableMapOf<String, Video>()

        override suspend fun updatePlaybackProgress(
            id: String,
            positionMs: Long,
            percentage: Float,
            lastPlayedAt: Long,
            isCompleted: Boolean
        ) {
            restoredProgressUpdates[id] = positionMs
        }

        override fun getAllHistoryVideos(): Flow<List<Video>> = historyVideosFlow.asStateFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = historyVideosFlow.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = favoriteVideosFlow.asStateFlow()
        override fun getFavoriteVideos(limit: Int): Flow<List<Video>> = favoriteVideosFlow.asStateFlow()
        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = allVideosFlow.asStateFlow()

        override suspend fun clearHistoryForVideo(id: String) {
            clearedVideoIds.add(id)
            historyVideosFlow.value = historyVideosFlow.value.filter { it.id != id }
        }

        override suspend fun clearAllHistory() {
            hasClearedAll = true
            historyVideosFlow.value = emptyList()
        }

        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            favoriteUpdates[id] = isFavorite
        }

        override suspend fun getVideoById(id: String): Video? = videosMap[id]
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = 0
        override suspend fun insertVideo(video: Video): Long = 1L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }

    private class FakeThumbnailLoader : ThumbnailLoader {
        override suspend fun loadThumbnail(mediaUri: String, targetWidth: Int, targetHeight: Int): android.graphics.Bitmap? = null
        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }

    private class FakeVideoFileOperationsManager : VideoFileOperationsManager {
        override fun getAvailableFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun renameVideo(videoId: String, newName: String): Result<MediaMetadata> = Result.success(
            sampleMetadata(videoId, newName)
        )
        override suspend fun moveVideo(videoId: String, targetFolderPath: String): Result<MediaMetadata> = Result.success(
            sampleMetadata(videoId, "Moved")
        )
        override suspend fun copyVideo(videoId: String, targetFolderPath: String): Result<MediaMetadata> = Result.success(
            sampleMetadata(videoId, "Copied")
        )
        override suspend fun deleteVideo(videoId: String, stageForUndo: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun restoreDeletedVideo(videoId: String): Result<MediaMetadata> = Result.success(sampleMetadata(videoId, "Restored"))
        override suspend fun purgeStagedDeletions() {}
        override suspend fun setFavorite(videoId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
        override fun shareVideo(context: android.content.Context, video: MediaMetadata) {}
        override fun openContainingFolder(
            context: android.content.Context,
            folderPath: String,
            folderName: String,
            onNavigateInApp: (folderPath: String, folderName: String) -> Unit
        ) {
            onNavigateInApp(folderPath, folderName)
        }

        private fun sampleMetadata(id: String, title: String) = MediaMetadata(
            id = id,
            mediaUri = "content://media/$id",
            filePath = "/storage/Movies/$title.mp4",
            fileName = "$title.mp4",
            title = title,
            folderName = "Movies",
            folderPath = "/storage/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H.264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "4.0 Mbps",
            videoBitrate = 4000000L,
            formattedSize = "1 MB",
            sizeBytes = 1000000L,
            formattedModifiedDate = "Jan 1, 1970",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )
    }
}
