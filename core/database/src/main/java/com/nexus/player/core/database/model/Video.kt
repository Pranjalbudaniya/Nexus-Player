package com.nexus.player.core.database.model

import com.nexus.player.core.database.dao.FolderSummary
import com.nexus.player.core.database.entity.VideoEntity

/**
 * Clean domain representation of a video media item.
 * Kept strictly decoupled from Room annotations to prevent database leak into UI/domain logic.
 */
data class Video(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val fileName: String,
    val title: String,
    val folderName: String,
    val folderPath: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val resolutionLabel: String,
    val videoCodec: String? = null,
    val videoBitrate: Long? = null,
    val frameRate: Float? = null,
    val audioCodec: String? = null,
    val audioTrackCount: Int = 1,
    val subtitleTrackCount: Int = 0,
    val dateAdded: Long,
    val lastModified: Long,
    val lastPlayedAt: Long? = null,
    val playbackPositionMs: Long = 0L,
    val playbackPercentage: Float = 0.0f,
    val isFavorite: Boolean = false,
    val watchCount: Int = 0,
    val extraMetadata: String? = null
)

/**
 * Domain representation of a media folder.
 */
data class VideoFolder(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val previewMediaUri: String? = null,
    val lastModified: Long = 0L,
    val parentPath: String? = null
)

/**
 * Derives the parent folder path from a folder path or returns null if it is at the root.
 */
fun deriveParentPath(folderPath: String): String? {
    val clean = folderPath.trimEnd('/')
    val lastSlash = clean.lastIndexOf('/')
    return if (lastSlash > 0) clean.substring(0, lastSlash) else null
}

/**
 * Supported sorting orders for video listings.
 */
enum class VideoSortOrder {
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    DURATION_DESC,
    DURATION_ASC,
    SIZE_DESC,
    SIZE_ASC,
    LAST_PLAYED_DESC,
    LAST_PLAYED_ASC
}

/**
 * Map [VideoEntity] to domain [Video].
 */
fun VideoEntity.asDomain(): Video = Video(
    id = id,
    mediaUri = mediaUri,
    filePath = filePath,
    fileName = fileName,
    title = title,
    folderName = folderName,
    folderPath = folderPath,
    sizeBytes = sizeBytes,
    durationMs = durationMs,
    width = width,
    height = height,
    resolutionLabel = resolutionLabel,
    videoCodec = videoCodec,
    videoBitrate = videoBitrate,
    frameRate = frameRate,
    audioCodec = audioCodec,
    audioTrackCount = audioTrackCount,
    subtitleTrackCount = subtitleTrackCount,
    dateAdded = dateAdded,
    lastModified = lastModified,
    lastPlayedAt = lastPlayedAt,
    playbackPositionMs = playbackPositionMs,
    playbackPercentage = playbackPercentage,
    isFavorite = isFavorite,
    watchCount = watchCount,
    extraMetadata = extraMetadata
)

/**
 * Map domain [Video] to [VideoEntity].
 */
fun Video.asEntity(): VideoEntity = VideoEntity(
    id = id,
    mediaUri = mediaUri,
    filePath = filePath,
    fileName = fileName,
    title = title,
    folderName = folderName,
    folderPath = folderPath,
    sizeBytes = sizeBytes,
    durationMs = durationMs,
    width = width,
    height = height,
    resolutionLabel = resolutionLabel,
    videoCodec = videoCodec,
    videoBitrate = videoBitrate,
    frameRate = frameRate,
    audioCodec = audioCodec,
    audioTrackCount = audioTrackCount,
    subtitleTrackCount = subtitleTrackCount,
    dateAdded = dateAdded,
    lastModified = lastModified,
    lastPlayedAt = lastPlayedAt,
    playbackPositionMs = playbackPositionMs,
    playbackPercentage = playbackPercentage,
    isFavorite = isFavorite,
    watchCount = watchCount,
    extraMetadata = extraMetadata
)

/**
 * Map [FolderSummary] to domain [VideoFolder].
 */
fun FolderSummary.asDomain(): VideoFolder = VideoFolder(
    folderPath = folderPath,
    folderName = folderName,
    videoCount = videoCount,
    previewMediaUri = previewMediaUri,
    lastModified = lastModified,
    parentPath = deriveParentPath(folderPath)
)
