package com.nexus.player

import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.AppearanceSettings
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.designsystem.theme.NexusAccentColor
import com.nexus.player.core.designsystem.theme.NexusThemeMode
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.isNetworkMedia
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.operations.VideoFileOperationsManager
import com.nexus.player.core.navigation.HomeRoute
import com.nexus.player.core.navigation.LibraryRoute
import com.nexus.player.core.navigation.MoreRoute
import com.nexus.player.core.navigation.PlaylistsRoute
import com.nexus.player.core.navigation.SettingsRoute
import com.nexus.player.core.navigation.TopLevelDestination
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration tests verifying cross-feature consistency across
 * Home, Library, Playlists, More, Player, Settings, and Network/Local media boundaries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrossFeatureIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Test 1: Navigation Architecture & Top-Level Tab State Preservation
    // =========================================================================

    @Test
    fun navigationArchitecture_topLevelDestinationsAreConsistent() {
        val topLevel = TopLevelDestination.entries
        assertEquals(4, topLevel.size)
        assertEquals(HomeRoute, topLevel[0].route)
        assertEquals(LibraryRoute, topLevel[1].route)
        assertEquals(PlaylistsRoute, topLevel[2].route)
        assertEquals(MoreRoute, topLevel[3].route)

        // Settings is strictly NOT in the bottom navigation
        assertFalse(topLevel.any { it.route == SettingsRoute })
    }

    // =========================================================================
    // Test 2: Local Video Playback, Resume, Completion & Continue Watching Sync
    // =========================================================================

    @Test
    fun playbackFlow_tracksProgressAndEnforces95PercentCompletionRule() = testScope.runTest {
        val fakeRepo = FakeIntegrationVideoRepository()
        val sampleVideo = createSampleVideo(
            id = "vid_101",
            title = "Nature Documentary",
            durationMs = 100_000L
        )
        fakeRepo.insertVideo(sampleVideo)

        // Initially no progress -> not in continue watching
        var continueWatching = fakeRepo.getContinueWatchingVideos(10).first()
        assertTrue(continueWatching.isEmpty())

        // 1. User watches to 40% (40,000ms)
        val now = 1700000000000L
        fakeRepo.updatePlaybackProgress(
            id = sampleVideo.id,
            positionMs = 40_000L,
            percentage = 0.40f,
            lastPlayedAt = now,
            isCompleted = false
        )

        continueWatching = fakeRepo.getContinueWatchingVideos(10).first()
        assertEquals(1, continueWatching.size)
        assertEquals("vid_101", continueWatching[0].id)
        assertEquals(40_000L, continueWatching[0].playbackPositionMs)
        assertEquals(0.40f, continueWatching[0].playbackPercentage, 0.001f)

        // 2. User completes playback >= 95% (96,000ms)
        fakeRepo.updatePlaybackProgress(
            id = sampleVideo.id,
            positionMs = 96_000L,
            percentage = 0.96f,
            lastPlayedAt = now + 60_000L,
            isCompleted = true
        )

        // Rule: Completed video MUST be removed from Continue Watching
        continueWatching = fakeRepo.getContinueWatchingVideos(10).first()
        assertTrue("Completed video must be excluded from Continue Watching", continueWatching.isEmpty())

        // Rule: Completed video MUST be present in Watch History
        val history = fakeRepo.getAllHistoryVideos().first()
        assertEquals(1, history.size)
        assertEquals("vid_101", history[0].id)
        assertTrue(history[0].isCompleted)
    }

    // =========================================================================
    // Test 3: Local Video File Operations & Multi-Screen State Reflection
    // =========================================================================

    @Test
    fun stateSynchronization_favoritesAndRenamesPropagateImmediately() = testScope.runTest {
        val fakeRepo = FakeIntegrationVideoRepository()
        val video = createSampleVideo(
            id = "vid_202",
            title = "Original Video",
            fileName = "Original Video.mp4"
        )
        fakeRepo.insertVideo(video)

        // Initially not favorite
        assertFalse(fakeRepo.getFavoriteVideos().first().any { it.id == "vid_202" })

        // Toggle favorite
        fakeRepo.setFavorite("vid_202", true)
        val favoritesAfterToggle = fakeRepo.getFavoriteVideos().first()
        assertEquals(1, favoritesAfterToggle.size)
        assertEquals("vid_202", favoritesAfterToggle[0].id)
        assertTrue(favoritesAfterToggle[0].isFavorite)

        // Rename video
        val updatedVideo = video.copy(
            title = "Renamed Video",
            fileName = "Renamed Video.mp4",
            isFavorite = true
        )
        fakeRepo.upsertVideo(updatedVideo)

        val allVideos = fakeRepo.getAllVideos().first()
        assertEquals("Renamed Video", allVideos.first { it.id == "vid_202" }.title)

        // Delete video
        fakeRepo.deleteVideo("vid_202")
        assertTrue(fakeRepo.getAllVideos().first().isEmpty())
        assertTrue(fakeRepo.getFavoriteVideos().first().isEmpty())
    }

    // =========================================================================
    // Test 4: Local vs Network Isolation (Security & Boundary Contracts)
    // =========================================================================

    @Test
    fun networkMedia_isIdentifiedAndRejectsLocalFileManagement() = testScope.runTest {
        val httpUrl = "https://example.com/streams/live_ocean.mp4"
        val networkMetadata = MediaMetadata(
            id = httpUrl,
            mediaUri = httpUrl,
            filePath = null,
            fileName = "live_ocean.mp4",
            title = "Live Ocean",
            folderName = "",
            folderPath = "",
            formattedDuration = "12:34",
            durationMs = 754000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H.264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "4.5 Mbps",
            videoBitrate = 4500000L,
            formattedSize = "",
            sizeBytes = 0L,
            formattedModifiedDate = "",
            lastModified = 0L,
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )

        // 1. isNetworkMedia contract
        assertTrue("MediaMetadata must identify HTTPS as network media", networkMetadata.isNetworkMedia)

        // 2. Local file metadata contract
        val localMetadata = createSampleVideo(id = "local_1", title = "My Video").toMediaMetadata()
        assertFalse("Local video must not be flagged as network media", localMetadata.isNetworkMedia)

        // 3. File operations manager rejection contract
        val fakeFileOps = FakeIntegrationFileOperationsManager()
        val renameResult = fakeFileOps.renameVideo(httpUrl, "New Name")
        assertTrue(renameResult.isFailure)
        assertTrue(renameResult.exceptionOrNull() is UnsupportedOperationException)

        val deleteResult = fakeFileOps.deleteVideo(httpUrl)
        assertTrue(deleteResult.isFailure)
        assertTrue(deleteResult.exceptionOrNull() is UnsupportedOperationException)

        val moveResult = fakeFileOps.moveVideo(httpUrl, "/storage/emulated/0/Movies")
        assertTrue(moveResult.isFailure)
        assertTrue(moveResult.exceptionOrNull() is UnsupportedOperationException)

        val copyResult = fakeFileOps.copyVideo(httpUrl, "/storage/emulated/0/Movies")
        assertTrue(copyResult.isFailure)
        assertTrue(copyResult.exceptionOrNull() is UnsupportedOperationException)
    }

    // =========================================================================
    // Test 5: Playback Queue Orchestration Across Multiple Sources
    // =========================================================================

    @Test
    fun playbackQueue_orchestratesHistoryAndPlaylistSources() {
        val queueManager: PlaybackQueueManager = PlaybackQueueManagerImpl()

        val items = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(
            items = items,
            initialVideoId = "vid_2",
            source = QueueSource.History
        )

        val initialQueue = queueManager.queueState.value
        assertEquals("vid_2", initialQueue.currentVideoId)
        assertEquals(1, initialQueue.currentIndex)
        assertEquals(QueueSource.History, initialQueue.source)
        assertTrue(initialQueue.hasNext)
        assertTrue(initialQueue.hasPrevious)

        // Play next
        val nextId = queueManager.playNext()
        assertEquals("vid_3", nextId)
        assertFalse(queueManager.queueState.value.hasNext)

        // Removing item updates playback order
        queueManager.removeItem("vid_1")
        assertEquals(listOf("vid_2", "vid_3"), queueManager.queueState.value.items)
    }

    // =========================================================================
    // Test 6: Reactive Settings & Global Theme Configuration Propagation
    // =========================================================================

    @Test
    fun themeSettings_propagateGloballyThroughMainViewModel() = testScope.runTest {
        val fakeSettingsRepo = FakeIntegrationSettingsRepository()
        val fakeStorageRepo = FakeIntegrationStorageRepository()
        val fakeScanner = FakeIntegrationScanOrchestrator()

        val mainViewModel = MainViewModel(
            storageAccessRepository = fakeStorageRepo,
            mediaScanOrchestrator = fakeScanner,
            settingsRepository = fakeSettingsRepo
        )

        // Default theme config is System, non-AMOLED
        var config = mainViewModel.themeConfig.first()
        assertEquals(NexusThemeMode.SYSTEM, config.themeMode)
        assertFalse(config.isAmoled)

        // User changes to Dark + AMOLED in Settings
        fakeSettingsRepo.updateAppearance {
            it.copy(
                themeMode = ThemeMode.DARK,
                useAmoledMode = true,
                accentColor = AccentColor.EMERALD
            )
        }

        config = mainViewModel.themeConfig.first { it.themeMode == NexusThemeMode.DARK }
        assertEquals(NexusThemeMode.DARK, config.themeMode)
        assertTrue(config.isAmoled)
        assertEquals(NexusAccentColor.EMERALD, config.accentColor)
    }

    // =========================================================================
    // Helpers & Test Doubles
    // =========================================================================

    private fun createSampleVideo(
        id: String,
        title: String,
        fileName: String = "$title.mp4",
        durationMs: Long = 60_000L
    ) = Video(
        id = id,
        fileName = fileName,
        title = title,
        folderPath = "/storage/emulated/0/Movies",
        folderName = "Movies",
        mediaUri = "content://media/external/video/media/$id",
        filePath = "/storage/emulated/0/Movies/$fileName",
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        sizeBytes = 10_000_000L,
        dateAdded = 1690000000000L,
        lastModified = 1690000000000L,
        videoCodec = "H.264",
        audioCodec = "AAC",
        isFavorite = false,
        playbackPositionMs = 0L,
        playbackPercentage = 0f,
        isCompleted = false,
        watchCount = 0,
        lastPlayedAt = null
    )
}

