package com.nexus.player.core.media.model

import com.nexus.player.core.database.model.Video
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Normalized domain model containing formatted, presentation-ready video metadata.
 * Designed for efficient binding in list and grid cards without repeated formatting on the UI thread.
 */
data class MediaMetadata(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val fileName: String,
    val title: String,
    val folderName: String,
    val folderPath: String,
    val formattedDuration: String,
    val durationMs: Long,
    val resolutionLabel: String,
    val dimensionsLabel: String,
    val width: Int,
    val height: Int,
    val videoCodec: String,
    val audioCodec: String,
    val formattedFps: String,
    val frameRate: Float?,
    val formattedBitrate: String,
    val videoBitrate: Long?,
    val formattedSize: String,
    val sizeBytes: Long,
    val formattedModifiedDate: String,
    val lastModified: Long,
    val audioTrackCount: Int,
    val subtitleTrackCount: Int,
    val isFavorite: Boolean,
    val playbackPositionMs: Long,
    val playbackPercentage: Float,
    val watchCount: Int,
    val lastPlayedAt: Long?
)

/**
 * Normalization extension that safely maps a [Video] database entity to [MediaMetadata].
 * Defensive against null, zero, negative, or malformed values.
 */
fun Video.toMediaMetadata(): MediaMetadata {
    val cleanTitle = when {
        title.isNotBlank() -> title
        fileName.isNotBlank() -> fileName
        else -> "Untitled"
    }
    val cleanFolder = if (folderName.isNotBlank()) folderName else "Internal Storage"
    val safeWidth = if (width > 0) width else 0
    val safeHeight = if (height > 0) height else 0
    val dims = if (safeWidth > 0 && safeHeight > 0) "${safeWidth}x${safeHeight}" else "Unknown"

    val res = when {
        resolutionLabel.isNotBlank() -> resolutionLabel
        safeWidth > 0 || safeHeight > 0 -> computeResolution(safeWidth, safeHeight)
        else -> "SD"
    }
    val vCodec = videoCodec?.takeIf { it.isNotBlank() } ?: "Unknown"
    val aCodec = audioCodec?.takeIf { it.isNotBlank() } ?: "Unknown"

    return MediaMetadata(
        id = id,
        mediaUri = mediaUri,
        filePath = filePath,
        fileName = fileName,
        title = cleanTitle,
        folderName = cleanFolder,
        folderPath = folderPath,
        formattedDuration = formatDuration(durationMs),
        durationMs = if (durationMs > 0) durationMs else 0L,
        resolutionLabel = res,
        dimensionsLabel = dims,
        width = safeWidth,
        height = safeHeight,
        videoCodec = vCodec,
        audioCodec = aCodec,
        formattedFps = formatFps(frameRate),
        frameRate = frameRate,
        formattedBitrate = formatBitrate(videoBitrate),
        videoBitrate = videoBitrate,
        formattedSize = formatFileSize(sizeBytes),
        sizeBytes = if (sizeBytes > 0) sizeBytes else 0L,
        formattedModifiedDate = formatModifiedDate(lastModified),
        lastModified = lastModified,
        audioTrackCount = if (audioTrackCount > 0) audioTrackCount else 1,
        subtitleTrackCount = if (subtitleTrackCount >= 0) subtitleTrackCount else 0,
        isFavorite = isFavorite,
        playbackPositionMs = if (playbackPositionMs > 0) playbackPositionMs else 0L,
        playbackPercentage = playbackPercentage.coerceIn(0.0f, 1.0f),
        watchCount = if (watchCount > 0) watchCount else 0,
        lastPlayedAt = lastPlayedAt
    )
}

/**
 * Formats duration in milliseconds into "HH:MM:SS" or "MM:SS".
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats byte size into human readable "1.2 GB", "450 MB", etc.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024.0 && unitIndex < units.size - 1) {
        size /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${size.toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }
}

/**
 * Formats frame rate into "60 fps", "23.9 fps", etc.
 */
fun formatFps(fps: Float?): String {
    if (fps == null || fps <= 0f) return "-- fps"
    return if (fps % 1.0f == 0.0f) {
        String.format(Locale.US, "%d fps", fps.toInt())
    } else {
        String.format(Locale.US, "%.1f fps", fps)
    }
}

/**
 * Formats video bitrate into "4.5 Mbps", "850 Kbps", etc.
 */
fun formatBitrate(bitrateBps: Long?): String {
    if (bitrateBps == null || bitrateBps <= 0) return "-- Mbps"
    return if (bitrateBps >= 1_000_000) {
        String.format(Locale.US, "%.1f Mbps", bitrateBps / 1_000_000.0)
    } else {
        String.format(Locale.US, "%d Kbps", bitrateBps / 1000)
    }
}

/**
 * Formats an epoch timestamp safely into "MMM d, yyyy".
 */
fun formatModifiedDate(epochMillis: Long): String {
    if (epochMillis <= 0) return "Unknown Date"
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        sdf.format(Date(epochMillis))
    } catch (e: Exception) {
        "Unknown Date"
    }
}

private fun computeResolution(width: Int, height: Int): String {
    val maxDim = maxOf(width, height)
    val minDim = minOf(width, height)
    return when {
        minDim >= 2160 || maxDim >= 3840 -> "4K"
        minDim >= 1440 || maxDim >= 2560 -> "2K"
        minDim >= 1080 || maxDim >= 1920 -> "1080p"
        minDim >= 720 || maxDim >= 1280 -> "720p"
        minDim >= 480 || maxDim >= 640 -> "480p"
        else -> "SD"
    }
}
