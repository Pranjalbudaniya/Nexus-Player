package com.nexus.player.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.database.repository.VideoRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VideoRepositoryTest {

    private lateinit var database: NexusDatabase
    private lateinit var videoDao: VideoDao
    private lateinit var repository: VideoRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoDao = database.videoDao()
        repository = VideoRepositoryImpl(
            videoDao = videoDao,
            ioDispatcher = testDispatcher
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    private fun sampleDomainVideo(
        id: String,
        title: String,
        mediaUri: String = "content://media/$id",
        folderName: String = "Movies",
        folderPath: String = "/Movies",
        durationMs: Long = 100000L,
        sizeBytes: Long = 50000000L,
        dateAdded: Long = 1000L,
        lastPlayedAt: Long? = null,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0.0f,
        isFavorite: Boolean = false,
        isCompleted: Boolean = false,
        fileName: String = "$title.mp4",
        resolutionLabel: String = "1080p",
        videoCodec: String? = "H.264",
        audioCodec: String? = "AAC"
    ) = Video(
        id = id,
        mediaUri = mediaUri,
        filePath = "/storage$folderPath/$fileName",
        fileName = fileName,
        title = title,
        folderName = folderName,
        folderPath = folderPath,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = resolutionLabel,
        videoCodec = videoCodec,
        audioCodec = audioCodec,
        dateAdded = dateAdded,
        lastModified = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = isFavorite,
        isCompleted = isCompleted
    )

    @Test
    fun repositorySortingOrders() = runTest {
        repository.upsertVideos(
            listOf(
                sampleDomainVideo(id = "1", title = "Zeta", dateAdded = 100L, durationMs = 3000L, sizeBytes = 10L, lastPlayedAt = 800L),
                sampleDomainVideo(id = "2", title = "Alpha", dateAdded = 300L, durationMs = 1000L, sizeBytes = 30L, lastPlayedAt = null),
                sampleDomainVideo(id = "3", title = "Beta", dateAdded = 200L, durationMs = 2000L, sizeBytes = 20L, lastPlayedAt = 500L)
            )
        )

        // Title ASC
        val byTitleAsc = repository.getAllVideos(VideoSortOrder.TITLE_ASC).first()
        assertEquals(listOf("Alpha", "Beta", "Zeta"), byTitleAsc.map { it.title })

        // Title DESC
        val byTitleDesc = repository.getAllVideos(VideoSortOrder.TITLE_DESC).first()
        assertEquals(listOf("Zeta", "Beta", "Alpha"), byTitleDesc.map { it.title })

        // Date Added DESC
        val byDateDesc = repository.getAllVideos(VideoSortOrder.DATE_ADDED_DESC).first()
        assertEquals(listOf("Alpha", "Beta", "Zeta"), byDateDesc.map { it.title })

        // Duration DESC
        val byDurationDesc = repository.getAllVideos(VideoSortOrder.DURATION_DESC).first()
        assertEquals(listOf("Zeta", "Beta", "Alpha"), byDurationDesc.map { it.title })

        // Duration ASC
        val byDurationAsc = repository.getAllVideos(VideoSortOrder.DURATION_ASC).first()
        assertEquals(listOf("Alpha", "Beta", "Zeta"), byDurationAsc.map { it.title })

        // Size DESC
        val bySizeDesc = repository.getAllVideos(VideoSortOrder.SIZE_DESC).first()
        assertEquals(listOf("Alpha", "Beta", "Zeta"), bySizeDesc.map { it.title })

        // Size ASC
        val bySizeAsc = repository.getAllVideos(VideoSortOrder.SIZE_ASC).first()
        assertEquals(listOf("Zeta", "Beta", "Alpha"), bySizeAsc.map { it.title })

        // Last Played DESC
        val byLastPlayedDesc = repository.getAllVideos(VideoSortOrder.LAST_PLAYED_DESC).first()
        assertEquals(listOf("Zeta", "Beta", "Alpha"), byLastPlayedDesc.map { it.title })

        // Last Played ASC
        val byLastPlayedAsc = repository.getAllVideos(VideoSortOrder.LAST_PLAYED_ASC).first()
        assertEquals(listOf("Beta", "Zeta", "Alpha"), byLastPlayedAsc.map { it.title })
    }

    @Test
    fun repositoryFavoritesAndProgress() = runTest {
        val video = sampleDomainVideo(id = "fav_test", title = "Favorite Test")
        repository.insertVideo(video)

        assertEquals(0, repository.getFavoriteVideos().first().size)

        repository.setFavorite("fav_test", true)
        val favs = repository.getFavoriteVideos().first()
        assertEquals(1, favs.size)
        assertEquals("fav_test", favs[0].id)

        // Update progress
        repository.updatePlaybackProgress(
            id = "fav_test",
            positionMs = 45000L,
            percentage = 0.45f,
            lastPlayedAt = 9999L
        )

        val updated = repository.getVideoById("fav_test")
        assertNotNull(updated)
        assertEquals(45000L, updated?.playbackPositionMs)
        assertEquals(0.45f, updated?.playbackPercentage ?: 0f, 0.001f)
        assertEquals(9999L, updated?.lastPlayedAt)

        // Continue watching item
        val continueWatching = repository.getContinueWatchingVideos().first()
        assertEquals(1, continueWatching.size)
        assertEquals("fav_test", continueWatching[0].id)
    }

    @Test
    fun repositoryDeleteAndCleanup() = runTest {
        repository.upsertVideos(
            listOf(
                sampleDomainVideo(id = "del_1", title = "One", mediaUri = "uri_1"),
                sampleDomainVideo(id = "del_2", title = "Two", mediaUri = "uri_2")
            )
        )
        assertEquals(2, repository.getVideosCount())

        repository.deleteVideo("del_1")
        assertEquals(1, repository.getVideosCount())
        assertNull(repository.getVideoById("del_1"))

        repository.deleteVideoByUri("uri_2")
        assertEquals(0, repository.getVideosCount())
    }

    @Test
    fun searchVideos_emptyOrWhitespaceQuery_returnsEmptyList() = runTest {
        repository.upsertVideos(
            listOf(sampleDomainVideo(id = "1", title = "Test Video", mediaUri = "u1"))
        )
        val emptyResult = repository.searchVideos("").first()
        val whitespaceResult = repository.searchVideos("    ").first()
        assertEquals(0, emptyResult.size)
        assertEquals(0, whitespaceResult.size)
    }

    @Test
    fun searchVideos_matchesTitleFolderResolutionAndCodecs() = runTest {
        val v1 = sampleDomainVideo(
            id = "1",
            title = "Oppenheimer",
            fileName = "oppenheimer.mkv",
            folderName = "Cinema",
            resolutionLabel = "4K",
            videoCodec = "hevc",
            audioCodec = "dts",
            mediaUri = "u1"
        )
        val v2 = sampleDomainVideo(
            id = "2",
            title = "Barbie",
            fileName = "barbie.mp4",
            folderName = "Comedy",
            resolutionLabel = "1080p",
            videoCodec = "h264",
            audioCodec = "aac",
            mediaUri = "u2"
        )
        repository.upsertVideos(listOf(v1, v2))

        // Title match
        val byTitle = repository.searchVideos("oppen").first()
        assertEquals(1, byTitle.size)
        assertEquals("1", byTitle[0].id)

        // Folder match
        val byFolder = repository.searchVideos("comedy").first()
        assertEquals(1, byFolder.size)
        assertEquals("2", byFolder[0].id)

        // Resolution match
        val byRes = repository.searchVideos("4k").first()
        assertEquals(1, byRes.size)
        assertEquals("1", byRes[0].id)

        // Codec match
        val byCodec = repository.searchVideos("hevc").first()
        assertEquals(1, byCodec.size)
        assertEquals("1", byCodec[0].id)
    }

    @Test
    fun searchVideos_multiTokenMatching() = runTest {
        val v1 = sampleDomainVideo(
            id = "1",
            title = "Family Vacation",
            folderName = "Trip2024",
            resolutionLabel = "4K",
            mediaUri = "u1"
        )
        val v2 = sampleDomainVideo(
            id = "2",
            title = "Family Reunion",
            folderName = "Home",
            resolutionLabel = "1080p",
            mediaUri = "u2"
        )
        repository.upsertVideos(listOf(v1, v2))

        // "family 4k" should match v1 only
        val result = repository.searchVideos("family 4k").first()
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)

        // "family trip" should match v1 only
        val tripResult = repository.searchVideos("family trip").first()
        assertEquals(1, tripResult.size)
        assertEquals("1", tripResult[0].id)
    }

    @Test
    fun searchVideos_relevanceRanking() = runTest {
        val v1 = sampleDomainVideo(id = "1", title = "Fast and Furious", mediaUri = "u1")
        val v2 = sampleDomainVideo(id = "2", title = "Fast", mediaUri = "u2")
        val v3 = sampleDomainVideo(id = "3", title = "Too Fast Too Furious", mediaUri = "u3")
        repository.upsertVideos(listOf(v1, v2, v3))

        val results = repository.searchVideos("fast").first()
        assertEquals(3, results.size)
        // Exact match "Fast" should be ranked 1st
        assertEquals("2", results[0].id)
        // Title starting with "Fast" ("Fast and Furious") should be ranked 2nd
        assertEquals("1", results[1].id)
        // Substring match ("Too Fast...") should be ranked 3rd
        assertEquals("3", results[2].id)
    }

    @Test
    fun searchVideos_reactiveUpdatesOnInsertAndDelete() = runTest {
        repository.upsertVideos(
            listOf(sampleDomainVideo(id = "1", title = "Avatar 1", mediaUri = "u1"))
        )
        assertEquals(1, repository.searchVideos("Avatar").first().size)

        // Insert new matching video
        repository.upsertVideos(
            listOf(sampleDomainVideo(id = "2", title = "Avatar 2", mediaUri = "u2"))
        )
        assertEquals(2, repository.searchVideos("Avatar").first().size)

        // Delete video
        repository.deleteVideo("1")
        val updated = repository.searchVideos("Avatar").first()
        assertEquals(1, updated.size)
        assertEquals("2", updated[0].id)
    }

    @Test
    fun repositoryHistoryOperations() = runTest {
        repository.upsertVideos(
            listOf(
                sampleDomainVideo(id = "1", title = "V1", lastPlayedAt = 1000L, playbackPositionMs = 20000L, isCompleted = false),
                sampleDomainVideo(id = "2", title = "V2", lastPlayedAt = 3000L, playbackPositionMs = 50000L, isCompleted = true),
                sampleDomainVideo(id = "3", title = "V3", lastPlayedAt = null)
            )
        )

        val history = repository.getAllHistoryVideos().first()
        assertEquals(2, history.size)
        assertEquals("V2", history[0].title)
        assertEquals("V1", history[1].title)

        // Clear single video
        repository.clearHistoryForVideo("1")
        val afterClearSingle = repository.getAllHistoryVideos().first()
        assertEquals(1, afterClearSingle.size)
        assertEquals("V2", afterClearSingle[0].title)

        // Clear all history
        repository.clearAllHistory()
        val afterClearAll = repository.getAllHistoryVideos().first()
        assertEquals(0, afterClearAll.size)
    }

    @Test
    fun repositoryRestartPlayback() = runTest {
        repository.upsertVideos(
            listOf(
                sampleDomainVideo(id = "restart_repo", title = "Completed Video", isCompleted = true, playbackPositionMs = 99000L, playbackPercentage = 0.99f, lastPlayedAt = 1000L)
            )
        )

        repository.restartPlayback("restart_repo", 5000L)

        val video = repository.getVideoById("restart_repo")
        assertNotNull(video)
        assertEquals(0L, video?.playbackPositionMs)
        assertEquals(0f, video?.playbackPercentage ?: 1f, 0.001f)
        assertEquals(false, video?.isCompleted)
        assertEquals(5000L, video?.lastPlayedAt)
    }
}
