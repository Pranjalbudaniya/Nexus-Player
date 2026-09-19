package com.nexus.player.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.entity.VideoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class VideoDaoTest {

    private lateinit var database: NexusDatabase
    private lateinit var videoDao: VideoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoDao = database.videoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    private fun createSampleVideo(
        id: String = "video_1",
        mediaUri: String = "content://media/external/video/media/1",
        title: String = "Big Buck Bunny",
        folderName: String = "Movies",
        folderPath: String = "/storage/emulated/0/Movies",
        sizeBytes: Long = 104857600L,
        durationMs: Long = 600000L,
        dateAdded: Long = 1000L,
        lastPlayedAt: Long? = null,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0.0f,
        isFavorite: Boolean = false,
        watchCount: Int = 0
    ) = VideoEntity(
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
        resolutionLabel = "1080p",
        videoCodec = "H.264",
        videoBitrate = 4000000L,
        frameRate = 30.0f,
        audioCodec = "AAC",
        audioTrackCount = 2,
        subtitleTrackCount = 1,
        dateAdded = dateAdded,
        lastModified = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isFavorite = isFavorite,
        watchCount = watchCount
    )

    @Test
    fun databaseBehaviorWithEmptyData() = runTest {
        assertEquals(0, videoDao.getVideosCount())
        assertTrue(videoDao.getAllVideosByTitleAsc().first().isEmpty())
        assertTrue(videoDao.getFavoriteVideos().first().isEmpty())
        assertTrue(videoDao.getContinueWatchingVideos().first().isEmpty())
        assertTrue(videoDao.getHistoryVideos().first().isEmpty())
        assertTrue(videoDao.getFolders().first().isEmpty())
        assertNull(videoDao.getVideoById("non_existent"))
        assertNull(videoDao.getVideoByUri("content://non_existent"))
    }

    @Test
    fun insertAndUpdateMedia() = runTest {
        val video = createSampleVideo(id = "vid_1", title = "Original Title")
        videoDao.insertVideo(video)

        assertEquals(1, videoDao.getVideosCount())
        val fetched = videoDao.getVideoById("vid_1")
        assertNotNull(fetched)
        assertEquals("Original Title", fetched?.title)
        assertEquals("1080p", fetched?.resolutionLabel)
        assertEquals("H.264", fetched?.videoCodec)

        // Update with upsert
        val updated = video.copy(title = "Updated Title", videoCodec = "HEVC")
        videoDao.upsertVideo(updated)

        assertEquals(1, videoDao.getVideosCount())
        val reFetched = videoDao.getVideoById("vid_1")
        assertEquals("Updated Title", reFetched?.title)
        assertEquals("HEVC", reFetched?.videoCodec)
    }

    @Test
    fun queryFavorites() = runTest {
        videoDao.upsertVideos(
            listOf(
                createSampleVideo(id = "1", mediaUri = "uri_1", title = "A Fav", isFavorite = true),
                createSampleVideo(id = "2", mediaUri = "uri_2", title = "B Not Fav", isFavorite = false),
                createSampleVideo(id = "3", mediaUri = "uri_3", title = "C Fav", isFavorite = true)
            )
        )

        val favorites = videoDao.getFavoriteVideos().first()
        assertEquals(2, favorites.size)
        assertEquals("A Fav", favorites[0].title)
        assertEquals("C Fav", favorites[1].title)

        // Toggle favorite off
        videoDao.updateFavorite("1", false)
        val updatedFavorites = videoDao.getFavoriteVideos().first()
        assertEquals(1, updatedFavorites.size)
        assertEquals("C Fav", updatedFavorites[0].title)
    }

    @Test
    fun queryContinueWatchingItems() = runTest {
        videoDao.upsertVideos(
            listOf(
                // 1. Not started (position 0) -> Should NOT appear
                createSampleVideo(
                    id = "1", mediaUri = "uri_1", title = "Not Started",
                    playbackPositionMs = 0L, playbackPercentage = 0.0f, lastPlayedAt = null
                ),
                // 2. In progress -> Should appear
                createSampleVideo(
                    id = "2", mediaUri = "uri_2", title = "In Progress Older",
                    playbackPositionMs = 30000L, playbackPercentage = 0.30f, lastPlayedAt = 1000L
                ),
                // 3. In progress newer -> Should appear first
                createSampleVideo(
                    id = "3", mediaUri = "uri_3", title = "In Progress Newer",
                    playbackPositionMs = 50000L, playbackPercentage = 0.50f, lastPlayedAt = 2000L
                ),
                // 4. Completed (percentage >= 0.95) -> Should NOT appear in Continue Watching
                createSampleVideo(
                    id = "4", mediaUri = "uri_4", title = "Completed",
                    playbackPositionMs = 590000L, playbackPercentage = 0.98f, lastPlayedAt = 3000L
                )
            )
        )

        val continueWatching = videoDao.getContinueWatchingVideos(limit = 10).first()
        assertEquals(2, continueWatching.size)
        // Ordered by lastPlayedAt DESC
        assertEquals("In Progress Newer", continueWatching[0].title)
        assertEquals("In Progress Older", continueWatching[1].title)
    }

    @Test
    fun queryRecentlyAddedItems() = runTest {
        videoDao.upsertVideos(
            listOf(
                createSampleVideo(id = "1", mediaUri = "uri_1", title = "Old", dateAdded = 100L),
                createSampleVideo(id = "2", mediaUri = "uri_2", title = "Newest", dateAdded = 300L),
                createSampleVideo(id = "3", mediaUri = "uri_3", title = "Middle", dateAdded = 200L)
            )
        )

        val recent = videoDao.getRecentlyAddedVideos(limit = 2).first()
        assertEquals(2, recent.size)
        assertEquals("Newest", recent[0].title)
        assertEquals("Middle", recent[1].title)
    }

    @Test
    fun queryHistory() = runTest {
        videoDao.upsertVideos(
            listOf(
                createSampleVideo(id = "1", mediaUri = "uri_1", title = "Never Played", lastPlayedAt = null),
                createSampleVideo(id = "2", mediaUri = "uri_2", title = "Played Yesterday", lastPlayedAt = 1000L),
                createSampleVideo(id = "3", mediaUri = "uri_3", title = "Played Today", lastPlayedAt = 2000L)
            )
        )

        val history = videoDao.getHistoryVideos(limit = 10).first()
        assertEquals(2, history.size)
        assertEquals("Played Today", history[0].title)
        assertEquals("Played Yesterday", history[1].title)
    }

    @Test
    fun folderFilteringAndSummaries() = runTest {
        videoDao.upsertVideos(
            listOf(
                createSampleVideo(id = "1", mediaUri = "uri_1", title = "Bunny", folderName = "Movies", folderPath = "/Movies"),
                createSampleVideo(id = "2", mediaUri = "uri_2", title = "Elephants", folderName = "Movies", folderPath = "/Movies"),
                createSampleVideo(id = "3", mediaUri = "uri_3", title = "Intro", folderName = "Downloads", folderPath = "/Downloads")
            )
        )

        val movieVideos = videoDao.getVideosByFolder("/Movies").first()
        assertEquals(2, movieVideos.size)

        val folders = videoDao.getFolders().first()
        assertEquals(2, folders.size)
        assertEquals("Downloads", folders[0].folderName)
        assertEquals(1, folders[0].videoCount)
        assertEquals("Movies", folders[1].folderName)
        assertEquals(2, folders[1].videoCount)
    }

    @Test
    fun stableIdentifierLookup() = runTest {
        val video = createSampleVideo(id = "stable_abc_123", mediaUri = "content://media/external/video/media/999")
        videoDao.insertVideo(video)

        val byId = videoDao.getVideoById("stable_abc_123")
        assertNotNull(byId)
        assertEquals("stable_abc_123", byId?.id)

        val byUri = videoDao.getVideoByUri("content://media/external/video/media/999")
        assertNotNull(byUri)
        assertEquals("content://media/external/video/media/999", byUri?.mediaUri)
    }

    @Test
    fun playbackProgressUpdates() = runTest {
        val video = createSampleVideo(id = "vid_progress", durationMs = 100000L, watchCount = 0)
        videoDao.insertVideo(video)

        // Partial progress update
        videoDao.updatePlaybackProgress(
            id = "vid_progress",
            positionMs = 50000L,
            percentage = 0.50f,
            lastPlayedAt = 12345L
        )

        val partial = videoDao.getVideoById("vid_progress")
        assertEquals(50000L, partial?.playbackPositionMs)
        assertEquals(0.50f, partial?.playbackPercentage ?: 0f, 0.001f)
        assertEquals(12345L, partial?.lastPlayedAt)
        assertEquals(0, partial?.watchCount)

        // Near completion (>= 90%) -> watchCount increments
        videoDao.updatePlaybackProgress(
            id = "vid_progress",
            positionMs = 95000L,
            percentage = 0.95f,
            lastPlayedAt = 67890L
        )

        val completed = videoDao.getVideoById("vid_progress")
        assertEquals(95000L, completed?.playbackPositionMs)
        assertEquals(0.95f, completed?.playbackPercentage ?: 0f, 0.001f)
        assertEquals(67890L, completed?.lastPlayedAt)
        assertEquals(1, completed?.watchCount)
    }

    @Test
    fun duplicateMediaPrevention() = runTest {
        val video1 = createSampleVideo(id = "id_1", mediaUri = "content://media/duplicate_check", title = "First")
        videoDao.insertVideo(video1)

        // Attempting to insert a duplicate with different ID but same unique mediaUri must throw SQLiteConstraintException
        val video2WithSameUri = createSampleVideo(id = "id_2", mediaUri = "content://media/duplicate_check", title = "Duplicate URI")
        try {
            videoDao.insertVideo(video2WithSameUri)
            fail("Expected SQLiteConstraintException due to duplicate mediaUri")
        } catch (e: SQLiteConstraintException) {
            // Expected
        }

        // Total count in database remains exactly 1
        assertEquals(1, videoDao.getVideosCount())

        // Upserting with same ID updates the record without creating duplicate
        val video1Updated = video1.copy(title = "First Renamed")
        videoDao.upsertVideo(video1Updated)
        assertEquals(1, videoDao.getVideosCount())
        assertEquals("First Renamed", videoDao.getVideoById("id_1")?.title)
    }

    @Test
    fun deleteStaleVideos() = runTest {
        videoDao.upsertVideos(
            listOf(
                createSampleVideo(id = "valid_1", mediaUri = "uri_1"),
                createSampleVideo(id = "valid_2", mediaUri = "uri_2"),
                createSampleVideo(id = "stale_3", mediaUri = "uri_3")
            )
        )
        assertEquals(3, videoDao.getVideosCount())

        videoDao.deleteStaleVideos(listOf("valid_1", "valid_2"))
        assertEquals(2, videoDao.getVideosCount())
        assertNull(videoDao.getVideoById("stale_3"))
        assertNotNull(videoDao.getVideoById("valid_1"))
        assertNotNull(videoDao.getVideoById("valid_2"))
    }
}
