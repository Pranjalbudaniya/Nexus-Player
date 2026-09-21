package com.nexus.player.feature.library

import android.graphics.Bitmap
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.scanner.model.ScanProgress
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.feature.library.preferences.FolderSortDirection
import com.nexus.player.feature.library.preferences.FolderSortField
import com.nexus.player.feature.library.preferences.FolderSortOption
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibraryPreferencesRepository
import com.nexus.player.feature.library.preferences.LibrarySortDirection
import com.nexus.player.feature.library.preferences.LibrarySortField
import com.nexus.player.feature.library.preferences.LibrarySortOption
import com.nexus.player.feature.library.preferences.LibraryTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class LibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var videoRepository: FakeVideoRepository
    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var scanOrchestrator: FakeScanOrchestrator
    private lateinit var storageRepository: FakeStorageRepository
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var thumbnailLoader: FakeThumbnailLoader
    private lateinit var fileOperationsManager: FakeVideoFileOperationsManager
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        videoRepository = FakeVideoRepository()
        folderRepository = FakeFolderRepository()
        scanOrchestrator = FakeScanOrchestrator()
        storageRepository = FakeStorageRepository()
        preferencesRepository = FakePreferencesRepository()
        thumbnailLoader = FakeThumbnailLoader()
        fileOperationsManager = FakeVideoFileOperationsManager()

        viewModel = LibraryViewModel(
            videoRepository = videoRepository,
            folderRepository = folderRepository,
            mediaScanOrchestrator = scanOrchestrator,
            storageAccessRepository = storageRepository,
            libraryPreferencesRepository = preferencesRepository,
            thumbnailLoader = thumbnailLoader,
            fileOperationsManager = fileOperationsManager,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsVideosAndPreferences() = runTest {
        val sample = createSampleVideo(id = "v1", title = "Inception")
        videoRepository.setVideos(listOf(sample))
        folderRepository.setRootFolders(listOf(VideoFolder(folderPath = "/storage/emulated/0/Movies", folderName = "Movies", videoCount = 1)))

        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(1, state.videos.size)
        assertEquals(1, state.folders.size)
        assertEquals("Inception", state.videos.first().title)
        assertEquals("Movies", state.folders.first().folderName)
        assertEquals(LibraryLayoutMode.GRID, state.layoutMode)
        assertEquals(LibraryTab.VIDEOS, state.selectedTab)
        assertEquals(LibrarySortField.RECENTLY_ADDED, state.sortOption.field)
        assertTrue(state.isStorageAccessGranted)
        assertFalse(state.isScanning)
    }

    @Test
    fun reactiveRoomUpdates_reflectsNewlyAddedVideos() = runTest {
        videoRepository.setVideos(listOf(createSampleVideo(id = "v1", title = "Interstellar")))

        val state1 = viewModel.uiState.first { it.videos.size == 1 }
        assertEquals(1, state1.videos.size)

        // Simulate background scanner discovering another video and inserting into Room
        videoRepository.setVideos(
            listOf(
                createSampleVideo(id = "v1", title = "Interstellar"),
                createSampleVideo(id = "v2", title = "Dunkirk")
            )
        )

        val state2 = viewModel.uiState.first { it.videos.size == 2 }
        assertEquals(2, state2.videos.size)
        assertEquals("Dunkirk", state2.videos[1].title)
    }

    @Test
    fun reactiveRoomUpdates_reflectsRemovedVideos() = runTest {
        videoRepository.setVideos(
            listOf(
                createSampleVideo(id = "v1", title = "Movie A"),
                createSampleVideo(id = "v2", title = "Movie B")
            )
        )

        val state1 = viewModel.uiState.first { it.videos.size == 2 }
        assertEquals(2, state1.videos.size)

        // Simulate stale pruning
        videoRepository.setVideos(listOf(createSampleVideo(id = "v1", title = "Movie A")))

        val state2 = viewModel.uiState.first { it.videos.size == 1 }
        assertEquals(1, state2.videos.size)
        assertEquals("Movie A", state2.videos.first().title)
    }

    @Test
    fun emptyState_whenNoVideosAvailable() = runTest {
        videoRepository.setVideos(emptyList())

        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isEmpty)
        assertEquals(0, state.videos.size)
    }

    @Test
    fun layoutModeSwitch_updatesPreference() = runTest {
        assertEquals(LibraryLayoutMode.GRID, viewModel.uiState.value.layoutMode)

        viewModel.setLayoutMode(LibraryLayoutMode.LIST)

        val state = viewModel.uiState.first { it.layoutMode == LibraryLayoutMode.LIST }
        assertEquals(LibraryLayoutMode.LIST, state.layoutMode)
        assertEquals(LibraryLayoutMode.LIST, preferencesRepository.layoutMode.value)
    }

    @Test
    fun sortingChange_updatesSortOptionAndQueriesRepository() = runTest {
        viewModel.setSortField(LibrarySortField.NAME)

        val state = viewModel.uiState.first { it.sortOption.field == LibrarySortField.NAME }
        assertEquals(LibrarySortField.NAME, state.sortOption.field)
        assertEquals(LibrarySortDirection.ASCENDING, state.sortOption.direction)
        assertEquals(VideoSortOrder.TITLE_ASC, videoRepository.lastRequestedSortOrder)

        // Toggling same field inverts direction
        viewModel.setSortField(LibrarySortField.NAME)
        val stateDesc = viewModel.uiState.first { it.sortOption.direction == LibrarySortDirection.DESCENDING }
        assertEquals(LibrarySortDirection.DESCENDING, stateDesc.sortOption.direction)
        assertEquals(VideoSortOrder.TITLE_DESC, videoRepository.lastRequestedSortOrder)
    }

    @Test
    fun toggleSortDirection_invertsDirection() = runTest {
        val initialDir = viewModel.uiState.value.sortOption.direction
        viewModel.toggleSortDirection()

        val toggled = viewModel.uiState.first { it.sortOption.direction != initialDir }
        assertEquals(LibrarySortDirection.ASCENDING, toggled.sortOption.direction)
    }

    @Test
    fun tabSwitch_updatesSelectedTab() = runTest {
        assertEquals(LibraryTab.VIDEOS, viewModel.uiState.value.selectedTab)

        viewModel.setSelectedTab(LibraryTab.FOLDERS)

        val state = viewModel.uiState.first { it.selectedTab == LibraryTab.FOLDERS }
        assertEquals(LibraryTab.FOLDERS, state.selectedTab)
        assertEquals(LibraryTab.FOLDERS, preferencesRepository.selectedTab.value)
    }

    @Test
    fun folderSortingChange_updatesSortOptionAndQueriesFolderRepository() = runTest {
        viewModel.setFolderSortOption(
            FolderSortOption(FolderSortField.VIDEO_COUNT, FolderSortDirection.DESCENDING)
        )

        val state = viewModel.uiState.first { it.folderSortOption.field == FolderSortField.VIDEO_COUNT }
        assertEquals(FolderSortField.VIDEO_COUNT, state.folderSortOption.field)
        assertEquals(FolderSortDirection.DESCENDING, state.folderSortOption.direction)
        assertEquals(FolderSortOrder.VIDEO_COUNT_DESC, folderRepository.lastRequestedSortOrder)
    }

    @Test
    fun folderSortSheetVisibility_canBeToggled() = runTest {
        assertFalse(viewModel.uiState.value.isFolderSortSheetVisible)

        viewModel.showFolderSortSheet(true)
        val visibleState = viewModel.uiState.first { it.isFolderSortSheetVisible }
        assertTrue(visibleState.isFolderSortSheetVisible)

        viewModel.showFolderSortSheet(false)
        val hiddenState = viewModel.uiState.first { !it.isFolderSortSheetVisible }
        assertFalse(hiddenState.isFolderSortSheetVisible)
    }

    @Test
    fun scannerState_reflectsScanningAndProgress() = runTest {
        scanOrchestrator.setScanning(true)
        scanOrchestrator.setScanState(ScanState.Scanning(ScanProgress(current = 15, total = 50)))

        val state = viewModel.uiState.first { it.isScanning }
        assertTrue(state.isScanning)
        assertTrue(state.scanState is ScanState.Scanning)
        val progress = (state.scanState as ScanState.Scanning).progress
        assertEquals(15, progress?.current)
        assertEquals(50, progress?.total)
    }

    @Test
    fun scannerError_reflectsErrorState() = runTest {
        scanOrchestrator.setScanState(ScanState.Error("I/O error during indexing"))

        val state = viewModel.uiState.first { it.scanState is ScanState.Error }
        assertTrue(state.scanState is ScanState.Error)
        assertEquals("I/O error during indexing", (state.scanState as ScanState.Error).message)
    }

    @Test
    fun storagePermissionRevoked_reflectsState() = runTest {
        storageRepository.setPermissionGranted(false)

        val state = viewModel.uiState.first { !it.isStorageAccessGranted }
        assertFalse(state.isStorageAccessGranted)
    }

    @Test
    fun triggerRescan_invokesOrchestrator() = runTest {
        val triggered = viewModel.triggerRescan()
        assertTrue(triggered)
        assertEquals(1, scanOrchestrator.manualScanCallCount)
    }

    @Test
    fun contextMenu_longClickSetsVideoAndDismissClearsIt() = runTest {
        val video = createSampleVideo(id = "v1", title = "Oppenheimer")
        videoRepository.setVideos(listOf(video))

        val mediaMetadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        assertNull(viewModel.uiState.value.selectedVideoForMenu)

        viewModel.onVideoLongClick(mediaMetadata)
        val menuState = viewModel.uiState.first { it.selectedVideoForMenu != null }
        assertEquals("Oppenheimer", menuState.selectedVideoForMenu?.title)

        viewModel.dismissContextMenu()
        val dismissedState = viewModel.uiState.first { it.selectedVideoForMenu == null }
        assertNull(dismissedState.selectedVideoForMenu)
    }

    @Test
    fun sortSheetVisibility_canBeToggled() = runTest {
        val initialState = viewModel.uiState.first()
        assertFalse(initialState.isSortSheetVisible)

        viewModel.showSortSheet(true)
        val visibleState = viewModel.uiState.first { it.isSortSheetVisible }
        assertTrue(visibleState.isSortSheetVisible)

        viewModel.showSortSheet(false)
        val hiddenState = viewModel.uiState.first { !it.isSortSheetVisible }
        assertFalse(hiddenState.isSortSheetVisible)
    }

    @Test
    fun toggleFavorite_callsFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Interstellar")
        videoRepository.setVideos(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        viewModel.toggleFavorite(metadata)
        assertEquals(true, fileOperationsManager.favoriteVideos["v1"])
    }

    @Test
    fun renameVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Inception")
        videoRepository.setVideos(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.renameVideo(metadata, "Inception 2") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "Inception 2", fileOperationsManager.renamedVideos.first())
    }

    @Test
    fun moveVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Dune")
        videoRepository.setVideos(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.moveVideo(metadata, "/storage/emulated/0/SciFi") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "/storage/emulated/0/SciFi", fileOperationsManager.movedVideos.first())
    }

    @Test
    fun copyVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Dune")
        videoRepository.setVideos(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.copyVideo(metadata, "/storage/emulated/0/Backup") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "/storage/emulated/0/Backup", fileOperationsManager.copiedVideos.first())
    }

    @Test
    fun deleteVideo_stagesAndRemovesFromQueue() = runTest {
        val video = createSampleVideo(id = "v1", title = "Tenet")
        videoRepository.setVideos(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        viewModel.playVideo("v1")
        assertEquals(listOf("v1"), viewModel.playbackQueueManager.queueState.value.items)

        var callbackInvoked = false
        viewModel.deleteVideo(metadata) { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals(listOf("v1"), fileOperationsManager.deletedVideos)
        assertTrue(viewModel.playbackQueueManager.queueState.value.items.isEmpty())
    }

    @Test
    fun restoreDeletedVideo_callsFileOperationsManager() = runTest {
        var callbackInvoked = false
        viewModel.restoreDeletedVideo("v1") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals(listOf("v1"), fileOperationsManager.restoredVideos)
    }

    private fun createSampleVideo(id: String, title: String): Video = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/emulated/0/Movies/$id.mp4",
        fileName = "$id.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage/emulated/0/Movies",
        sizeBytes = 1_500_000_000L,
        durationMs = 7_200_000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        videoCodec = "H.264",
        audioCodec = "AAC",
        dateAdded = 1000L,
        lastModified = 1000L
    )

    // =========================================================================
    // Fakes
    // =========================================================================

    private class FakeVideoRepository : VideoRepository {
        private val _videosFlow = MutableStateFlow<List<Video>>(emptyList())
        var lastRequestedSortOrder: VideoSortOrder? = null

        fun setVideos(videos: List<Video>) {
            _videosFlow.value = videos
        }

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> {
            lastRequestedSortOrder = sortOrder
            return _videosFlow.asStateFlow()
        }

        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = _videosFlow.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = _videosFlow.value.find { it.id == id }
        override suspend fun getVideoByUri(mediaUri: String): Video? = _videosFlow.value.find { it.mediaUri == mediaUri }
        override suspend fun getVideosCount(): Int = _videosFlow.value.size
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

    private class FakeFolderRepository : FolderRepository {
        private val _rootFoldersFlow = MutableStateFlow<List<VideoFolder>>(emptyList())
        var lastRequestedSortOrder: FolderSortOrder? = null

        fun setRootFolders(folders: List<VideoFolder>) {
            _rootFoldersFlow.value = folders
        }

        override fun getFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> {
            lastRequestedSortOrder = sortOrder
            return _rootFoldersFlow.asStateFlow()
        }

        override fun getRootFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> {
            lastRequestedSortOrder = sortOrder
            return _rootFoldersFlow.asStateFlow()
        }

        override fun getChildFolders(parentFolderPath: String, sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = flowOf(emptyList())
        override fun getFolderByPath(folderPath: String): Flow<VideoFolder?> = flowOf(_rootFoldersFlow.value.find { it.folderPath == folderPath })
        override fun getVideosInFolder(folderPath: String, sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(emptyList())
    }

    private class FakeScanOrchestrator : MediaScanOrchestrator {
        private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
        override val scanState = _scanState.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning = _isScanning.asStateFlow()

        var manualScanCallCount = 0
        var cancelScanCallCount = 0

        fun setScanState(state: ScanState) {
            _scanState.value = state
        }

        fun setScanning(scanning: Boolean) {
            _isScanning.value = scanning
        }

        override fun triggerStartupScan(): Boolean = true
        override fun triggerManualScan(): Boolean {
            manualScanCallCount++
            return true
        }
        override fun triggerIncrementalScan(): Boolean = true
        override fun triggerLocationScan(locationUriOrPath: String): Boolean = true
        override fun cancelScan() {
            cancelScanCallCount++
        }
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

    private class FakePreferencesRepository : LibraryPreferencesRepository {
        private val _layoutMode = MutableStateFlow(LibraryLayoutMode.GRID)
        override val layoutMode = _layoutMode.asStateFlow()

        private val _sortOption = MutableStateFlow(LibrarySortOption())
        override val sortOption = _sortOption.asStateFlow()

        private val _selectedTab = MutableStateFlow(LibraryTab.VIDEOS)
        override val selectedTab = _selectedTab.asStateFlow()

        private val _folderSortOption = MutableStateFlow(FolderSortOption())
        override val folderSortOption = _folderSortOption.asStateFlow()

        override suspend fun setLayoutMode(mode: LibraryLayoutMode) {
            _layoutMode.value = mode
        }

        override suspend fun setSortOption(sortOption: LibrarySortOption) {
            _sortOption.value = sortOption
        }

        override suspend fun setSelectedTab(tab: LibraryTab) {
            _selectedTab.value = tab
        }

        override suspend fun setFolderSortOption(sortOption: FolderSortOption) {
            _folderSortOption.value = sortOption
        }
    }

    private class FakeThumbnailLoader : ThumbnailLoader {
        override suspend fun loadThumbnail(mediaUri: String, targetWidth: Int, targetHeight: Int): Bitmap? = null
        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }
}
