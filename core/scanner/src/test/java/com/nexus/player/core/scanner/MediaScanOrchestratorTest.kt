package com.nexus.player.core.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.scanner.extractor.VideoMetadataExtractor
import com.nexus.player.core.scanner.model.ScanResult
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestratorImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
class MediaScanOrchestratorTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeStorageRepository: FakeStorageAccessRepository
    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var fakeMediaStoreScanner: FakeMediaStoreScanner
    private lateinit var fakeSafScanner: FakeSafFolderScanner
    private lateinit var metadataExtractor: VideoMetadataExtractor
    private lateinit var mediaScanner: MediaScannerImpl
    private lateinit var orchestrator: MediaScanOrchestrator

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

        orchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = mediaScanner,
            applicationScope = testScope,
            ioDispatcher = testDispatcher
        )
    }

    private fun sampleItem(
        id: String,
        size: Long = 1000L,
        lastModified: Long = 1000L
    ) = com.nexus.player.core.scanner.datasource.DiscoveredMediaItem(
        id = id,
        mediaUri = "content://media/external/video/media/$id",
        filePath = "/storage/Movies/$id.mp4",
        fileName = "$id.mp4",
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
    fun startupScanTrigger() = runTest(testDispatcher) {
        fakeMediaStoreScanner.itemsToEmit = listOf(sampleItem("startup_1"))

        val triggered = orchestrator.triggerStartupScan()
        assertTrue(triggered)
        assertTrue(orchestrator.scanState.value is ScanState.Completed)
        assertEquals(1, fakeVideoRepository.getVideosCount())
    }

    @Test
    fun noDuplicateConcurrentScans() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScanner = ControllableMediaScanner()
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = controlledScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher
        )

        // Trigger first scan
        val firstTriggered = customOrchestrator.triggerStartupScan()
        assertTrue(firstTriggered)
        testScheduler.runCurrent()

        // Attempting to trigger second scan while first is running must return false
        val secondTriggered = customOrchestrator.triggerStartupScan()
        assertFalse(secondTriggered)

        // Manual scan trigger while running must also be rejected
        val manualTriggered = customOrchestrator.triggerManualScan()
        assertFalse(manualTriggered)

        // Allow first scan to complete
        controlledScanner.complete()
        testScheduler.advanceUntilIdle()

        // Now subsequent trigger succeeds
        val postTrigger = customOrchestrator.triggerManualScan()
        assertTrue(postTrigger)
        controlledScanner.complete()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun accessUnavailableDoesNotCrash() = runTest(testDispatcher) {
        // Storage access not granted
        fakeStorageRepository.state.value = StorageAccessState(
            isOnboardingCompleted = false,
            isPermissionGranted = false,
            accessMode = StorageAccessMode.ALL_MEDIA
        )

        val triggered = orchestrator.triggerStartupScan()
        assertTrue(triggered) // Launched successfully without crashing
        // But scan immediately finishes without adding videos
        assertEquals(0, fakeVideoRepository.getVideosCount())
    }

    @Test
    fun scanCancellation() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScanner = ControllableMediaScanner()
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = controlledScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher
        )

        customOrchestrator.triggerStartupScan()
        testScheduler.runCurrent()

        customOrchestrator.cancelScan()
        testScheduler.advanceUntilIdle()

        assertEquals(ScanState.Cancelled, customOrchestrator.scanState.value)
        assertFalse(customOrchestrator.isScanning.value)
    }

    @Test
    fun scannerFailureContainment() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val errorScanner = FailingMediaScanner()
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = errorScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher
        )

        customOrchestrator.triggerStartupScan()
        testScheduler.advanceUntilIdle()

        assertTrue(customOrchestrator.scanState.value is ScanState.Error)
        assertFalse(customOrchestrator.isScanning.value)
    }

    @Test
    fun newlyDiscoveredFiles() = runTest(testDispatcher) {
        fakeMediaStoreScanner.itemsToEmit = listOf(
            sampleItem("new_1"),
            sampleItem("new_2")
        )

        orchestrator.triggerStartupScan()

        assertEquals(2, fakeVideoRepository.getVideosCount())
        assertNotNull(fakeVideoRepository.getVideoById("new_1"))
        assertNotNull(fakeVideoRepository.getVideoById("new_2"))
    }

    @Test
    fun modifiedFilesUpdateMetadata() = runTest(testDispatcher) {
        val initial = Video(
            id = "mod_1",
            mediaUri = "content://media/external/video/media/mod_1",
            filePath = "/storage/Movies/mod_1.mp4",
            fileName = "mod_1.mp4",
            title = "Old Title",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 50000L,
            width = 1280,
            height = 720,
            resolutionLabel = "720p",
            dateAdded = 100L,
            lastModified = 100L
        )
        fakeVideoRepository.upsertVideo(initial)

        // Discover modified file with new size and timestamp
        fakeMediaStoreScanner.itemsToEmit = listOf(
            sampleItem("mod_1", size = 2500L, lastModified = 5000L)
        )

        orchestrator.triggerStartupScan()

        val updated = fakeVideoRepository.getVideoById("mod_1")
        assertNotNull(updated)
        assertEquals(2500L, updated?.sizeBytes)
        assertEquals(5000L, updated?.lastModified)
    }

    @Test
    fun removedFilesPruning() = runTest(testDispatcher) {
        fakeVideoRepository.upsertVideos(
            listOf(
                Video(
                    id = "active_1", mediaUri = "uri_1", filePath = null, fileName = "1.mp4",
                    title = "1", folderName = "Movies", folderPath = "/Movies", sizeBytes = 10L,
                    durationMs = 10L, width = 10, height = 10, resolutionLabel = "SD",
                    dateAdded = 1L, lastModified = 1L
                ),
                Video(
                    id = "stale_2", mediaUri = "uri_2", filePath = null, fileName = "2.mp4",
                    title = "2", folderName = "Movies", folderPath = "/Movies", sizeBytes = 10L,
                    durationMs = 10L, width = 10, height = 10, resolutionLabel = "SD",
                    dateAdded = 1L, lastModified = 1L
                )
            )
        )
        assertEquals(2, fakeVideoRepository.getVideosCount())

        // Scanner now only discovers active_1
        fakeMediaStoreScanner.itemsToEmit = listOf(sampleItem("active_1"))

        orchestrator.triggerStartupScan()

        assertEquals(1, fakeVideoRepository.getVideosCount())
        assertNotNull(fakeVideoRepository.getVideoById("active_1"))
        assertNull(fakeVideoRepository.getVideoById("stale_2"))
    }

    @Test
    fun preservationOfUserSpecificMetadata() = runTest(testDispatcher) {
        // Pre-populate with user favorite, watch count, and playback progress
        val userVideo = Video(
            id = "user_media",
            mediaUri = "content://media/external/video/media/user_media",
            filePath = "/storage/Movies/user_media.mp4",
            fileName = "user_media.mp4",
            title = "User Media",
            folderName = "Movies",
            folderPath = "/storage/Movies",
            sizeBytes = 1000L,
            durationMs = 50000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = 100L,
            lastModified = 100L,
            isFavorite = true,
            playbackPositionMs = 35000L,
            playbackPercentage = 0.70f,
            watchCount = 3,
            lastPlayedAt = 9999L
        )
        fakeVideoRepository.upsertVideo(userVideo)

        // Rescan with updated file timestamp
        fakeMediaStoreScanner.itemsToEmit = listOf(
            sampleItem("user_media", size = 1200L, lastModified = 200L)
        )

        orchestrator.triggerStartupScan()

        val preserved = fakeVideoRepository.getVideoById("user_media")
        assertNotNull(preserved)
        assertTrue("isFavorite should be preserved", preserved?.isFavorite == true)
        assertEquals(35000L, preserved?.playbackPositionMs)
        assertEquals(0.70f, preserved?.playbackPercentage ?: 0f, 0.001f)
        assertEquals(3, preserved?.watchCount)
        assertEquals(9999L, preserved?.lastPlayedAt)
    }

    @Test
    fun largeLibraryIncrementalBatches() = runTest(testDispatcher) {
        // 110 items to trigger batching
        val largeList = (1..110).map { sampleItem("batch_item_$it") }
        fakeMediaStoreScanner.itemsToEmit = largeList

        orchestrator.triggerStartupScan()

        assertEquals(110, fakeVideoRepository.getVideosCount())
        assertEquals(listOf(50, 50, 10), fakeVideoRepository.upsertBatchSizes)
    }

    @Test
    fun startupScanSkippedWhenDisabledInSettings() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScanner = ControllableMediaScanner()
        val fakeSettings = FakeOrchestratorSettingsRepository(
            NexusSettings(
                library = com.nexus.player.core.common.settings.model.LibrarySettings(
                    scanOnAppLaunch = false
                )
            )
        )
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = controlledScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher,
            settingsRepository = fakeSettings
        )

        val triggered = customOrchestrator.triggerStartupScan()
        assertTrue(triggered)
        testScheduler.advanceUntilIdle()

        assertNull("Media scanner should not be called when scanOnAppLaunch is false", controlledScanner.lastScanOptions)
        assertEquals(ScanState.Idle, controlledScanner.scanState.value)
    }

    @Test
    fun incrementalScanPassesIncrementalOption() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScanner = ControllableMediaScanner()
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = controlledScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher
        )

        val triggered = customOrchestrator.triggerIncrementalScan()
        assertTrue(triggered)
        testScheduler.runCurrent()

        assertNotNull(controlledScanner.lastScanOptions)
        assertTrue(controlledScanner.lastScanOptions?.isIncremental == true)

        controlledScanner.complete()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun locationScanPassesTargetLocationOption() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScanner = ControllableMediaScanner()
        val customOrchestrator = MediaScanOrchestratorImpl(
            storageAccessRepository = fakeStorageRepository,
            mediaScanner = controlledScanner,
            applicationScope = CoroutineScope(standardDispatcher),
            ioDispatcher = standardDispatcher
        )

        val targetPath = "/storage/Movies/Action"
        val triggered = customOrchestrator.triggerLocationScan(targetPath)
        assertTrue(triggered)
        testScheduler.runCurrent()

        assertNotNull(controlledScanner.lastScanOptions)
        assertEquals(targetPath, controlledScanner.lastScanOptions?.targetFolderUriOrPath)

        controlledScanner.complete()
        testScheduler.advanceUntilIdle()
    }
}

