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
}
