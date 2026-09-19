package com.nexus.player.core.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.scanner.datasource.DiscoveredMediaItem
import com.nexus.player.core.scanner.datasource.MediaStoreScanner
import com.nexus.player.core.scanner.datasource.SafFolderScanner
import com.nexus.player.core.scanner.extractor.VideoMetadataExtractor
import com.nexus.player.core.scanner.model.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MediaScannerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeStorageRepository: FakeStorageAccessRepository
    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var fakeMediaStoreScanner: FakeMediaStoreScanner
    private lateinit var fakeSafScanner: FakeSafFolderScanner
    private lateinit var metadataExtractor: VideoMetadataExtractor
    private lateinit var mediaScanner: MediaScanner

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeStorageRepository = FakeStorageAccessRepository()
        fakeVideoRepository = FakeVideoRepository()
        fakeMediaStoreScanner = FakeMediaStoreScanner()
        fakeSafScanner = FakeSafFolderScanner()
        metadataExtractor = VideoMetadataExtractor(context)

        mediaScanner = MediaScannerImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaStoreScanner = fakeMediaStoreScanner,
            safFolderScanner = fakeSafScanner,
            metadataExtractor = metadataExtractor,
            videoRepository = fakeVideoRepository,
            ioDispatcher = testDispatcher
        )
    }

    private fun createDiscoveredItem(
        id: String,
        uri: String = "content://media/external/video/media/$id",
        name: String = "$id.mp4",
        size: Long = 1024L * 1024L,
        lastModified: Long = 1000L
    ) = DiscoveredMediaItem(
        id = id,
        mediaUri = uri,
        filePath = "/storage/Movies/$name",
        fileName = name,
        title = id,
        folderName = "Movies",
        folderPath = "/storage/Movies",
        sizeBytes = size,
        durationMs = 60000L,
        width = 1920,
        height = 1080,
        dateAdded = 1000L,
        lastModified = lastModified,
        mimeType = "video/mp4"
    )

    @Test
    fun emptyLibraryScan() = runTest(testDispatcher) {
        fakeMediaStoreScanner.itemsToEmit = emptyList()

        val result = mediaScanner.startScan()
        assertEquals(0, result.totalScanned)
        assertEquals(0, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(0, result.removed)
        assertTrue(mediaScanner.scanState.value is ScanState.Completed)
    }

    @Test
    fun newFileInsertion() = runTest(testDispatcher) {
        val items = listOf(
            createDiscoveredItem("video_1"),
            createDiscoveredItem("video_2"),
            createDiscoveredItem("video_3")
        )
        fakeMediaStoreScanner.itemsToEmit = items

        val result = mediaScanner.startScan()
        assertEquals(3, result.totalScanned)
        assertEquals(3, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(0, result.removed)
        assertEquals(3, fakeVideoRepository.getVideosCount())
        assertNotNull(fakeVideoRepository.getVideoById("video_1"))
        assertNotNull(fakeVideoRepository.getVideoById("video_2"))
        assertNotNull(fakeVideoRepository.getVideoById("video_3"))
        assertTrue(mediaScanner.scanState.value is ScanState.Completed)
    }

    @Test
    fun duplicatePreventionInScanPass() = runTest(testDispatcher) {
        // Feed two items with identical mediaUri
        val item1 = createDiscoveredItem("video_1", uri = "content://media/same_uri")
        val item2WithSameUri = createDiscoveredItem("video_2", uri = "content://media/same_uri")
        fakeMediaStoreScanner.itemsToEmit = listOf(item1, item2WithSameUri)

        val result = mediaScanner.startScan()
        // Only 1 unique URI is scanned and inserted
        assertEquals(1, result.totalScanned)
        assertEquals(1, result.inserted)
        assertEquals(1, fakeVideoRepository.getVideosCount())
    }

    @Test
    fun existingFileUpdate() = runTest(testDispatcher) {
        // Pre-populate database with an existing video
        val existing = Video(
            id = "video_1",
            mediaUri = "content://media/external/video/media/video_1",
            filePath = "/storage/Movies/video_1.mp4",
            fileName = "video_1.mp4",
            title = "video_1",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 50000L,
            width = 1280,
            height = 720,
            resolutionLabel = "720p",
            dateAdded = 1000L,
            lastModified = 1000L,
            isFavorite = true,
            playbackPositionMs = 25000L,
            watchCount = 2
        )
        fakeVideoRepository.upsertVideo(existing)

        // Discovered item has newer lastModified timestamp and larger size
        val updatedItem = createDiscoveredItem("video_1", size = 2000L, lastModified = 5000L)
        fakeMediaStoreScanner.itemsToEmit = listOf(updatedItem)

        val result = mediaScanner.startScan()
        assertEquals(1, result.totalScanned)
        assertEquals(0, result.inserted)
        assertEquals(1, result.updated)

        val fetched = fakeVideoRepository.getVideoById("video_1")
        assertNotNull(fetched)
        assertEquals(2000L, fetched?.sizeBytes)
        assertEquals(5000L, fetched?.lastModified)
        // User state preserved across metadata update
        assertTrue(fetched?.isFavorite == true)
        assertEquals(25000L, fetched?.playbackPositionMs)
        assertEquals(2, fetched?.watchCount)
    }

    @Test
    fun removedInaccessibleMediaCleanup() = runTest(testDispatcher) {
        // Existing videos in database: video_1, video_2, video_stale
        fakeVideoRepository.upsertVideos(
            listOf(
                Video(
                    id = "video_1", mediaUri = "uri_1", filePath = null, fileName = "v1.mp4",
                    title = "V1", folderName = "Movies", folderPath = "/Movies", sizeBytes = 100L,
                    durationMs = 100L, width = 100, height = 100, resolutionLabel = "SD",
                    dateAdded = 1L, lastModified = 1L
                ),
                Video(
                    id = "video_stale", mediaUri = "uri_stale", filePath = null, fileName = "stale.mp4",
                    title = "Stale", folderName = "Movies", folderPath = "/Movies", sizeBytes = 100L,
                    durationMs = 100L, width = 100, height = 100, resolutionLabel = "SD",
                    dateAdded = 1L, lastModified = 1L
                )
            )
        )
        assertEquals(2, fakeVideoRepository.getVideosCount())

        // Discovered files on device now only contains video_1
        fakeMediaStoreScanner.itemsToEmit = listOf(
            createDiscoveredItem("video_1", uri = "uri_1")
        )

        val result = mediaScanner.startScan()
        assertEquals(1, result.totalScanned)
        assertEquals(1, result.removed)
        assertEquals(1, fakeVideoRepository.getVideosCount())
        assertNotNull(fakeVideoRepository.getVideoById("video_1"))
        assertNull(fakeVideoRepository.getVideoById("video_stale"))
    }

    @Test
    fun largeScanBatching() = runTest(testDispatcher) {
        // Create 125 items to verify 50-item incremental batching
        val items = (1..125).map { createDiscoveredItem("item_$it") }
        fakeMediaStoreScanner.itemsToEmit = items

        val result = mediaScanner.startScan()
        assertEquals(125, result.totalScanned)
        assertEquals(125, result.inserted)
        assertEquals(125, fakeVideoRepository.getVideosCount())

        // Verify batches were flushed: 50, 50, 25
        assertEquals(listOf(50, 50, 25), fakeVideoRepository.upsertBatchSizes)
    }

    @Test
    fun scanCancellation() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val customScanner = MediaScannerImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaStoreScanner = fakeMediaStoreScanner,
            safFolderScanner = fakeSafScanner,
            metadataExtractor = metadataExtractor,
            videoRepository = fakeVideoRepository,
            ioDispatcher = standardDispatcher
        )

        fakeMediaStoreScanner.itemsToEmit = (1..200).map { createDiscoveredItem("item_$it") }

        val job = launch(standardDispatcher) {
            try {
                customScanner.startScan()
            } catch (ignored: CancellationException) {
            }
        }

        testScheduler.runCurrent()
        customScanner.cancelScan()
        testScheduler.advanceUntilIdle()

        assertEquals(ScanState.Cancelled, customScanner.scanState.value)
    }
}

