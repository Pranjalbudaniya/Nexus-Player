package com.nexus.player.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderSortOrder
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.feature.library.folder.FolderViewModel
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
class FolderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var folderRepository: FakeFolderRepository
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var thumbnailLoader: FakeThumbnailLoader
    private lateinit var fileOperationsManager: FakeVideoFileOperationsManager
    private lateinit var viewModel: FolderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        folderRepository = FakeFolderRepository()
        preferencesRepository = FakePreferencesRepository()
        thumbnailLoader = FakeThumbnailLoader()
        fileOperationsManager = FakeVideoFileOperationsManager()

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "folderPath" to "/storage/emulated/0/Movies/Action",
                "folderName" to "Action"
            )
        )

        viewModel = FolderViewModel(
            savedStateHandle = savedStateHandle,
            folderRepository = folderRepository,
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
    fun initialState_loadsFolderPathAndName() = runTest {
        assertEquals("/storage/emulated/0/Movies/Action", viewModel.folderPath)
        assertEquals("Action", viewModel.folderName)

        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertEquals("/storage/emulated/0/Movies/Action", state.folderPath)
        assertEquals("Action", state.folderName)
        assertEquals(LibraryLayoutMode.GRID, state.layoutMode)
    }

    @Test
    fun loadsSubfoldersAndContainedVideos() = runTest {
        val subfolder = VideoFolder(
            folderPath = "/storage/emulated/0/Movies/Action/SciFi",
            folderName = "SciFi",
            videoCount = 3,
            parentPath = "/storage/emulated/0/Movies/Action"
        )
        val video = createSampleVideo(id = "v1", title = "The Matrix")

        folderRepository.setChildFolders(listOf(subfolder))
        folderRepository.setVideosInFolder(listOf(video))

        val state = viewModel.uiState.first { it.subfolders.isNotEmpty() && it.videos.isNotEmpty() }
        assertFalse(state.isEmpty)
        assertEquals(1, state.subfolders.size)
        assertEquals("SciFi", state.subfolders.first().folderName)
        assertEquals(1, state.videos.size)
        assertEquals("The Matrix", state.videos.first().title)
    }

    @Test
    fun emptyState_whenNoSubfoldersAndNoVideos() = runTest {
        folderRepository.setChildFolders(emptyList())
        folderRepository.setVideosInFolder(emptyList())

        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isEmpty)
        assertEquals(0, state.subfolders.size)
        assertEquals(0, state.videos.size)
    }

    @Test
    fun layoutModeSwitch_updatesPreference() = runTest {
        viewModel.setLayoutMode(LibraryLayoutMode.LIST)

        val state = viewModel.uiState.first { it.layoutMode == LibraryLayoutMode.LIST }
        assertEquals(LibraryLayoutMode.LIST, state.layoutMode)
        assertEquals(LibraryLayoutMode.LIST, preferencesRepository.layoutMode.value)
    }

    @Test
    fun sortOptionUpdate_queriesContainedVideos() = runTest {
        viewModel.setSortOption(
            LibrarySortOption(field = LibrarySortField.NAME, direction = LibrarySortDirection.ASCENDING)
        )

        val state = viewModel.uiState.first { it.sortOption.field == LibrarySortField.NAME }
        assertEquals(LibrarySortField.NAME, state.sortOption.field)
        assertEquals(LibrarySortDirection.ASCENDING, state.sortOption.direction)
        assertEquals(VideoSortOrder.TITLE_ASC, folderRepository.lastRequestedVideoSortOrder)
    }

    @Test
    fun videoLongClick_andDismissContextMenu() = runTest {
        val video = createSampleVideo(id = "v2", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))

        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()
        viewModel.onVideoLongClick(metadata)

        val stateWithMenu = viewModel.uiState.first { it.selectedVideoForMenu != null }
        assertEquals("Die Hard", stateWithMenu.selectedVideoForMenu?.title)

        viewModel.dismissContextMenu()
        val stateWithoutMenu = viewModel.uiState.first { it.selectedVideoForMenu == null }
        assertNull(stateWithoutMenu.selectedVideoForMenu)
    }

    @Test
    fun sortSheetVisibility_canBeToggled() = runTest {
        assertFalse(viewModel.uiState.value.isSortSheetVisible)

        viewModel.showSortSheet(true)
        val visible = viewModel.uiState.first { it.isSortSheetVisible }
        assertTrue(visible.isSortSheetVisible)

        viewModel.showSortSheet(false)
        val hidden = viewModel.uiState.first { !it.isSortSheetVisible }
        assertFalse(hidden.isSortSheetVisible)
    }

    @Test
    fun toggleFavorite_callsFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        viewModel.toggleFavorite(metadata)
        assertEquals(true, fileOperationsManager.favoriteVideos["v1"])
    }

    @Test
    fun renameVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.renameVideo(metadata, "Die Harder") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "Die Harder", fileOperationsManager.renamedVideos.first())
    }

    @Test
    fun moveVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.moveVideo(metadata, "/storage/emulated/0/Movies/Classics") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "/storage/emulated/0/Movies/Classics", fileOperationsManager.movedVideos.first())
    }

    @Test
    fun copyVideo_delegatesToFileOperationsManager() = runTest {
        val video = createSampleVideo(id = "v1", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))
        val metadata = viewModel.uiState.first { it.videos.isNotEmpty() }.videos.first()

        var callbackInvoked = false
        viewModel.copyVideo(metadata, "/storage/emulated/0/Movies/Classics") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }

        assertTrue(callbackInvoked)
        assertEquals("v1" to "/storage/emulated/0/Movies/Classics", fileOperationsManager.copiedVideos.first())
    }

    @Test
    fun deleteVideo_stagesAndRemovesFromQueue() = runTest {
        val video = createSampleVideo(id = "v1", title = "Die Hard")
        folderRepository.setVideosInFolder(listOf(video))
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
        filePath = "/storage/emulated/0/Movies/Action/$id.mp4",
        fileName = "$id.mp4",
        title = title,
        folderName = "Action",
        folderPath = "/storage/emulated/0/Movies/Action",
        sizeBytes = 2_000_000_000L,
        durationMs = 8_000_000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        videoCodec = "H.264",
        audioCodec = "AAC",
        dateAdded = 2000L,
        lastModified = 2000L
    )

    private class FakeFolderRepository : FolderRepository {
        private val _childFoldersFlow = MutableStateFlow<List<VideoFolder>>(emptyList())
        private val _videosInFolderFlow = MutableStateFlow<List<Video>>(emptyList())
        var lastRequestedVideoSortOrder: VideoSortOrder? = null

        fun setChildFolders(folders: List<VideoFolder>) {
            _childFoldersFlow.value = folders
        }

        fun setVideosInFolder(videos: List<Video>) {
            _videosInFolderFlow.value = videos
        }

        override fun getFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = flowOf(emptyList())
        override fun getRootFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = flowOf(emptyList())
        override fun getChildFolders(parentFolderPath: String, sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = _childFoldersFlow.asStateFlow()
        override fun getFolderByPath(folderPath: String): Flow<VideoFolder?> = flowOf(null)

        override fun getVideosInFolder(folderPath: String, sortOrder: VideoSortOrder): Flow<List<Video>> {
            lastRequestedVideoSortOrder = sortOrder
            return _videosInFolderFlow.asStateFlow()
        }
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
