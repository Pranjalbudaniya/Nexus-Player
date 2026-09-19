package com.nexus.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.player.core.database.dao.PlaylistDao
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.entity.PlaylistEntity
import com.nexus.player.core.database.entity.PlaylistItemEntity
import com.nexus.player.core.database.entity.VideoEntity

/**
 * Main Room database for Nexus Player.
 *
 * Schema version 1: Initial local video metadata table [VideoEntity].
 * Schema version 2: User playlists and playlist-items relationship [PlaylistEntity], [PlaylistItemEntity].
 * Robust migration strategy: migrations are explicitly registered without destructive fallbacks.
 */
@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "nexus_player.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlists_name`
                    ON `playlists` (`name`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlists_updatedAt`
                    ON `playlists` (`updatedAt`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playlistId` TEXT NOT NULL,
                        `videoId` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_items_playlistId_videoId`
                    ON `playlist_items` (`playlistId`, `videoId`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId_position`
                    ON `playlist_items` (`playlistId`, `position`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId`
                    ON `playlist_items` (`playlistId`)
                    """.trimIndent()
                )
            }
        }
    }
}