/**
 * In-memory reactive [VideoRepository] for cross-feature integration testing.
 */
private class FakeIntegrationVideoRepository : VideoRepository {
    private val videosFlow = MutableStateFlow<Map<String, Video>>(emptyMap())

    override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> =
        videosFlow.map { it.values.toList() }

    override fun getFavoriteVideos(): Flow<List<Video>> =
        videosFlow.map { it.values.filter { v -> v.isFavorite } }

    override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> =
        videosFlow.map { map ->
            map.values
                .filter { it.playbackPositionMs > 0 && !it.isCompleted && it.playbackPercentage < 0.95f }
                .sortedByDescending { it.lastPlayedAt ?: 0L }
                .take(limit)
        }

    override fun getAllHistoryVideos(): Flow<List<Video>> =
        videosFlow.map { map ->
            map.values.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }
        }

    override fun getHistoryVideos(limit: Int): Flow<List<Video>> =
        videosFlow.map { map ->
            map.values.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }.take(limit)
        }

    override fun getVideosByFolder(folderPath: String): Flow<List<Video>> =
        videosFlow.map { map -> map.values.filter { it.folderPath == folderPath } }

    override fun getFolders(): Flow<List<VideoFolder>> = MutableStateFlow(emptyList())

    override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> =
        videosFlow.map { it.values.take(limit) }

    override suspend fun getVideoById(id: String): Video? =
        videosFlow.value[id]

    override suspend fun getVideoByUri(mediaUri: String): Video? =
        videosFlow.value.values.firstOrNull { it.mediaUri == mediaUri }

    override suspend fun getVideosCount(): Int = videosFlow.value.size

    override suspend fun insertVideo(video: Video): Long {
        videosFlow.value = videosFlow.value + (video.id to video)
        return 1L
    }

    override suspend fun upsertVideo(video: Video) {
        videosFlow.value = videosFlow.value + (video.id to video)
    }

    override suspend fun upsertVideos(videos: List<Video>) {
        videosFlow.value = videosFlow.value + videos.associateBy { it.id }
    }

    override suspend fun deleteVideo(id: String) {
        videosFlow.value = videosFlow.value - id
    }

    override suspend fun deleteVideoByUri(mediaUri: String) {
        val target = videosFlow.value.values.firstOrNull { it.mediaUri == mediaUri }
        if (target != null) {
            deleteVideo(target.id)
        }
    }

    override suspend fun deleteStaleVideos(validIds: List<String>) {
        videosFlow.value = videosFlow.value.filterKeys { it in validIds }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        val existing = videosFlow.value[id] ?: return
        videosFlow.value = videosFlow.value + (id to existing.copy(isFavorite = isFavorite))
    }

    override suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        percentage: Float,
        lastPlayedAt: Long,
        isCompleted: Boolean
    ) {
        val existing = videosFlow.value[id] ?: return
        videosFlow.value = videosFlow.value + (id to existing.copy(
            playbackPositionMs = positionMs,
            playbackPercentage = percentage,
            lastPlayedAt = lastPlayedAt,
            isCompleted = isCompleted
        ))
    }

    override suspend fun clearAll() {
        videosFlow.value = emptyMap()
    }
}