// --- Controllable and Failing Scanner Fakes for Orchestrator Tests ---

class ControllableMediaScanner : MediaScanner {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private var completionDeferred = CompletableDeferred<Unit>()
    var lastScanOptions: com.nexus.player.core.scanner.model.ScanOptions? = null

    override suspend fun startScan(options: com.nexus.player.core.scanner.model.ScanOptions): ScanResult {
        lastScanOptions = options
        _scanState.value = ScanState.Scanning()
        completionDeferred.await()
        val result = ScanResult(0, 0, 0, 0, 10L)
        _scanState.value = ScanState.Completed(result)
        return result
    }

    fun complete() {
        completionDeferred.complete(Unit)
        completionDeferred = CompletableDeferred()
    }

    override fun cancelScan() {
        _scanState.value = ScanState.Cancelled
        completionDeferred.cancel(CancellationException("Cancelled"))
        completionDeferred = CompletableDeferred()
    }
}

class FailingMediaScanner : MediaScanner {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    override suspend fun startScan(options: com.nexus.player.core.scanner.model.ScanOptions): ScanResult {
        _scanState.value = ScanState.Scanning()
        val error = RuntimeException("Disk unreadable")
        _scanState.value = ScanState.Error("Media scan failed", error)
        throw error
    }

    override fun cancelScan() {
        _scanState.value = ScanState.Cancelled
    }
}

class FakeOrchestratorSettingsRepository(
    initialSettings: com.nexus.player.core.common.settings.model.NexusSettings = com.nexus.player.core.common.settings.model.NexusSettings()
) : com.nexus.player.core.common.settings.SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<com.nexus.player.core.common.settings.model.NexusSettings> = _settings.asStateFlow()
}
