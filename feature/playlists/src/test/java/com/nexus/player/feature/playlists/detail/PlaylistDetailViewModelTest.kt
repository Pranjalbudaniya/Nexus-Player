package com.nexus.player.feature.playlists.detail

import androidx.lifecycle.SavedStateHandle
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.feature.playlists.FakePlaylistRepository
import com.nexus.player.feature.playlists.FakeVideoFileOperationsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class PlaylistDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePlaylistRepository
    private lateinit var fakeQueueManager: PlaybackQueueManagerImpl
    private lateinit var fakeFileOperationsManager: FakeVideoFileOperationsManager
    private lateinit var viewModel: PlaylistDetailViewModel
    private val testPlaylistId = "pl_detail_1"

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePlaylistRepository()
        fakeQueueManager = PlaybackQueueManagerImpl()
        fakeFileOperationsManager = FakeVideoFileOperationsManager()

        // Create initial playlist and add items
        fakeRepository.createPlaylist("Favorites")
        val playlist = fakeRepository.getPlaylists().first()
        fakeRepository.addVideoToPlaylist(playlist.id, "vid_1")
        fakeRepository.addVideoToPlaylist(playlist.id, "vid_2")
        fakeRepository.addVideoToPlaylist(playlist.id, "vid_3")

        viewModel = PlaylistDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("playlistId" to playlist.id)),
            playlistRepository = fakeRepository,
            playbackQueueManager = fakeQueueManager,
            fileOperationsManager = fakeFileOperationsManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPlaylistAndItemsCorrectly() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.playlist)
        assertEquals("Favorites", state.playlist?.name)
        assertEquals(3, state.items.size)
        assertEquals(listOf("vid_1", "vid_2", "vid_3"), state.playableVideoIds)

        collectJob.cancel()
    }

    @Test
    fun playAll_primesQueueManagerWithPlaylistSource() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var startedVideoId: String? = null
        viewModel.playAll { videoId ->
            startedVideoId = videoId
        }

        assertEquals("vid_1", startedVideoId)
        val queue = fakeQueueManager.queueState.value
        assertEquals("vid_1", queue.currentVideoId)
        assertEquals(3, queue.size)
        assertTrue(queue.source is QueueSource.Playlist)
        assertEquals("Favorites", (queue.source as QueueSource.Playlist).playlistName)

        collectJob.cancel()
    }

    @Test
    fun shufflePlay_enablesShuffleAndStartsPlayback() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var startedVideoId: String? = null
        viewModel.shufflePlay { videoId ->
            startedVideoId = videoId
        }

        assertNotNull(startedVideoId)
        val queue = fakeQueueManager.queueState.value
        assertTrue(queue.isShuffleEnabled)
        assertTrue(queue.source is QueueSource.Playlist)

        collectJob.cancel()
    }

    @Test
    fun playVideo_startsAtSpecificQueueItem() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var startedVideoId: String? = null
        viewModel.playVideo("vid_2") { videoId ->
            startedVideoId = videoId
        }

        assertEquals("vid_2", startedVideoId)
        val queue = fakeQueueManager.queueState.value
        assertEquals("vid_2", queue.currentVideoId)
        assertEquals(1, queue.currentIndex)

        collectJob.cancel()
    }

    @Test
    fun removeVideoAndUndo_modifiesAndRestoresItems() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.removeVideo("vid_2")
        advanceUntilIdle()

        assertEquals(listOf("vid_1", "vid_3"), viewModel.uiState.value.playableVideoIds)

        viewModel.undoRemoveVideo()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.items.size)

        collectJob.cancel()
    }

    @Test
    fun moveItem_updatesPersistentOrder() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Move item at 0 (vid_1) to index 2
        viewModel.moveItem(fromIndex = 0, toIndex = 2)
        advanceUntilIdle()

        assertEquals(listOf("vid_2", "vid_3", "vid_1"), viewModel.uiState.value.playableVideoIds)

        collectJob.cancel()
    }

    @Test
    fun contextMenu_selectsAndDismissesVideo() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        assertNull(viewModel.uiState.value.selectedVideoForMenu)

        viewModel.onVideoLongClick(sampleMetadata)
        advanceUntilIdle()

        assertEquals(sampleMetadata, viewModel.uiState.value.selectedVideoForMenu)

        viewModel.dismissContextMenu()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedVideoForMenu)

        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_delegatesToFileOperationsManager() = runTest {
        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        viewModel.toggleFavorite(sampleMetadata)
        advanceUntilIdle()

        assertEquals(true, fakeFileOperationsManager.favoriteVideos["vid_1"])
    }

    @Test
    fun renameVideo_delegatesToFileOperationsManager() = runTest {
        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        var callbackInvoked = false
        viewModel.renameVideo(sampleMetadata, "NewVid") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("vid_1" to "NewVid", fakeFileOperationsManager.renamedVideos.first())
    }

    @Test
    fun moveVideo_delegatesToFileOperationsManager() = runTest {
        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        var callbackInvoked = false
        viewModel.moveVideo(sampleMetadata, "/Movies/Archive") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("vid_1" to "/Movies/Archive", fakeFileOperationsManager.movedVideos.first())
    }

    @Test
    fun copyVideo_delegatesToFileOperationsManager() = runTest {
        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        var callbackInvoked = false
        viewModel.copyVideo(sampleMetadata, "/Movies/Backup") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("vid_1" to "/Movies/Backup", fakeFileOperationsManager.copiedVideos.first())
    }

    @Test
    fun deleteVideo_delegatesToFileOperationsManagerAndRemovesFromQueue() = runTest {
        val sampleMetadata = MediaMetadata(
            id = "vid_1",
            mediaUri = "file:///Movies/vid_1.mp4",
            filePath = "/Movies/vid_1.mp4",
            fileName = "vid_1.mp4",
            title = "vid_1",
            folderName = "Movies",
            folderPath = "/Movies",
            formattedDuration = "01:00",
            durationMs = 60000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2000000L,
            formattedSize = "10 MB",
            sizeBytes = 10000000L,
            formattedModifiedDate = "Today",
            lastModified = 1000L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.playAll {}
        advanceUntilIdle()
        assertEquals(3, fakeQueueManager.queueState.value.size)

        var callbackInvoked = false
        viewModel.deleteVideo(sampleMetadata) { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(listOf("vid_1"), fakeFileOperationsManager.deletedVideos)
        assertEquals(2, fakeQueueManager.queueState.value.size)

        collectJob.cancel()
    }

    @Test
    fun restoreDeletedVideo_delegatesToFileOperationsManager() = runTest {
        var callbackInvoked = false
        viewModel.restoreDeletedVideo("vid_1") { result ->
            callbackInvoked = true
            assertTrue(result.isSuccess)
        }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(listOf("vid_1"), fakeFileOperationsManager.restoredVideos)
    }
}