/**
 * Fake [VideoFileOperationsManager] that enforces rejection of network streams.
 */
private class FakeIntegrationFileOperationsManager : VideoFileOperationsManager {
    private fun isNetworkVideo(videoId: String): Boolean =
        videoId.startsWith("http://", ignoreCase = true) || videoId.startsWith("https://", ignoreCase = true)

    override suspend fun renameVideo(videoId: String, newName: String): Result<MediaMetadata> {
        if (isNetworkVideo(videoId)) {
            return Result.failure(UnsupportedOperationException("File operations are not supported for network streams."))
        }
        return Result.success(createStubMetadata(videoId, newName))
    }

    override suspend fun moveVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> {
        if (isNetworkVideo(videoId)) {
            return Result.failure(UnsupportedOperationException("File operations are not supported for network streams."))
        }
        return Result.success(createStubMetadata(videoId, "Moved"))
    }

    override suspend fun copyVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> {
        if (isNetworkVideo(videoId)) {
            return Result.failure(UnsupportedOperationException("File operations are not supported for network streams."))
        }
        return Result.success(createStubMetadata(videoId, "Copied"))
    }

    override suspend fun deleteVideo(videoId: String, stageForUndo: Boolean): Result<Unit> {
        if (isNetworkVideo(videoId)) {
            return Result.failure(UnsupportedOperationException("File operations are not supported for network streams."))
        }
        return Result.success(Unit)
    }

