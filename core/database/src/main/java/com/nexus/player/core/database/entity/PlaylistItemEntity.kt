package com.nexus.player.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an item link between a playlist and a video.
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "videoId"], unique = true),
        Index(value = ["playlistId", "position"]),
        Index(value = ["playlistId"])
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "playlistId")
    val playlistId: String,

    @ColumnInfo(name = "videoId")
    val videoId: String,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "addedAt")
    val addedAt: Long
)
