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
        isFavorite: Boolean = false
    ) = Video(
        id = id,
        mediaUri = mediaUri,
        filePath = "/storage$folderPath/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = folderName,
        folderPath = folderPath,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        videoCodec = "H.264",
        dateAdded = dateAdded,
        lastModified = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = isFavorite
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
}
