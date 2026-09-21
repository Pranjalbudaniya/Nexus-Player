package com.nexus.player.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NexusDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NexusDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesExistingVideosAndCreatesPlaylistTables() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO videos (
                    id, mediaUri, filePath, fileName, title, folderName, folderPath,
                    sizeBytes, durationMs, width, height, resolutionLabel,
                    dateAdded, lastModified, lastPlayedAt, playbackPositionMs,
                    playbackPercentage, isFavorite, watchCount, audioTrackCount,
                    subtitleTrackCount
                ) VALUES (
                    'v_migrated', 'content://test/1', '/path/test.mp4', 'test.mp4', 'Migrated Video',
                    'Movies', '/path', 1000, 60000, 1920, 1080, '1080p',
                    1000, 1000, NULL, 0, 0.0, 0, 0, 1, 0
                )
                """.trimIndent()
            )
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, NexusDatabase.MIGRATION_1_2)

        // Verify video still exists
        val cursor = db.query("SELECT id, title FROM videos WHERE id = 'v_migrated'")
        assertTrue(cursor.moveToFirst())
        assertEquals("v_migrated", cursor.getString(0))
        assertEquals("Migrated Video", cursor.getString(1))
        cursor.close()

        // Verify playlists table works
        db.execSQL("INSERT INTO playlists (id, name, description, createdAt, updatedAt) VALUES ('p1', 'My Playlist', 'Desc', 100, 100)")
        val plCursor = db.query("SELECT id, name FROM playlists WHERE id = 'p1'")
        assertTrue(plCursor.moveToFirst())
        assertEquals("My Playlist", plCursor.getString(1))
        plCursor.close()

        // Verify playlist_items table works
        db.execSQL("INSERT INTO playlist_items (playlistId, videoId, position, addedAt) VALUES ('p1', 'v_migrated', 0, 100)")
        val itemCursor = db.query("SELECT playlistId, videoId, position FROM playlist_items WHERE playlistId = 'p1'")
        assertTrue(itemCursor.moveToFirst())
        assertEquals("v_migrated", itemCursor.getString(1))
        assertEquals(0, itemCursor.getInt(2))
        itemCursor.close()
    }

    @Test
    fun migrate2To3_addsIsCompletedColumnAndIndex() {
        var db = helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO videos (
                    id, mediaUri, filePath, fileName, title, folderName, folderPath,
                    sizeBytes, durationMs, width, height, resolutionLabel,
                    dateAdded, lastModified, lastPlayedAt, playbackPositionMs,
                    playbackPercentage, isFavorite, watchCount, audioTrackCount,
                    subtitleTrackCount
                ) VALUES (
                    'v_v2', 'content://test/2', '/path/test2.mp4', 'test2.mp4', 'V2 Video',
                    'Movies', '/path', 1000, 60000, 1920, 1080, '1080p',
                    1000, 1000, NULL, 0, 0.0, 0, 0, 1, 0
                )
                """.trimIndent()
            )
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, NexusDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT id, isCompleted FROM videos WHERE id = 'v_v2'")
        assertTrue(cursor.moveToFirst())
        assertEquals("v_v2", cursor.getString(0))
        assertEquals(0, cursor.getInt(1))
        cursor.close()
    }

    @Test
    fun migrate3To4_createsCompositeAndTitleIndices() {
        var db = helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO videos (
                    id, mediaUri, filePath, fileName, title, folderName, folderPath,
                    sizeBytes, durationMs, width, height, resolutionLabel,
                    dateAdded, lastModified, lastPlayedAt, playbackPositionMs,
                    playbackPercentage, isFavorite, watchCount, audioTrackCount,
                    subtitleTrackCount, isCompleted
                ) VALUES (
                    'v_v3', 'content://test/3', '/path/test3.mp4', 'test3.mp4', 'V3 Video',
                    'Movies', '/path', 1000, 60000, 1920, 1080, '1080p',
                    1000, 1000, NULL, 0, 0.0, 0, 0, 1, 0, 1
                )
                """.trimIndent()
            )
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, NexusDatabase.MIGRATION_3_4)

        val cursor = db.query("SELECT id, title, isCompleted FROM videos WHERE id = 'v_v3'")
        assertTrue(cursor.moveToFirst())
        assertEquals("v_v3", cursor.getString(0))
        assertEquals("V3 Video", cursor.getString(1))
        assertEquals(1, cursor.getInt(2))
        cursor.close()
    }

    @Test
    fun migrateAll_1To4_migratesCleanlyAcrossAllVersions() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO videos (
                    id, mediaUri, filePath, fileName, title, folderName, folderPath,
                    sizeBytes, durationMs, width, height, resolutionLabel,
                    dateAdded, lastModified, lastPlayedAt, playbackPositionMs,
                    playbackPercentage, isFavorite, watchCount, audioTrackCount,
                    subtitleTrackCount
                ) VALUES (
                    'v_all', 'content://test/all', '/path/all.mp4', 'all.mp4', 'All Video',
                    'Movies', '/path', 2000, 120000, 1920, 1080, '1080p',
                    500, 500, NULL, 1000, 0.1, 1, 1, 2, 1
                )
                """.trimIndent()
            )
            close()
        }

        db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            NexusDatabase.MIGRATION_1_2,
            NexusDatabase.MIGRATION_2_3,
            NexusDatabase.MIGRATION_3_4
        )

        val cursor = db.query("SELECT id, title, isFavorite, isCompleted FROM videos WHERE id = 'v_all'")
        assertTrue(cursor.moveToFirst())
        assertEquals("v_all", cursor.getString(0))
        assertEquals("All Video", cursor.getString(1))
        assertEquals(1, cursor.getInt(2))
        assertEquals(0, cursor.getInt(3))
        cursor.close()
    }
}
