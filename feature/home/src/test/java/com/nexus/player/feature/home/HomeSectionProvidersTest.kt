package com.nexus.player.feature.home

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.FoldersSectionProviderImpl
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeSectionProvidersTest {

    private lateinit var fakeVideoRepository: FakeTestVideoRepository
    private lateinit var fakeFolderRepository: FakeTestFolderRepository

    private lateinit var continueWatchingProvider: ContinueWatchingSectionProviderImpl
    private lateinit var recentlyAddedProvider: RecentlyAddedSectionProviderImpl
    private lateinit var favoritesProvider: FavoritesSectionProviderImpl
    private lateinit var foldersProvider: FoldersSectionProviderImpl

    @Before
    fun setUp() {
        fakeVideoRepository = FakeTestVideoRepository()
        fakeFolderRepository = FakeTestFolderRepository()

        val dispatcher = Dispatchers.Unconfined
        continueWatchingProvider = ContinueWatchingSectionProviderImpl(fakeVideoRepository, dispatcher)
        recentlyAddedProvider = RecentlyAddedSectionProviderImpl(fakeVideoRepository, dispatcher)
        favoritesProvider = FavoritesSectionProviderImpl(fakeVideoRepository, dispatcher)
        foldersProvider = FoldersSectionProviderImpl(fakeFolderRepository, dispatcher)
    }

    // =========================================================================
    // 1. Continue Watching Tests
    // =========================================================================

    @Test
    fun continueWatching_filtersCompletedVideos_respects95PercentRule() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", title = "Completed at 95%", durationMs = 100_000L, playbackPositionMs = 95_000L, playbackPercentage = 0.95f, lastPlayedAt = 1000L),
            createTestVideo(id = "2", title = "Completed at 98%", durationMs = 100_000L, playbackPositionMs = 98_000L, playbackPercentage = 0.98f, lastPlayedAt = 2000L),
            createTestVideo(id = "3", title = "In Progress at 50%", durationMs = 100_000L, playbackPositionMs = 50_000L, playbackPercentage = 0.50f, lastPlayedAt = 3000L)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(1, items.size)
        assertEquals("3", items.first().id)
        assertEquals("In Progress at 50%", items.first().title)
        assertEquals(50_000L, items.first().playbackPositionMs)
    }

    @Test
    fun continueWatching_filtersNotStartedVideos() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", title = "Not Started", playbackPositionMs = 0L, playbackPercentage = 0.0f, lastPlayedAt = 1000L),
            createTestVideo(id = "2", title = "Valid In Progress", playbackPositionMs = 20_000L, playbackPercentage = 0.20f, lastPlayedAt = 2000L)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(1, items.size)
        assertEquals("2", items.first().id)
    }

    @Test
    fun continueWatching_filtersZeroDurationVideos() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", title = "Zero Duration", durationMs = 0L, playbackPositionMs = 1000L, playbackPercentage = 0.5f, lastPlayedAt = 1000L),
            createTestVideo(id = "2", title = "Negative Duration", durationMs = -100L, playbackPositionMs = 1000L, playbackPercentage = 0.5f, lastPlayedAt = 1000L),
            createTestVideo(id = "3", title = "Valid Duration", durationMs = 60_000L, playbackPositionMs = 30_000L, playbackPercentage = 0.5f, lastPlayedAt = 1000L)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(1, items.size)
        assertEquals("3", items.first().id)
    }

    @Test
    fun continueWatching_filtersMissingTimestamps() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", title = "No Timestamp", lastPlayedAt = null, playbackPositionMs = 10_000L, playbackPercentage = 0.3f),
            createTestVideo(id = "2", title = "Zero Timestamp", lastPlayedAt = 0L, playbackPositionMs = 10_000L, playbackPercentage = 0.3f),
            createTestVideo(id = "3", title = "Valid Timestamp", lastPlayedAt = 5000L, playbackPositionMs = 10_000L, playbackPercentage = 0.3f)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(1, items.size)
        assertEquals("3", items.first().id)
    }

    @Test
    fun continueWatching_filtersBlankOrDeletedMediaUri() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", mediaUri = "", title = "Blank URI", playbackPositionMs = 10_000L, playbackPercentage = 0.3f, lastPlayedAt = 1000L),
            createTestVideo(id = "2", mediaUri = "content://valid", title = "Valid URI", playbackPositionMs = 10_000L, playbackPercentage = 0.3f, lastPlayedAt = 1000L)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(1, items.size)
        assertEquals("2", items.first().id)
    }

    @Test
    fun continueWatching_preservesExactPlaybackPositionAndSortsByLastPlayedDescending() = runTest {
        fakeVideoRepository.continueWatchingVideos.value = listOf(
            createTestVideo(id = "1", title = "Older", durationMs = 100_000L, playbackPositionMs = 12_345L, playbackPercentage = 0.12f, lastPlayedAt = 1000L),
            createTestVideo(id = "2", title = "Newer", durationMs = 100_000L, playbackPositionMs = 67_890L, playbackPercentage = 0.67f, lastPlayedAt = 9000L)
        )

        val items = continueWatchingProvider.getContinueWatching().first()
        assertEquals(2, items.size)
        assertEquals("Newer", items[0].title)
        assertEquals(67_890L, items[0].playbackPositionMs)
        assertEquals("Older", items[1].title)
        assertEquals(12_345L, items[1].playbackPositionMs)
    }

    @Test
    fun continueWatching_respectsConfigurableLimit() = runTest {
        val videos = (1..15).map { i ->
            createTestVideo(id = "$i", title = "Video $i", playbackPositionMs = 5000L, playbackPercentage = 0.2f, lastPlayedAt = i * 1000L)
        }
        fakeVideoRepository.continueWatchingVideos.value = videos

        val items = continueWatchingProvider.getContinueWatching(limit = 5).first()
        assertEquals(5, items.size)
        assertEquals("Video 15", items.first().title)
    }

    // =========================================================================
    // 2. Recently Added Tests
    // =========================================================================

    @Test
    fun recentlyAdded_ordersByDateAddedDescending() = runTest {
        fakeVideoRepository.recentlyAddedVideos.value = listOf(
            createTestVideo(id = "1", title = "Added Older", dateAdded = 1000L),
            createTestVideo(id = "2", title = "Added Newer", dateAdded = 5000L)
        )

        val items = recentlyAddedProvider.getRecentlyAdded().first()
        assertEquals(2, items.size)
        assertEquals("Added Newer", items[0].title)
        assertEquals("Added Older", items[1].title)
    }

    @Test
    fun recentlyAdded_formatsTechnicalSpecsAndFolder() = runTest {
        fakeVideoRepository.recentlyAddedVideos.value = listOf(
            createTestVideo(
                id = "1",
                title = "4K Movie",
                resolutionLabel = "4K",
                videoCodec = "HEVC",
                sizeBytes = 1_500_000_000L,
                folderName = "Movies",
                dateAdded = System.currentTimeMillis() - 3600_000L // 1 hour ago
            )
        )

        val items = recentlyAddedProvider.getRecentlyAdded().first()
        assertEquals(1, items.size)
        val item = items.first()
        // Quality removed below card since it's already on the card badge
        assertFalse(item.technicalSpecs.contains("4K"))
        assertTrue(item.technicalSpecs.contains("HEVC"))
        assertTrue(item.technicalSpecs.contains("1.4 GB") || item.technicalSpecs.contains("1.5 GB") || item.technicalSpecs.contains("GB"))
        // Date only, no "Added" and no "Movies" folder
        assertFalse(item.addedTimeAndFolder.contains("Movies"))
        assertFalse(item.addedTimeAndFolder.contains("Added"))
        assertTrue(item.addedTimeAndFolder.matches(Regex("[A-Za-z]{3} \\d{1,2}, \\d{4}")))
    }

    @Test
    fun recentlyAdded_respectsConfigurableLimit() = runTest {
        val videos = (1..20).map { i ->
            createTestVideo(id = "$i", title = "Video $i", dateAdded = i * 1000L)
        }
        fakeVideoRepository.recentlyAddedVideos.value = videos

        val items = recentlyAddedProvider.getRecentlyAdded(limit = 4).first()
        assertEquals(4, items.size)
        assertEquals("Video 20", items.first().title)
    }

    // =========================================================================
    // 3. Favorites Tests
    // =========================================================================

    @Test
    fun favorites_filtersOnlyFavorites() = runTest {
        fakeVideoRepository.favoriteVideos.value = listOf(
            createTestVideo(id = "1", title = "Fav 1", isFavorite = true),
            createTestVideo(id = "2", title = "Fav 2", isFavorite = true)
        )

        val items = favoritesProvider.getFavorites().first()
        assertEquals(2, items.size)
        assertEquals("Fav 1", items[0].title)
        assertEquals("Fav 2", items[1].title)
    }

    @Test
    fun favorites_reactsAutomaticallyWhenFavoriteStateChanges() = runTest {
        fakeVideoRepository.favoriteVideos.value = listOf(
            createTestVideo(id = "1", title = "Fav 1", isFavorite = true)
        )

        val flow = favoritesProvider.getFavorites()
        assertEquals(1, flow.first().size)

        // Simulate user unfavoriting or favoriting another video
        fakeVideoRepository.favoriteVideos.value = listOf(
            createTestVideo(id = "1", title = "Fav 1", isFavorite = true),
            createTestVideo(id = "2", title = "Fav 2", isFavorite = true)
        )

        assertEquals(2, flow.first().size)
    }

    // =========================================================================
    // 4. Folders Tests
    // =========================================================================

    @Test
    fun folders_providesRootFoldersWithCountsAndPreviewUri() = runTest {
        fakeFolderRepository.rootFolders.value = listOf(
            VideoFolder(
                folderPath = "/storage/emulated/0/Movies",
                folderName = "Movies",
                videoCount = 42,
                previewMediaUri = "content://preview/movies",
                lastModified = 1000L
            ),
            VideoFolder(
                folderPath = "/storage/emulated/0/Downloads",
                folderName = "Downloads",
                videoCount = 1,
                previewMediaUri = "content://preview/downloads",
                lastModified = 2000L
            )
        )

        val items = foldersProvider.getHomeFolders().first()
        assertEquals(2, items.size)
        assertEquals("Movies", items[0].name)
        assertEquals("42 videos", items[0].videoCountText)
        assertEquals("content://preview/movies", items[0].previewMediaUri)

        assertEquals("Downloads", items[1].name)
        assertEquals("1 video", items[1].videoCountText)
        assertEquals("content://preview/downloads", items[1].previewMediaUri)
    }

    @Test
    fun folders_filtersOutEmptyFolders() = runTest {
        fakeFolderRepository.rootFolders.value = listOf(
            VideoFolder(
                folderPath = "/storage/emulated/0/Empty",
                folderName = "Empty",
                videoCount = 0
            ),
            VideoFolder(
                folderPath = "/storage/emulated/0/Videos",
                folderName = "Videos",
                videoCount = 5
            )
        )

        val items = foldersProvider.getHomeFolders().first()
        assertEquals(1, items.size)
        assertEquals("Videos", items.first().name)
    }

    // =========================================================================
    // Helpers & Fakes
    // =========================================================================

    private fun createTestVideo(
        id: String,
        mediaUri: String = "content://media/$id",
        title: String,
        durationMs: Long = 60_000L,
        resolutionLabel: String = "1080p",
        videoCodec: String? = "H.264",
        folderName: String = "Movies",
        folderPath: String = "/storage/emulated/0/Movies",
        sizeBytes: Long = 50_000_000L,
        dateAdded: Long = 1000L,
        lastPlayedAt: Long? = null,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0.0f,
        isFavorite: Boolean = false
    ): Video = Video(
        id = id,
        mediaUri = mediaUri,
        filePath = "/storage/emulated/0/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = folderName,
        folderPath = folderPath,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = resolutionLabel,
        videoCodec = videoCodec,
        dateAdded = dateAdded,
        lastModified = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = isFavorite
    )

    private class FakeTestVideoRepository : VideoRepository {
        val continueWatchingVideos = MutableStateFlow<List<Video>>(emptyList())
        val recentlyAddedVideos = MutableStateFlow<List<Video>>(emptyList())
        val favoriteVideos = MutableStateFlow<List<Video>>(emptyList())

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(emptyList())
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = recentlyAddedVideos.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = favoriteVideos.asStateFlow()
        override fun getFavoriteVideos(limit: Int): Flow<List<Video>> = favoriteVideos.asStateFlow()
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = continueWatchingVideos.asStateFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = null
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = 0
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

    private class FakeTestFolderRepository : FolderRepository {
        val rootFolders = MutableStateFlow<List<VideoFolder>>(emptyList())

        override fun getFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = rootFolders.asStateFlow()
        override fun getRootFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = rootFolders.asStateFlow()
        override fun getChildFolders(parentFolderPath: String, sortOrder: FolderSortOrder): Flow<List<VideoFolder>> = flowOf(emptyList())
        override fun getFolderByPath(folderPath: String): Flow<VideoFolder?> = flowOf(rootFolders.value.find { it.folderPath == folderPath })
        override fun getVideosInFolder(folderPath: String, sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(emptyList())
    }
}
