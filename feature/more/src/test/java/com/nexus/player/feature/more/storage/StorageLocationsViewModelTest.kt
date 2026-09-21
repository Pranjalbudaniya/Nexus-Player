package com.nexus.player.feature.more.storage

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.LibrarySettings
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.scanner.model.ScanOptions
import com.nexus.player.core.scanner.model.ScanResult
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StorageLocationsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var fakeStorageAccessRepository: FakeStorageAccessRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeMediaScanOrchestrator: FakeMediaScanOrchestrator
    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var viewModel: StorageLocationsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        fakeStorageAccessRepository = FakeStorageAccessRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeMediaScanOrchestrator = FakeMediaScanOrchestrator()
        fakeVideoRepository = FakeVideoRepository()

        viewModel = StorageLocationsViewModel(
            context = context,
            storageAccessRepository = fakeStorageAccessRepository,
            settingsRepository = fakeSettingsRepository,
            mediaScanOrchestrator = fakeMediaScanOrchestrator,
            videoRepository = fakeVideoRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_allMediaMode_loadsFoldersFromDatabase() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        fakeStorageAccessRepository.state.value = StorageAccessState(
            isOnboardingCompleted = true,
            isPermissionGranted = true,
            accessMode = StorageAccessMode.ALL_MEDIA
        )
        fakeVideoRepository.setFolders(
            listOf(
                VideoFolder(
                    folderPath = "/storage/emulated/0/Movies",
                    folderName = "Movies",
                    videoCount = 12,
                    lastModified = 2000L
                )
            )
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StorageAccessMode.ALL_MEDIA, state.accessMode)
        assertTrue(state.isPermissionGranted)
        assertEquals(1, state.indexedFolders.size)
        assertEquals("Movies", state.indexedFolders.first().displayName)
        assertEquals(12, state.indexedFolders.first().videoCount)
        assertFalse(state.indexedFolders.first().isSafFolder)
    }

    @Test
    fun initialState_selectedFoldersMode_loadsFoldersFromStorageAccessRepository() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val folderUri = "content://com.android.externalstorage.documents/tree/primary%3AMovies"
        fakeStorageAccessRepository.state.value = StorageAccessState(
            isOnboardingCompleted = true,
            isPermissionGranted = true,
            accessMode = StorageAccessMode.SELECTED_FOLDERS,
            selectedFolderUris = setOf(folderUri)
        )
        fakeVideoRepository.setFolders(
            listOf(
                VideoFolder(
                    folderPath = folderUri,
                    folderName = "Movies",
                    videoCount = 5,
                    lastModified = 1000L
                )
            )
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StorageAccessMode.SELECTED_FOLDERS, state.accessMode)
        assertEquals(1, state.indexedFolders.size)
        assertEquals(folderUri, state.indexedFolders.first().uriOrPath)
        assertTrue(state.indexedFolders.first().isSafFolder)
        assertEquals(5, state.indexedFolders.first().videoCount)
    }

    @Test
    fun requestRemoveFolder_setsPendingFolder_andDismissClearsIt() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val folder = StorageFolderItem(
            uriOrPath = "content://test/folder",
            displayName = "Test Folder",
            isSafFolder = true,
            videoCount = 3,
            isAccessible = true
        )

        viewModel.requestRemoveFolder(folder)
        testScheduler.advanceUntilIdle()
        assertEquals(folder, viewModel.uiState.value.folderPendingRemoval)

        viewModel.dismissRemoveFolder()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.folderPendingRemoval)
    }

    @Test
    fun confirmRemoveFolder_removesFromStorageAccessRepository_andDeletesVideosIfRequested() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val folderUri = "content://com.android.externalstorage.documents/tree/primary%3AMovies"
        fakeStorageAccessRepository.state.value = StorageAccessState(
            accessMode = StorageAccessMode.SELECTED_FOLDERS,
            selectedFolderUris = setOf(folderUri)
        )
        fakeVideoRepository.videosInFolder[folderUri] = mutableListOf("vid1", "vid2")

        val folder = StorageFolderItem(
            uriOrPath = folderUri,
            displayName = "Movies",
            isSafFolder = true,
            videoCount = 2,
            isAccessible = true
        )

        viewModel.requestRemoveFolder(folder)
        viewModel.confirmRemoveFolder(deleteVideos = true)
        testScheduler.advanceUntilIdle()

        assertFalse(fakeStorageAccessRepository.state.value.selectedFolderUris.contains(folderUri))
        assertEquals(0, fakeVideoRepository.videosInFolder[folderUri]?.size ?: 0)
        assertNull(viewModel.uiState.value.folderPendingRemoval)
        assertNotNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun confirmRemoveFolder_preservesVideosWhenDeleteNotRequested() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val folderUri = "content://com.android.externalstorage.documents/tree/primary%3AMovies"
        fakeStorageAccessRepository.state.value = StorageAccessState(
            accessMode = StorageAccessMode.SELECTED_FOLDERS,
            selectedFolderUris = setOf(folderUri)
        )
        fakeVideoRepository.videosInFolder[folderUri] = mutableListOf("vid1", "vid2")

        val folder = StorageFolderItem(
            uriOrPath = folderUri,
            displayName = "Movies",
            isSafFolder = true,
            videoCount = 2,
            isAccessible = true
        )

        viewModel.requestRemoveFolder(folder)
        viewModel.confirmRemoveFolder(deleteVideos = false)
        testScheduler.advanceUntilIdle()

        assertFalse(fakeStorageAccessRepository.state.value.selectedFolderUris.contains(folderUri))
        // Videos are preserved
        assertEquals(2, fakeVideoRepository.videosInFolder[folderUri]?.size)
    }

    @Test
    fun addExcludedFolder_updatesExcludedFoldersState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.addExcludedFolder("/storage/emulated/0/Movies/Private")
        testScheduler.advanceUntilIdle()

        assertTrue(fakeStorageAccessRepository.state.value.excludedFolderPaths.contains("/storage/emulated/0/Movies/Private"))
        assertTrue(fakeSettingsRepository.settings.value.library.excludedFolders.contains("/storage/emulated/0/Movies/Private"))
        assertTrue(viewModel.uiState.value.excludedFolders.contains("/storage/emulated/0/Movies/Private"))
    }

    @Test
    fun removeExcludedFolder_restoresEligibility() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val path = "/storage/emulated/0/Movies/Private"
        viewModel.addExcludedFolder(path)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.excludedFolders.contains(path))

        viewModel.removeExcludedFolder(path)
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.excludedFolders.contains(path))
    }

    @Test
    fun triggerLocationScan_delegatesToOrchestrator() = runTest(testDispatcher) {
        viewModel.triggerLocationScan("/storage/emulated/0/Movies")
        assertEquals("/storage/emulated/0/Movies", fakeMediaScanOrchestrator.lastScannedLocation)
    }

    @Test
    fun cancelScan_delegatesToOrchestrator() = runTest(testDispatcher) {
        viewModel.cancelScan()
        assertTrue(fakeMediaScanOrchestrator.wasCancelCalled)
    }

    // --- Fakes ---

    private class FakeStorageAccessRepository : StorageAccessRepository {
        val state = MutableStateFlow(StorageAccessState())
        override val storageAccessState: Flow<StorageAccessState> = state.asStateFlow()

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            state.value = state.value.copy(isOnboardingCompleted = completed)
        }

        override suspend fun setStorageAccessMode(mode: StorageAccessMode) {
            state.value = state.value.copy(accessMode = mode)
        }

        override suspend fun addSelectedFolderUri(uriString: String) {
            state.value = state.value.copy(selectedFolderUris = state.value.selectedFolderUris + uriString)
        }

        override suspend fun removeSelectedFolderUri(uriString: String) {
            state.value = state.value.copy(selectedFolderUris = state.value.selectedFolderUris - uriString)
        }

        override suspend fun clearSelectedFolders() {
            state.value = state.value.copy(selectedFolderUris = emptySet())
        }

        override suspend fun addExcludedFolder(folderPath: String) {
            state.value = state.value.copy(excludedFolderPaths = state.value.excludedFolderPaths + folderPath)
        }

        override suspend fun removeExcludedFolder(folderPath: String) {
            state.value = state.value.copy(excludedFolderPaths = state.value.excludedFolderPaths - folderPath)
        }

        override suspend fun clearExcludedFolders() {
            state.value = state.value.copy(excludedFolderPaths = emptySet())
        }

        override fun isPermissionGranted(): Boolean = state.value.isPermissionGranted
        override fun getRequiredPermissions(): List<String> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(NexusSettings())
        override val settings: StateFlow<NexusSettings> = _settings.asStateFlow()

        override suspend fun addExcludedFolder(folderPath: String) {
            _settings.value = _settings.value.copy(
                library = _settings.value.library.copy(
                    excludedFolders = _settings.value.library.excludedFolders + folderPath
                )
            )
        }

        override suspend fun removeExcludedFolder(folderPath: String) {
            _settings.value = _settings.value.copy(
                library = _settings.value.library.copy(
                    excludedFolders = _settings.value.library.excludedFolders - folderPath
                )
            )
        }
    }

    private class FakeMediaScanOrchestrator : MediaScanOrchestrator {
        private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
        override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
        override val isScanning: StateFlow<Boolean> = MutableStateFlow(false)

        var lastScannedLocation: String? = null
        var wasCancelCalled: Boolean = false

        override fun triggerStartupScan(): Boolean = true
        override fun triggerManualScan(): Boolean = true
        override fun triggerIncrementalScan(): Boolean = true
        override fun triggerLocationScan(folderUriOrPath: String): Boolean {
            lastScannedLocation = folderUriOrPath
            return true
        }
        override fun cancelScan() {
            wasCancelCalled = true
        }
    }

    private class FakeVideoRepository : VideoRepository {
        private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
        val videosInFolder = mutableMapOf<String, MutableList<String>>()

        fun setFolders(folders: List<VideoFolder>) {
            _folders.value = folders
        }

        override fun getFolders(): Flow<List<VideoFolder>> = _folders

        override suspend fun deleteVideosInFolder(folderPath: String) {
            videosInFolder[folderPath]?.clear()
        }

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = emptyFlow()
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = emptyFlow()
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = emptyFlow()
        override suspend fun getVideoById(id: String): Video? = null
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = 0
        override suspend fun insertVideo(video: Video): Long = 0L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }
}
