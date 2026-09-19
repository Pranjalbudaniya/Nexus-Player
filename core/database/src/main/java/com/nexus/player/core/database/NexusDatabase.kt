package com.nexus.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.entity.VideoEntity

/**
 * Main Room database for Nexus Player.
 *
 * Schema version 1: Initial local video metadata table [VideoEntity].
 * Robust migration strategy: migrations are explicitly registered without destructive fallbacks.
 */
@Database(
    entities = [VideoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao

    companion object {
        const val DATABASE_NAME = "nexus_player.db"

        // Future Room migrations should be declared here, e.g.:
        // val MIGRATION_1_2 = object : Migration(1, 2) { ... }
    }
}
