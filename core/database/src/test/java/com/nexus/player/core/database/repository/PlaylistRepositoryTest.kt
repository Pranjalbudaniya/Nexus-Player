package com.nexus.player.core.database.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.database.NexusDatabase
import com.nexus.player.core.database.dao.PlaylistDao
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.entity.VideoEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryTest {

    private lateinit var database: NexusDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var videoDao: VideoDao
    private lateinit var repository: PlaylistRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = database.playlistDao()
        videoDao = database.videoDao()
        repository = PlaylistRepositoryImpl(
            playlistDao = playlistDao,
            ioDispatcher = testDispatcher
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    private fun createSampleVideo(id: String, title: String) = VideoEntity(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/sdcard/$id.mp4",
        fileName = "$id.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/sdcard/Movies",
        sizeBytes = 1000L,
        durationMs = 60000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        dateAdded = 1000L,
        lastModified = 1000L
    )

    @Test
    fun createPlaylist_validationAndCreation() = runTest {
        // Empty or blank name
        val emptyResult = repository.createPlaylist("   ")
        assertTrue(emptyResult.isFailure)

        // Excessively long name
        val longName = "A".repeat(101)
        val longResult = repository.createPlaylist(longName)
        assertTrue(longResult.isFailure)

        // Valid name
        val result = repository.createPlaylist("Action Movies", "Fast-paced movies")
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()

        val playlist = repository.getPlaylistById(id)
        assertNotNull(playlist)
        assertEquals("Action Movies", playlist?.name)
        assertEquals("Fast-paced movies", playlist?.description)
        assertEquals(0, playlist?.itemCount)

        // Duplicate name (case-insensitive)
        val duplicateResult = repository.createPlaylist("action movies")
        assertTrue(duplicateResult.isFailure)
    }

    @Test
    fun renamePlaylist_validationAndExecution() = runTest {
        val id = repository.createPlaylist("Sci-Fi").getOrThrow()

        // Rename to invalid
        val emptyRename = repository.renamePlaylist(id, "  ")
        assertTrue(emptyRename.isFailure)

        // Rename to valid
        val validRename = repository.renamePlaylist(id, "Science Fiction")
        assertTrue(validRename.isSuccess)

        val updated = repository.getPlaylistById(id)
        assertEquals("Science Fiction", updated?.name)

        // Rename to duplicate of another playlist
        repository.createPlaylist("Drama")
        val duplicateRename = repository.renamePlaylist(id, "drama")
        assertTrue(duplicateRename.isFailure)
    }

    @Test
    fun deletePlaylist_preservesLibraryVideos() = runTest {
        val video = createSampleVideo("v_keep", "Keep Me")
        videoDao.insertVideo(video)

        val id = repository.createPlaylist("Temp Playlist").getOrThrow()
        repository.addVideoToPlaylist(id, "v_keep")

        assertEquals(1, repository.getPlaylistById(id)?.itemCount)

        repository.deletePlaylist(id)

        assertNull(repository.getPlaylistById(id))
        // Verify video entity is untouched
        assertNotNull(videoDao.getVideoById("v_keep"))
    }

    @Test
    fun addVideoToPlaylist_preventsDuplicatesAndAssignsPositions() = runTest {
        val id = repository.createPlaylist("Favorites").getOrThrow()

        val res1 = repository.addVideoToPlaylist(id, "v1")
        assertTrue(res1.isSuccess)

        val res2 = repository.addVideoToPlaylist(id, "v2")
        assertTrue(res2.isSuccess)

        // Duplicate addition
        val dupRes = repository.addVideoToPlaylist(id, "v1")
        assertTrue(dupRes.isFailure)

        val videoIds = repository.getPlaylistVideoIds(id)
        assertEquals(listOf("v1", "v2"), videoIds)
    }

    @Test
    fun removeVideoFromPlaylist_recompactsPositions() = runTest {
        val id = repository.createPlaylist("Queue").getOrThrow()
        repository.addVideoToPlaylist(id, "v1")
        repository.addVideoToPlaylist(id, "v2")
        repository.addVideoToPlaylist(id, "v3")

        repository.removeVideoFromPlaylist(id, "v2")

        val remaining = repository.getPlaylistVideoIds(id)
        assertEquals(listOf("v1", "v3"), remaining)

        val items = playlistDao.getPlaylistItems(id)
        assertEquals(0, items[0].position)
        assertEquals(1, items[1].position)
    }

    @Test
    fun reorderVideos_persistsNewOrder() = runTest {
        val id = repository.createPlaylist("Order Test").getOrThrow()
        repository.addVideoToPlaylist(id, "v1")
        repository.addVideoToPlaylist(id, "v2")
        repository.addVideoToPlaylist(id, "v3")

        repository.reorderVideos(id, listOf("v3", "v1", "v2"))

        assertEquals(listOf("v3", "v1", "v2"), repository.getPlaylistVideoIds(id))
    }

    @Test
    fun observePlaylistVideos_marksMissingVideosAsUnavailable() = runTest {
        val v1 = createSampleVideo("v_available", "Available Video")
        videoDao.insertVideo(v1)

        val id = repository.createPlaylist("Availability Test").getOrThrow()
        repository.addVideoToPlaylist(id, "v_available")
        repository.addVideoToPlaylist(id, "v_deleted_from_disk")

        val items = repository.observePlaylistVideos(id).first()
        assertEquals(2, items.size)

        val item1 = items[0]
        assertEquals("v_available", item1.videoId)
        assertTrue(item1.isAvailable)
        assertNotNull(item1.video)

        val item2 = items[1]
        assertEquals("v_deleted_from_disk", item2.videoId)
        assertFalse(item2.isAvailable)
        assertNull(item2.video)
    }
}