// --- Test Fakes ---

class FakeStorageAccessRepository : StorageAccessRepository {
    val state = MutableStateFlow(
        StorageAccessState(
            isOnboardingCompleted = true,
            isPermissionGranted = true,
            accessMode = StorageAccessMode.ALL_MEDIA
        )
    )

    override val storageAccessState: Flow<StorageAccessState> = state
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun setStorageAccessMode(mode: StorageAccessMode) {}
    override suspend fun addSelectedFolderUri(uriString: String) {}
    override suspend fun removeSelectedFolderUri(uriString: String) {}
    override suspend fun clearSelectedFolders() {}
    override fun isPermissionGranted(): Boolean = state.value.isPermissionGranted
    override fun getRequiredPermissions(): List<String> = emptyList()
}

class FakeMediaStoreScanner : MediaStoreScanner(ApplicationProvider.getApplicationContext()) {
    var itemsToEmit: List<DiscoveredMediaItem> = emptyList()

    override suspend fun scanMediaStore(onItemDiscovered: suspend (DiscoveredMediaItem) -> Unit): Int {
        for (item in itemsToEmit) {
            onItemDiscovered(item)
        }
        return itemsToEmit.size
    }
}

class FakeSafFolderScanner : SafFolderScanner(ApplicationProvider.getApplicationContext()) {
    var itemsToEmit: List<DiscoveredMediaItem> = emptyList()

