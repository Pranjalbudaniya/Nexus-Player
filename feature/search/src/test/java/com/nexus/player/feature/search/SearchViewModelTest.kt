package com.nexus.player.feature.search

import android.graphics.Bitmap
import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeVideoRepository
    private lateinit var fakeQueueManager: PlaybackQueueManagerImpl
    private lateinit var fakeThumbnailLoader: FakeThumbnailLoader
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeVideoRepository()
        fakeQueueManager = PlaybackQueueManagerImpl()
        fakeThumbnailLoader = FakeThumbnailLoader()

        viewModel = SearchViewModel(
            videoRepository = fakeRepository,
            playbackQueueManager = fakeQueueManager,
            thumbnailLoader = fakeThumbnailLoader
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleVideo(
        id: String,
        title: String,
        folderName: String = "Movies",
        resolutionLabel: String = "1080p",
        durationMs: Long = 120_000L
    ) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = folderName,
        folderPath = "/storage/Movies",
        sizeBytes = 50_000_000L,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = resolutionLabel,
        dateAdded = 1000L,
        lastModified = 1000L
    )

    @Test
    fun initialState_emptyQuery_showsRecentVideos() = runTest {
        val v1 = sampleVideo("1", "Recent Video 1")
        val v2 = sampleVideo("2", "Recent Video 2")
        fakeRepository.setRecentVideos(listOf(v1, v2))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Initial)
        val initial = state as SearchUiState.Initial
        assertEquals(2, initial.recentVideos.size)
        assertEquals("Recent Video 1", initial.recentVideos[0].title)

        collectJob.cancel()
    }

    @Test
    fun whitespaceQuery_showsInitialState() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("    ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Initial)

        collectJob.cancel()
    }

    @Test
    fun debouncedQuery_doesNotQueryUntilWindowExpires() = runTest {
        val v1 = sampleVideo("1", "The Matrix")
        fakeRepository.setAllVideos(listOf(v1))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Rapid keystrokes
        viewModel.onSearchQueryChange("m")
        advanceTimeBy(100L)
        viewModel.onSearchQueryChange("ma")
        advanceTimeBy(100L)
        viewModel.onSearchQueryChange("mat")
        advanceTimeBy(100L)
        viewModel.onSearchQueryChange("matrix")

        // Before 200ms has elapsed since "matrix"
        advanceTimeBy(100L)
        assertTrue(viewModel.uiState.value is SearchUiState.Searching)

        // Advance remaining debounce window
        advanceTimeBy(150L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success)
        val success = state as SearchUiState.Success
        assertEquals(1, success.resultCount)
        assertEquals("The Matrix", success.results[0].title)

        collectJob.cancel()
    }

    @Test
    fun searchMatching_filtersByTitleFolderResolution() = runTest {
        val v1 = sampleVideo("1", "Inception", folderName = "SciFi", resolutionLabel = "4K")
        val v2 = sampleVideo("2", "Interstellar", folderName = "SciFi", resolutionLabel = "1080p")
        val v3 = sampleVideo("3", "The Hangover", folderName = "Comedy", resolutionLabel = "720p")
        fakeRepository.setAllVideos(listOf(v1, v2, v3))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Search by resolution "4K"
        viewModel.onSearchQueryChange("4K")
        advanceTimeBy(250L)
        advanceUntilIdle()
        val res4k = viewModel.uiState.value as SearchUiState.Success
        assertEquals(1, res4k.resultCount)
        assertEquals("Inception", res4k.results[0].title)

        // Search by folder "Comedy"
        viewModel.onSearchQueryChange("Comedy")
        advanceTimeBy(250L)
        advanceUntilIdle()
        val comedy = viewModel.uiState.value as SearchUiState.Success
        assertEquals(1, comedy.resultCount)
        assertEquals("The Hangover", comedy.results[0].title)

        collectJob.cancel()
    }

    @Test
    fun noResults_showsEmptyState() = runTest {
        fakeRepository.setAllVideos(listOf(sampleVideo("1", "Toy Story")))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Avengers")
        advanceTimeBy(250L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Empty)
        assertEquals("Avengers", (state as SearchUiState.Empty).trimmedQuery)

        collectJob.cancel()
    }

    @Test
    fun clearQuery_immediatelyTransitionsToInitial() = runTest {
        fakeRepository.setAllVideos(listOf(sampleVideo("1", "Gladiator")))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Gladiator")
        advanceTimeBy(250L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.Success)

        // Clear query
        viewModel.clearSearchQuery()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Initial)

        collectJob.cancel()
    }

    @Test
    fun playVideo_setsPlaybackQueueWithSearchSource() = runTest {
        val v1 = sampleVideo("1", "Dune Part One")
        val v2 = sampleVideo("2", "Dune Part Two")
        fakeRepository.setAllVideos(listOf(v1, v2))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Dune")
        advanceTimeBy(250L)
        advanceUntilIdle()

        viewModel.playVideo("2")
        advanceUntilIdle()

        val queue = fakeQueueManager.queueState.value
        assertEquals("2", queue.currentVideoId)
        assertEquals(2, queue.items.size)
        assertTrue(queue.source is QueueSource.Search)
        assertEquals("Dune", (queue.source as QueueSource.Search).query)

        collectJob.cancel()
    }

    @Test
    fun toggleLayoutMode_switchesBetweenGridAndList() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(SearchLayoutMode.LIST, viewModel.uiState.value.layoutMode)

        viewModel.toggleLayoutMode()
        advanceUntilIdle()
        assertEquals(SearchLayoutMode.GRID, viewModel.uiState.value.layoutMode)

        viewModel.toggleLayoutMode()
        advanceUntilIdle()
        assertEquals(SearchLayoutMode.LIST, viewModel.uiState.value.layoutMode)

        collectJob.cancel()
    }

    @Test
    fun contextMenu_selectsAndDismissesVideo() = runTest {
        val v1 = sampleVideo("1", "Casablanca")
        fakeRepository.setAllVideos(listOf(v1))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Casablanca")
        advanceTimeBy(250L)
        advanceUntilIdle()

        val video = (viewModel.uiState.value as SearchUiState.Success).results.first()
        viewModel.onVideoLongClick(video)
        advanceUntilIdle()

        assertEquals(video, viewModel.uiState.value.selectedVideoForMenu)

        viewModel.dismissContextMenu()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedVideoForMenu)

        collectJob.cancel()
    }

    @Test
    fun reactiveUpdates_updatesSearchResultsWhenRepositoryChanges() = runTest {
        val v1 = sampleVideo("1", "Alien")
        fakeRepository.setAllVideos(listOf(v1))

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Alien")
        advanceTimeBy(250L)
        advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value as SearchUiState.Success).resultCount)

        // Repository updates with new matching video
        val v2 = sampleVideo("2", "Alien Romulus")
        fakeRepository.setAllVideos(listOf(v1, v2))
        advanceUntilIdle()

        val updated = viewModel.uiState.value as SearchUiState.Success
        assertEquals(2, updated.resultCount)

        collectJob.cancel()
    }

    // --- Test Doubles ---

    private class FakeThumbnailLoader : ThumbnailLoader {
        override suspend fun loadThumbnail(
            mediaUri: String,
            targetWidth: Int,
            targetHeight: Int
        ): Bitmap? = null

        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }

    private class FakeVideoRepository : VideoRepository {
        private val _allVideosFlow = MutableStateFlow<List<Video>>(emptyList())
        private val _recentVideosFlow = MutableStateFlow<List<Video>>(emptyList())

        fun setAllVideos(videos: List<Video>) {
            _allVideosFlow.value = videos
        }

        fun setRecentVideos(videos: List<Video>) {
            _recentVideosFlow.value = videos
        }

        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> =
            _recentVideosFlow.asStateFlow()

        override fun searchVideos(query: String): Flow<List<Video>> {
            val q = query.trim().lowercase()
            return _allVideosFlow.map { list ->
                if (q.isEmpty()) emptyList()
                else list.filter { video ->
                    video.title.lowercase().contains(q) ||
                    video.fileName.lowercase().contains(q) ||
                    video.folderName.lowercase().contains(q) ||
                    video.resolutionLabel.lowercase().contains(q)
                }
            }
        }

        override fun searchVideos(filter: SearchFilter): Flow<List<Video>> = searchVideos(filter.query)

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = _allVideosFlow.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = _allVideosFlow.value.find { it.id == id }
        override suspend fun getVideoByUri(mediaUri: String): Video? = _allVideosFlow.value.find { it.mediaUri == mediaUri }
        override suspend fun getVideosCount(): Int = _allVideosFlow.value.size
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