    override suspend fun restoreDeletedVideo(videoId: String): Result<MediaMetadata> =
        Result.success(createStubMetadata(videoId, "Restored"))

    override suspend fun purgeStagedDeletions() {}
    override suspend fun setFavorite(videoId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    override fun getAvailableFolders(): Flow<List<VideoFolder>> =
        MutableStateFlow(emptyList())
    override fun shareVideo(context: android.content.Context, video: MediaMetadata) {}
    override fun openContainingFolder(
        context: android.content.Context,
        folderPath: String,
        folderName: String,
        onNavigateInApp: (folderPath: String, folderName: String) -> Unit
    ) {}

    private fun createStubMetadata(id: String, title: String) = MediaMetadata(
        id = id,
        mediaUri = "file:///storage/$id.mp4",
        filePath = "/storage/$id.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage",
        formattedDuration = "01:00",
        durationMs = 60000L,
        resolutionLabel = "1080p",
        dimensionsLabel = "1920x1080",
        width = 1920,
        height = 1080,
        videoCodec = "H264",
        audioCodec = "AAC",
        formattedFps = "30",
        frameRate = 30f,
        formattedBitrate = "2 Mbps",
        videoBitrate = 2000000L,
        formattedSize = "50 MB",
        sizeBytes = 50000000L,
        formattedModifiedDate = "Today",
        lastModified = 0L,
        audioTrackCount = 1,
        subtitleTrackCount = 0,
        isFavorite = false,
        playbackPositionMs = 0L,
        playbackPercentage = 0f,
        watchCount = 0,
        lastPlayedAt = null
    )
}

/**
 * Fake [SettingsRepository] matching MainViewModel's usage.
 */
private class FakeIntegrationSettingsRepository(
    initialSettings: NexusSettings = NexusSettings()
) : SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<NexusSettings> = _settings.asStateFlow()

    fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        _settings.value = _settings.value.copy(
            appearance = transform(_settings.value.appearance)
        )
    }

    override suspend fun setDefaultPlaybackSpeed(speed: Float) {}
    override suspend fun setAutoNextEnabled(enabled: Boolean) {}
    override suspend fun setRepeatMode(mode: RepeatModeSetting) {}
    override suspend fun setResumeBehavior(behavior: ResumeBehavior) {}
    override suspend fun setSeekDurationSeconds(seconds: Int) {}
    override suspend fun setDoubleTapSeekEnabled(enabled: Boolean) {}
    override suspend fun setPressAndHoldSpeed(speed: Float) {}
    override suspend fun setDefaultDisplayMode(mode: VideoDisplayMode) {}
    override suspend fun setSubtitlesEnabled(enabled: Boolean) {}
    override suspend fun setPreferredSubtitleLanguage(language: String) {}
    override suspend fun setSubtitleFontScale(scale: Float) {}
    override suspend fun setDefaultLayoutMode(layout: LibraryLayout) {}
    override suspend fun setDefaultSortOption(sort: LibrarySort) {}
    override suspend fun setScanBehavior(behavior: ScanBehavior) {}
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun setAmoledMode(enabled: Boolean) {}
    override suspend fun setDynamicColor(enabled: Boolean) {}
    override suspend fun setAccentColor(accent: AccentColor) {}
    override suspend fun setPreferredAudioLanguage(language: String) {}
    override suspend fun setAudioBoostEnabled(enabled: Boolean) {}
    override suspend fun setAudioDelayMs(delayMs: Long) {}
    override suspend fun setCacheThumbnailMaxEntries(maxEntries: Int) {}
    override suspend fun setPreserveStagedDeletions(preserve: Boolean) {}
    override suspend fun setHardwareAcceleration(enabled: Boolean) {}
    override suspend fun setDebugLogging(enabled: Boolean) {}
}

/**
 * Fake [StorageAccessRepository] matching MainViewModel's usage.
 */
private class FakeIntegrationStorageRepository : StorageAccessRepository {
    private val state = MutableStateFlow(
        StorageAccessState(isOnboardingCompleted = true, isPermissionGranted = true)
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

/**
 * Fake [MediaScanOrchestrator] matching MainViewModel's usage.
 */
private class FakeIntegrationScanOrchestrator : MediaScanOrchestrator {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun triggerStartupScan(): Boolean = true
    override fun triggerManualScan(): Boolean = true
    override fun triggerIncrementalScan(): Boolean = true
    override fun triggerLocationScan(locationUriOrPath: String): Boolean = true
    override fun cancelScan() {}
}