    override suspend fun scanFolders(
        folderUris: Set<String>,
        onItemDiscovered: suspend (DiscoveredMediaItem) -> Unit
    ): Int {
        for (item in itemsToEmit) {
            onItemDiscovered(item)
        }
        return itemsToEmit.size
    }
}

class FakeVideoRepository : VideoRepository {
    private val videos = mutableMapOf<String, Video>()
    val upsertBatchSizes = mutableListOf<Int>()

    override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(videos.values.toList())
    override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = flowOf(videos.values.toList().take(limit))
    override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(videos.values.filter { it.isFavorite })
    override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = emptyFlow()
    override fun getHistoryVideos(limit: Int): Flow<List<Video>> = emptyFlow()
    override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = emptyFlow()
    override fun getFolders(): Flow<List<VideoFolder>> = emptyFlow()

    override suspend fun getVideoById(id: String): Video? = videos[id]
    override suspend fun getVideoByUri(mediaUri: String): Video? = videos.values.find { it.mediaUri == mediaUri }
    override suspend fun getVideosCount(): Int = videos.size

    override suspend fun insertVideo(video: Video): Long {
        videos[video.id] = video
        return 1L
    }

    override suspend fun upsertVideo(video: Video) {
        videos[video.id] = video
    }

    override suspend fun upsertVideos(videosList: List<Video>) {
        upsertBatchSizes.add(videosList.size)
        for (v in videosList) {
            videos[v.id] = v
        }
    }

    override suspend fun updatePlaybackProgress(id: String, positionMs: Long, percentage: Float, lastPlayedAt: Long) {
        videos[id]?.let {
            videos[id] = it.copy(playbackPositionMs = positionMs, playbackPercentage = percentage, lastPlayedAt = lastPlayedAt)
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        videos[id]?.let {
            videos[id] = it.copy(isFavorite = isFavorite)
        }
    }

    override suspend fun deleteVideo(id: String) {
        videos.remove(id)
    }

    override suspend fun deleteVideoByUri(mediaUri: String) {
        val entry = videos.entries.find { it.value.mediaUri == mediaUri }
        if (entry != null) videos.remove(entry.key)
    }

    override suspend fun deleteStaleVideos(validIds: List<String>) {
        val toRemove = videos.keys.filter { !validIds.contains(it) }
        for (k in toRemove) {
            videos.remove(k)
        }
    }

    override suspend fun clearAll() {
        videos.clear()
    }
}
