package com.nexus.player.feature.home.domain.model

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.media.model.formatFileSize
import com.nexus.player.feature.home.ContinueWatchingItem
import com.nexus.player.feature.home.FavoriteVideoItem
import com.nexus.player.feature.home.FolderItem
import com.nexus.player.feature.home.RecentVideoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain representation of an in-progress video eligible for the Continue Watching section.
 * Preserves exact playback position and metadata.
 */
data class ContinueWatchingVideo(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val title: String,
    val durationMs: Long,
    val formattedDuration: String,
    val playbackPositionMs: Long,
    val playbackPercentage: Float,
    val quality: String,
    val lastPlayedAt: Long
) {
    fun toUiItem(): ContinueWatchingItem = ContinueWatchingItem(
        id = id,
        title = title,
        duration = formattedDuration,
        progress = playbackPercentage,
        quality = quality,
        playbackPositionMs = playbackPositionMs,
        mediaUri = mediaUri
    )
}

/**
 * Domain representation of a recently discovered or added video.
 */
data class RecentlyAddedVideo(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val title: String,
    val durationMs: Long,
    val formattedDuration: String,
    val technicalSpecs: String,
    val addedTimeAndFolder: String,
    val quality: String,
    val dateAdded: Long
) {
    fun toUiItem(): RecentVideoItem = RecentVideoItem(
        id = id,
        title = title,
        technicalSpecs = technicalSpecs,
        addedTimeAndFolder = addedTimeAndFolder,
        duration = formattedDuration,
        quality = quality,
        mediaUri = mediaUri
    )
}

/**
 * Domain representation of a favorite video item.
 */
data class FavoriteVideo(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val title: String,
    val durationMs: Long,
    val formattedDuration: String,
    val quality: String,
    val dateAdded: Long
) {
    fun toUiItem(): FavoriteVideoItem = FavoriteVideoItem(
        id = id,
        title = title,
        duration = formattedDuration,
        quality = quality,
        mediaUri = mediaUri
    )
}

/**
 * Domain representation of a media folder on the Home screen.
 */
data class HomeFolder(
    val folderPath: String,
    val name: String,
    val videoCount: Int,
    val videoCountText: String,
    val previewMediaUri: String?,
    val lastModified: Long
) {
    fun toUiItem(): FolderItem = FolderItem(
        id = folderPath,
        name = name,
        videoCountText = videoCountText,
        previewMediaUri = previewMediaUri
    )
}

/**
 * Helper to map a [Video] domain entity to [ContinueWatchingVideo].
 */
fun Video.toContinueWatchingVideo(): ContinueWatchingVideo {
    val cleanTitle = when {
        title.isNotBlank() -> title
        fileName.isNotBlank() -> fileName
        else -> "Untitled"
    }
    val qualityLabel = if (resolutionLabel.isNotBlank()) resolutionLabel else "HD"
    return ContinueWatchingVideo(
        id = id,
        mediaUri = mediaUri,
        filePath = filePath,
        title = cleanTitle,
        durationMs = durationMs,
        formattedDuration = formatDuration(durationMs),
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage.coerceIn(0.0f, 1.0f),
        quality = qualityLabel,
        lastPlayedAt = lastPlayedAt ?: 0L
    )
}

/**
 * Helper to map a [Video] domain entity to [RecentlyAddedVideo].
 */
fun Video.toRecentlyAddedVideo(currentTimeMillis: Long = System.currentTimeMillis()): RecentlyAddedVideo {
    val cleanTitle = when {
        title.isNotBlank() -> title
        fileName.isNotBlank() -> fileName
        else -> "Untitled"
    }
    val qualityLabel = if (resolutionLabel.isNotBlank()) resolutionLabel else "HD"
    val specsList = mutableListOf<String>()
    videoCodec?.takeIf { it.isNotBlank() }?.let { specsList.add(it) }
    val sizeText = formatFileSize(sizeBytes)
    if (sizeText != "0 B") specsList.add(sizeText)
    val technicalSpecs = if (specsList.isNotEmpty()) specsList.joinToString(" • ") else ""

    val addedTimeAndFolder = formatDateOnly(dateAdded)

    return RecentlyAddedVideo(
        id = id,
        mediaUri = mediaUri,
        filePath = filePath,
        title = cleanTitle,
        durationMs = durationMs,
        formattedDuration = formatDuration(durationMs),
        technicalSpecs = technicalSpecs,
        addedTimeAndFolder = addedTimeAndFolder,
        quality = qualityLabel,
        dateAdded = dateAdded
    )
}

/**
 * Safe date-only formatter (e.g., "Sep 19, 2026").
 */
fun formatDateOnly(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        sdf.format(Date(timestampMs))
    } catch (e: Exception) {
        ""
    }
}

/**
 * Helper to map a [Video] domain entity to [FavoriteVideo].
 */
fun Video.toFavoriteVideo(): FavoriteVideo {
    val cleanTitle = when {
        title.isNotBlank() -> title
        fileName.isNotBlank() -> fileName
        else -> "Untitled"
    }
    val qualityLabel = if (resolutionLabel.isNotBlank()) resolutionLabel else "HD"
    return FavoriteVideo(
        id = id,
        mediaUri = mediaUri,
        filePath = filePath,
        title = cleanTitle,
        durationMs = durationMs,
        formattedDuration = formatDuration(durationMs),
        quality = qualityLabel,
        dateAdded = dateAdded
    )
}

/**
 * Helper to map a [VideoFolder] domain entity to [HomeFolder].
 */
fun VideoFolder.toHomeFolder(): HomeFolder {
    val countText = if (videoCount == 1) "1 video" else "$videoCount videos"
    return HomeFolder(
        folderPath = folderPath,
        name = folderName,
        videoCount = videoCount,
        videoCountText = countText,
        previewMediaUri = previewMediaUri,
        lastModified = lastModified
    )
}

/**
 * Safe, human-friendly relative time formatter.
 */
fun formatRelativeTime(timestampMs: Long, currentMs: Long = System.currentTimeMillis()): String {
    if (timestampMs <= 0L) return "Recently added"
    val diff = (currentMs - timestampMs).coerceAtLeast(0L)
    val minutes = diff / (60 * 1000L)
    val hours = diff / (3600 * 1000L)
    val days = diff / (24 * 3600 * 1000L)

    return when {
        minutes < 1 -> "Added just now"
        minutes < 60 -> "Added $minutes ${if (minutes == 1L) "min" else "mins"} ago"
        hours < 24 -> "Added $hours ${if (hours == 1L) "hour" else "hours"} ago"
        days == 1L -> "Added yesterday"
        days < 7 -> "Added $days days ago"
        else -> {
            try {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                "Added ${sdf.format(Date(timestampMs))}"
            } catch (e: Exception) {
                "Recently added"
            }
        }
    }
}
