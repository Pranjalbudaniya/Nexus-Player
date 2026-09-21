package com.nexus.player.feature.home

import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.feature.home.domain.model.ContinueWatchingVideo
import com.nexus.player.feature.home.domain.model.FavoriteVideo
import com.nexus.player.feature.home.domain.model.HomeFolder
import com.nexus.player.feature.home.domain.model.RecentlyAddedVideo
import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProvider
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProvider
import com.nexus.player.feature.home.domain.provider.FoldersSectionProvider
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContinueWatchingProvider: FakeContinueWatchingProvider
    private lateinit var fakeRecentlyAddedProvider: FakeRecentlyAddedProvider
    private lateinit var fakeFavoritesProvider: FakeFavoritesProvider
    private lateinit var fakeFoldersProvider: FakeFoldersProvider
    private lateinit var fakeScanOrchestrator: FakeScanOrchestrator
    private lateinit var fakeStorageRepository: FakeStorageRepository
    private lateinit var fakeThumbnailLoader: FakeThumbnailLoader
    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var fakeFileOperationsManager: FakeVideoFileOperationsManager

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeContinueWatchingProvider = FakeContinueWatchingProvider()
        fakeRecentlyAddedProvider = FakeRecentlyAddedProvider()
        fakeFavoritesProvider = FakeFavoritesProvider()
        fakeFoldersProvider = FakeFoldersProvider()
        fakeScanOrchestrator = FakeScanOrchestrator()
        fakeStorageRepository = FakeStorageRepository()
        fakeThumbnailLoader = FakeThumbnailLoader()
        fakeVideoRepository = FakeVideoRepository()
        fakeFileOperationsManager = FakeVideoFileOperationsManager()

        viewModel = HomeViewModel(
            continueWatchingProvider = fakeContinueWatchingProvider,
            recentlyAddedProvider = fakeRecentlyAddedProvider,
            favoritesProvider = fakeFavoritesProvider,
            foldersProvider = fakeFoldersProvider,
            mediaScanOrchestrator = fakeScanOrchestrator,
            storageAccessRepository = fakeStorageRepository,
            thumbnailLoader = fakeThumbnailLoader,
            videoRepository = fakeVideoRepository,
            fileOperationsManager = fakeFileOperationsManager,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_startsAsLoadingAndPopulatesAllFourSections() = runTest(testDispatcher) {
        // Setup sample data
        fakeContinueWatchingProvider.items.value = listOf(
            ContinueWatchingVideo("cw_1", "uri1", null, "CW Title", 1000L, "00:01", 500L, 0.5f, "4K", 1000L)
        )
        fakeRecentlyAddedProvider.items.value = listOf(
            RecentlyAddedVideo("ra_1", "uri2", null, "RA Title", 2000L, "00:02", "4K • HEVC", "Added today • Movies", "4K", 2000L)
        )
        fakeFavoritesProvider.items.value = listOf(
            FavoriteVideo("fav_1", "uri3", null, "Fav Title", 3000L, "00:03", "1080p", 3000L)
        )
        fakeFoldersProvider.items.value = listOf(
            HomeFolder("/storage/Movies", "Movies", 5, "5 videos", "preview_uri", 4000L)
        )

        val state = viewModel.uiState.first { it is HomeUiState.Success }
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success

        // 1. Continue Watching
        assertEquals(1, success.continueWatching.size)
        assertEquals("cw_1", success.continueWatching.first().id)
        assertEquals(0.5f, success.continueWatching.first().progress, 0.001f)

        // 2. Recently Added
        assertEquals(1, success.recentlyAdded.size)
        assertEquals("ra_1", success.recentlyAdded.first().id)
        assertEquals("4K • HEVC", success.recentlyAdded.first().technicalSpecs)

        // 3. Favorites
        assertEquals(1, success.favorites.size)
        assertEquals("fav_1", success.favorites.first().id)

        // 4. Folders
        assertEquals(1, success.folders.size)
        assertEquals("/storage/Movies", success.folders.first().id)
        assertEquals("Movies", success.folders.first().name)
        assertEquals("5 videos", success.folders.first().videoCountText)
    }

    @Test
    fun emptySections_handledGracefullyInSuccessState() = runTest(testDispatcher) {
        // Only Recently Added and Folders have data; Continue Watching and Favorites are empty
        fakeRecentlyAddedProvider.items.value = listOf(
            RecentlyAddedVideo("ra_1", "uri2", null, "RA Title", 2000L, "00:02", "4K", "Added today", "4K", 2000L)
        )
        fakeFoldersProvider.items.value = listOf(
            HomeFolder("/storage/Movies", "Movies", 1, "1 video", null, 1000L)
        )

        val state = viewModel.uiState.first { it is HomeUiState.Success }
        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success

        assertTrue(success.isContinueWatchingEmpty)
        assertTrue(success.isFavoritesEmpty)
        assertFalse(success.isRecentlyAddedEmpty)
        assertFalse(success.isFoldersEmpty)
    }

    @Test
    fun allEmpty_whenLibraryEmpty_emitsEmptyState() = runTest(testDispatcher) {
        fakeContinueWatchingProvider.items.value = emptyList()
        fakeRecentlyAddedProvider.items.value = emptyList()
        fakeFavoritesProvider.items.value = emptyList()
        fakeFoldersProvider.items.value = emptyList()

        val state = viewModel.uiState.first { it is HomeUiState.Empty }
        assertTrue(state is HomeUiState.Empty)
        val empty = state as HomeUiState.Empty
        assertFalse(empty.isScanning)
        assertFalse(empty.noAccessibleMedia)
    }

    @Test
    fun emptyState_reflectsScanningStatus() = runTest(testDispatcher) {
        fakeScanOrchestrator.setScanning(true)

        val state = viewModel.uiState.first { it is HomeUiState.Empty && it.isScanning }
        assertTrue(state is HomeUiState.Empty)
        val empty = state as HomeUiState.Empty
        assertTrue(empty.isScanning)
    }

    @Test
    fun emptyState_reflectsNoAccessibleMediaWhenPermissionRevoked() = runTest(testDispatcher) {
        fakeStorageRepository.setPermissionGranted(false)

        val state = viewModel.uiState.first { it is HomeUiState.Empty && it.noAccessibleMedia }
        assertTrue(state is HomeUiState.Empty)
        val empty = state as HomeUiState.Empty
        assertTrue(empty.noAccessibleMedia)
    }

    @Test
    fun reactiveDatabaseUpdates_automaticallyUpdatesUiState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        fakeRecentlyAddedProvider.items.value = listOf(
            RecentlyAddedVideo("ra_1", "uri1", null, "Video 1", 1000L, "00:01", "HD", "Added today", "HD", 1000L)
        )

        val firstState = viewModel.uiState.first { it is HomeUiState.Success && it.recentlyAdded.size == 1 }
        assertEquals(1, (firstState as HomeUiState.Success).recentlyAdded.size)

        // Reactive update from DB: second video discovered
        fakeRecentlyAddedProvider.items.value = listOf(
            RecentlyAddedVideo("ra_1", "uri1", null, "Video 1", 1000L, "00:01", "HD", "Added today", "HD", 1000L),
            RecentlyAddedVideo("ra_2", "uri2", null, "Video 2", 2000L, "00:02", "4K", "Added now", "4K", 2000L)
        )

        val updatedState = viewModel.uiState.first { it is HomeUiState.Success && it.recentlyAdded.size == 2 }
        assertEquals(2, (updatedState as HomeUiState.Success).recentlyAdded.size)
    }

    @Test
    fun errorHandling_whenProviderThrows_emitsErrorState() = runTest(testDispatcher) {
        val errorViewModel = HomeViewModel(
            continueWatchingProvider = object : ContinueWatchingSectionProvider {
                override fun getContinueWatching(limit: Int): Flow<List<ContinueWatchingVideo>> = flow {
                    throw RuntimeException("Database read error")
                }
            },
            recentlyAddedProvider = fakeRecentlyAddedProvider,
            favoritesProvider = fakeFavoritesProvider,
            foldersProvider = fakeFoldersProvider,
            mediaScanOrchestrator = fakeScanOrchestrator,
            storageAccessRepository = fakeStorageRepository,
            thumbnailLoader = fakeThumbnailLoader,
            videoRepository = fakeVideoRepository,
            fileOperationsManager = fakeFileOperationsManager,
            ioDispatcher = testDispatcher
        )

        val state = errorViewModel.uiState.first { it is HomeUiState.Error }
        assertTrue(state is HomeUiState.Error)
        assertEquals("Database read error", (state as HomeUiState.Error).message)
    }

    @Test
    fun contextMenu_longClickSetsVideoAndDismissClearsIt() = runTest(testDispatcher) {
        val video = Video(
            id = "video_123",
            mediaUri = "content://media/video_123",
            filePath = "/storage/Movies/Movie.mp4",
            fileName = "Movie.mp4",
            title = "Movie Title",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L
        )
        fakeVideoRepository.setVideos(listOf(video))

        assertNull(viewModel.selectedVideoForMenu.value)

        viewModel.onVideoLongClick("video_123")
        advanceUntilIdle()

        assertNotNull(viewModel.selectedVideoForMenu.value)
        assertEquals("video_123", viewModel.selectedVideoForMenu.value?.id)
        assertEquals("Movie Title", viewModel.selectedVideoForMenu.value?.title)

        viewModel.dismissContextMenu()
        assertNull(viewModel.selectedVideoForMenu.value)
    }

    @Test
    fun toggleFavorite_delegatesToFileOperationsManager() = runTest(testDispatcher) {
        val video = Video(
            id = "fav_video",
            mediaUri = "content://media/fav_video",
            filePath = "/storage/Movies/Fav.mp4",
            fileName = "Fav.mp4",
            title = "Fav",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L,
            isFavorite = false
        )
        fakeVideoRepository.setVideos(listOf(video))

        viewModel.toggleFavorite(video.toMediaMetadata())
        advanceUntilIdle()

        assertEquals(true, fakeFileOperationsManager.favoriteVideos["fav_video"])
    }

    @Test
    fun renameVideo_delegatesToFileOperationsManager() = runTest(testDispatcher) {
        val video = Video(
            id = "ren_video",
            mediaUri = "content://media/ren_video",
            filePath = "/storage/Movies/Ren.mp4",
            fileName = "Ren.mp4",
            title = "Ren",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L
        )

        var callbackInvoked = false
        viewModel.renameVideo(video.toMediaMetadata(), "NewRen") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("ren_video" to "NewRen", fakeFileOperationsManager.renamedVideos.first())
    }

    @Test
    fun moveVideo_delegatesToFileOperationsManager() = runTest(testDispatcher) {
        val video = Video(
            id = "mv_video",
            mediaUri = "content://media/mv_video",
            filePath = "/storage/Movies/Mv.mp4",
            fileName = "Mv.mp4",
            title = "Mv",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L
        )

        var callbackInvoked = false
        viewModel.moveVideo(video.toMediaMetadata(), "/storage/Movies/Archive") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("mv_video" to "/storage/Movies/Archive", fakeFileOperationsManager.movedVideos.first())
    }

    @Test
    fun copyVideo_delegatesToFileOperationsManager() = runTest(testDispatcher) {
        val video = Video(
            id = "cp_video",
            mediaUri = "content://media/cp_video",
            filePath = "/storage/Movies/Cp.mp4",
            fileName = "Cp.mp4",
            title = "Cp",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L
        )

        var callbackInvoked = false
        viewModel.copyVideo(video.toMediaMetadata(), "/storage/Movies/Backup") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("cp_video" to "/storage/Movies/Backup", fakeFileOperationsManager.copiedVideos.first())
    }

    @Test
    fun deleteVideo_delegatesToFileOperationsManagerAndRemovesFromQueue() = runTest(testDispatcher) {
        val video = Video(
            id = "del_video",
            mediaUri = "content://media/del_video",
            filePath = "/storage/Movies/Del.mp4",
            fileName = "Del.mp4",
            title = "Del",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 1000L,
            lastModified = 1000L
        )

        viewModel.playVideo("del_video")
        assertEquals(listOf("del_video"), viewModel.playbackQueueManager.queueState.value.items)

        var callbackInvoked = false
        viewModel.deleteVideo(video.toMediaMetadata()) { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(listOf("del_video"), fakeFileOperationsManager.deletedVideos)
        assertTrue(viewModel.playbackQueueManager.queueState.value.items.isEmpty())
    }

    @Test
    fun restoreDeletedVideo_delegatesToFileOperationsManager() = runTest(testDispatcher) {
        var callbackInvoked = false
        viewModel.restoreDeletedVideo("del_video") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(listOf("del_video"), fakeFileOperationsManager.restoredVideos)
    }

    // =========================================================================
    // Test Fakes
    // =========================================================================

    private class FakeContinueWatchingProvider : ContinueWatchingSectionProvider {
        val items = MutableStateFlow<List<ContinueWatchingVideo>>(emptyList())
        override fun getContinueWatching(limit: Int): Flow<List<ContinueWatchingVideo>> = items.asStateFlow()
    }

    private class FakeRecentlyAddedProvider : RecentlyAddedSectionProvider {
        val items = MutableStateFlow<List<RecentlyAddedVideo>>(emptyList())
        override fun getRecentlyAdded(limit: Int): Flow<List<RecentlyAddedVideo>> = items.asStateFlow()
    }

    private class FakeFavoritesProvider : FavoritesSectionProvider {
        val items = MutableStateFlow<List<FavoriteVideo>>(emptyList())
        override fun getFavorites(limit: Int): Flow<List<FavoriteVideo>> = items.asStateFlow()
    }

    private class FakeFoldersProvider : FoldersSectionProvider {
        val items = MutableStateFlow<List<HomeFolder>>(emptyList())
        override fun getHomeFolders(limit: Int): Flow<List<HomeFolder>> = items.asStateFlow()
    }

    private class FakeScanOrchestrator : MediaScanOrchestrator {
        private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
        override val scanState = _scanState.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning = _isScanning.asStateFlow()

        fun setScanning(scanning: Boolean) {
            _isScanning.value = scanning
        }

        override fun triggerStartupScan(): Boolean = true
        override fun triggerManualScan(): Boolean = true
        override fun triggerIncrementalScan(): Boolean = true
        override fun triggerLocationScan(folderUriOrPath: String): Boolean = true
        override fun cancelScan() {}
    }

    private class FakeStorageRepository : StorageAccessRepository {
        private val _state = MutableStateFlow(
            StorageAccessState(
                isOnboardingCompleted = true,
                isPermissionGranted = true,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        override val storageAccessState = _state.asStateFlow()

        fun setPermissionGranted(granted: Boolean) {
            _state.value = _state.value.copy(isPermissionGranted = granted)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setStorageAccessMode(mode: StorageAccessMode) {}
        override suspend fun addSelectedFolderUri(uriString: String) {}
        override suspend fun removeSelectedFolderUri(uriString: String) {}
        override suspend fun clearSelectedFolders() {}
        override fun isPermissionGranted(): Boolean = _state.value.isPermissionGranted
        override fun getRequiredPermissions(): List<String> = emptyList()
    }

    private class FakeThumbnailLoader : com.nexus.player.core.media.thumbnail.ThumbnailLoader {
        override suspend fun loadThumbnail(mediaUri: String, targetWidth: Int, targetHeight: Int): android.graphics.Bitmap? = null
        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }

    private class FakeVideoRepository : VideoRepository {
        private val _videosFlow = MutableStateFlow<List<Video>>(emptyList())

        fun setVideos(videos: List<Video>) {
            _videosFlow.value = videos
        }

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = _videosFlow.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = _videosFlow.value.find { it.id == id }
        override suspend fun getVideoByUri(mediaUri: String): Video? = _videosFlow.value.find { it.mediaUri == mediaUri }
        override suspend fun getVideosCount(): Int = _videosFlow.value.size
        override fun searchVideos(query: String): Flow<List<Video>> = flowOf(emptyList())
        override fun searchVideos(filter: SearchFilter): Flow<List<Video>> = flowOf(emptyList())
        override suspend fun insertVideo(video: Video): Long = 1L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun updatePlaybackProgress(id: String, positionMs: Long, percentage: Float, lastPlayedAt: Long) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }
}
