package com.nexus.player.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a locally discovered video file and its metadata.
 *
 * Indexed for optimal querying of:
 * - Duplicate prevention & direct lookup via [mediaUri]
 * - Favorites section via [isFavorite]
 * - Recently added sorting via [dateAdded]
 * - History and continue watching via [lastPlayedAt]
 * - Folder navigation and grouping via [folderPath]
 */
@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["mediaUri"], unique = true),
        Index(value = ["isFavorite"]),
        Index(value = ["dateAdded"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["folderPath"]),
        Index(value = ["isCompleted"])
    ]
)
data class VideoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "mediaUri")
    val mediaUri: String,

    @ColumnInfo(name = "filePath")
    val filePath: String?,

    @ColumnInfo(name = "fileName")
    val fileName: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "folderName")
    val folderName: String,

    @ColumnInfo(name = "folderPath")
    val folderPath: String,

    @ColumnInfo(name = "sizeBytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "durationMs")
    val durationMs: Long,

    @ColumnInfo(name = "width")
    val width: Int,

    @ColumnInfo(name = "height")
    val height: Int,

    @ColumnInfo(name = "resolutionLabel")
    val resolutionLabel: String,

    @ColumnInfo(name = "videoCodec")
    val videoCodec: String? = null,

    @ColumnInfo(name = "videoBitrate")
    val videoBitrate: Long? = null,

    @ColumnInfo(name = "frameRate")
    val frameRate: Float? = null,

    @ColumnInfo(name = "audioCodec")
    val audioCodec: String? = null,

    @ColumnInfo(name = "audioTrackCount")
    val audioTrackCount: Int = 1,

    @ColumnInfo(name = "subtitleTrackCount")
    val subtitleTrackCount: Int = 0,

    @ColumnInfo(name = "dateAdded")
    val dateAdded: Long,

    @ColumnInfo(name = "lastModified")
    val lastModified: Long,

    @ColumnInfo(name = "lastPlayedAt")
    val lastPlayedAt: Long? = null,

    @ColumnInfo(name = "playbackPositionMs")
    val playbackPositionMs: Long = 0L,

    @ColumnInfo(name = "playbackPercentage")
    val playbackPercentage: Float = 0.0f,

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "watchCount")
    val watchCount: Int = 0,

    @ColumnInfo(name = "extraMetadata")
    val extraMetadata: String? = null
)
