package com.nexus.player.core.scanner.util

import java.util.Locale

/**
 * Utility for detecting video media by file extension and MIME type.
 * Supports popular streaming and container formats common in local and network playback.
 */
object VideoFormatDetector {

    val SUPPORTED_VIDEO_EXTENSIONS: Set<String> = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "3gp",
        "ts", "m2ts", "mts", "m4v", "wmv", "flv",
        "vob", "ogv", "f4v", "divx", "rmvb", "asf",
        "mpg", "mpeg"
    )

    private val VIDEO_APPLICATION_MIMES: Set<String> = setOf(
        "application/x-matroska",
        "application/vnd.apple.mpegurl",
        "application/mp4",
        "application/x-mpegurl",
        "application/dash+xml"
    )

    /**
     * Checks if a file represents a video based on its name and optional MIME type.
     */
    fun isVideoFile(fileName: String, mimeType: String? = null): Boolean {
        if (mimeType != null && isVideoMimeType(mimeType)) {
            return true
        }
        val ext = extractExtension(fileName)
        return isVideoExtension(ext)
    }

    /**
     * Checks if a file extension (without leading dot) matches a supported video format.
     */
    fun isVideoExtension(extension: String): Boolean {
        if (extension.isBlank()) return false
        val clean = extension.lowercase(Locale.ROOT).removePrefix(".")
        return SUPPORTED_VIDEO_EXTENSIONS.contains(clean)
    }

    /**
     * Checks if a MIME type represents video content.
     */
    fun isVideoMimeType(mimeType: String): Boolean {
        if (mimeType.isBlank()) return false
        val clean = mimeType.lowercase(Locale.ROOT).trim()
        return clean.startsWith("video/") || VIDEO_APPLICATION_MIMES.contains(clean)
    }

    /**
     * Safely extracts the lowercase file extension from a filename or path.
     */
    fun extractExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot < 0 || lastDot == fileName.length - 1) return ""
        return fileName.substring(lastDot + 1).lowercase(Locale.ROOT)
    }

    /**
     * Generates a clean display title from a raw filename by stripping the file extension.
     */
    fun extractDisplayTitle(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0) {
            return fileName.substring(0, lastDot)
        }
        return fileName
    }
}
