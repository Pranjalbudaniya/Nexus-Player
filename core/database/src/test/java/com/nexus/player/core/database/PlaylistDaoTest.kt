package com.nexus.player.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.database.dao.PlaylistDao
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.entity.PlaylistEntity
import com.nexus.player.core.database.entity.PlaylistItemEntity
import com.nexus.player.core.database.entity.VideoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PlaylistDaoTest {

    private lateinit var database: NexusDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var videoDao: VideoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = database.playlistDao()
        videoDao = database.videoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    private fun createSampleVideo(id: String = "v1", title: String = "Test Video") = VideoEntity(
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
    fun insertAndRetrievePlaylist() = runTest {
        val playlist = PlaylistEntity(
            id = "pl_1",
            name = "Favorites",
            description = "My favorite videos",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        playlistDao.insertPlaylist(playlist)

        val retrieved = playlistDao.getPlaylistById("pl_1")
        assertNotNull(retrieved)
        assertEquals("Favorites", retrieved?.name)
        assertEquals("My favorite videos", retrieved?.description)

        val byName = playlistDao.getPlaylistByName("favorites")
        assertNotNull(byName)
        assertEquals("pl_1", byName?.id)
    }

    @Test
    fun deletePlaylist_cascadesToItems() = runTest {
        val playlist = PlaylistEntity(
            id = "pl_del",
            name = "To Delete",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        playlistDao.insertPlaylist(playlist)

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = "pl_del",
                videoId = "v_1",
                position = 0,
                addedAt = 1000L
            )
        )
        assertEquals(1, playlistDao.getItemCount("pl_del"))

        playlistDao.deletePlaylistById("pl_del")
        assertNull(playlistDao.getPlaylistById("pl_del"))
        assertEquals(0, playlistDao.getItemCount("pl_del"))
    }

    @Test
    fun duplicateVideoInSamePlaylist_throwsConstraintException() = runTest {
        val playlist = PlaylistEntity(
            id = "pl_unique",
            name = "Unique Test",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        playlistDao.insertPlaylist(playlist)

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = "pl_unique",
                videoId = "v_dup",
                position = 0,
                addedAt = 1000L
            )
        )

        try {
            playlistDao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = "pl_unique",
                    videoId = "v_dup",
                    position = 1,
                    addedAt = 1001L
                )
            )
            fail("Expected SQLiteConstraintException on duplicate (playlistId, videoId)")
        } catch (e: SQLiteConstraintException) {
            // Success
        }
    }

    @Test
    fun playlistWithItems_resolvesVideoMetadataAndHandlesMissingVideos() = runTest {
        val v1 = createSampleVideo(id = "v_exist", title = "Existing Video")
        videoDao.insertVideo(v1)

        val playlist = PlaylistEntity(
            id = "pl_rel",
            name = "Relation Test",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        playlistDao.insertPlaylist(playlist)

        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(playlistId = "pl_rel", videoId = "v_exist", position = 0, addedAt = 100L)
        )
        // v_missing is never inserted into videoDao!
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(playlistId = "pl_rel", videoId = "v_missing", position = 1, addedAt = 200L)
        )

        val items = playlistDao.observePlaylistItemsWithVideo("pl_rel").first()
        assertEquals(2, items.size)

        // Item 0: Video exists
        assertEquals("v_exist", items[0].item.videoId)
        assertNotNull(items[0].video)
        assertEquals("Existing Video", items[0].video?.title)

        // Item 1: Video is missing
        assertEquals("v_missing", items[1].item.videoId)
        assertNull(items[1].video)
    }

    @Test
    fun reorderItems_updatesPositionsCorrectly() = runTest {
        val playlist = PlaylistEntity(id = "pl_reorder", name = "Reorder", createdAt = 1000L, updatedAt = 1000L)
        playlistDao.insertPlaylist(playlist)

        playlistDao.insertPlaylistItem(PlaylistItemEntity(playlistId = "pl_reorder", videoId = "v1", position = 0, addedAt = 100L))
        playlistDao.insertPlaylistItem(PlaylistItemEntity(playlistId = "pl_reorder", videoId = "v2", position = 1, addedAt = 200L))
        playlistDao.insertPlaylistItem(PlaylistItemEntity(playlistId = "pl_reorder", videoId = "v3", position = 2, addedAt = 300L))

        assertEquals(listOf("v1", "v2", "v3"), playlistDao.getPlaylistVideoIds("pl_reorder"))

        // Reverse order: v3, v1, v2
        playlistDao.reorderItems("pl_reorder", listOf("v3", "v1", "v2"))

        assertEquals(listOf("v3", "v1", "v2"), playlistDao.getPlaylistVideoIds("pl_reorder"))
        val items = playlistDao.getPlaylistItems("pl_reorder")
        assertEquals(0, items.first { it.videoId == "v3" }.position)
        assertEquals(1, items.first { it.videoId == "v1" }.position)
        assertEquals(2, items.first { it.videoId == "v2" }.position)
    }

    @Test
    fun observePlaylistsWithCount_calculatesItemCount() = runTest {
        val p1 = PlaylistEntity(id = "p1", name = "Empty Playlist", createdAt = 100L, updatedAt = 100L)
        val p2 = PlaylistEntity(id = "p2", name = "Full Playlist", createdAt = 200L, updatedAt = 200L)
        playlistDao.insertPlaylist(p1)
        playlistDao.insertPlaylist(p2)

        playlistDao.insertPlaylistItem(PlaylistItemEntity(playlistId = "p2", videoId = "v1", position = 0, addedAt = 100L))
        playlistDao.insertPlaylistItem(PlaylistItemEntity(playlistId = "p2", videoId = "v2", position = 1, addedAt = 200L))

        val summaries = playlistDao.observePlaylistsWithCount().first()
        assertEquals(2, summaries.size)

        val s2 = summaries.first { it.id == "p2" }
        assertEquals(2, s2.itemCount)

        val s1 = summaries.first { it.id == "p1" }
        assertEquals(0, s1.itemCount)
    }
}
