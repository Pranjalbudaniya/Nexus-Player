package com.nexus.player.core.ui.feedback

import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.FileAlreadyExistsException
import java.util.concurrent.CancellationException

/**
 * Centralized, user-friendly error and feedback translation utility.
 *
 * Converts low-level platform, filesystem, network, and database exceptions
 * into short, understandable user-facing messages.
 *
 * Never exposes raw stack traces, filesystem inode details, or internal classes.
 */
object UserFeedbackFormatter {

    /**
     * Translates file operation exceptions (rename, move, copy, delete, restore)
     * into actionable user messages.
     */
    fun formatFileError(operation: String, throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val rawMessage = throwable.message.orEmpty().lowercase()
        val cause = throwable.cause
        val causeMessage = cause?.message.orEmpty().lowercase()
        val combined = "$rawMessage $causeMessage"

        return when {
            throwable is SecurityException || combined.contains("permission denied") || combined.contains("access denied") -> {
                "Storage permission denied. Please grant storage access in Settings."
            }

            throwable is FileNotFoundException || combined.contains("not found") || combined.contains("no such file") -> {
                "File could not be found. It may have been moved, renamed, or deleted."
            }

            throwable is FileAlreadyExistsException || combined.contains("already exists") || combined.contains("eexist") -> {
                "A file with this name already exists in the destination folder."
            }

            throwable is IllegalArgumentException && combined.contains("character") -> {
                "Invalid file name: names cannot contain \\ / : * ? \" < > |"
            }

            throwable is IllegalArgumentException && (combined.contains("empty") || combined.contains("blank")) -> {
                "File name cannot be empty."
            }

            combined.contains("enospc") || combined.contains("no space") || combined.contains("storage full") -> {
                "Not enough storage space available on your device."
            }

            combined.contains("erofs") || combined.contains("read-only") -> {
                "This storage location is read-only and cannot be modified."
            }

            combined.contains("ebusy") || combined.contains("resource busy") || combined.contains("locked") -> {
                "File is currently in use by another application. Please try again later."
            }

            throwable is IOException -> {
                "$operation failed due to a storage I/O issue. Please check your storage connection."
            }

            else -> {
                "$operation failed. Please try again."
            }
        }
    }

    /**
     * Formats media scanning errors into user-friendly descriptions.
     */
    fun formatScanError(throwable: Throwable?, defaultMessage: String? = null): String {
        if (throwable == null && !defaultMessage.isNullOrBlank()) {
            return formatScanErrorMessage(defaultMessage)
        }
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            throwable is SecurityException || combined.contains("permission") || combined.contains("access denied") -> {
                "Storage access is required to scan your video library."
            }

            combined.contains("enospc") || combined.contains("no space") -> {
                "Storage is full. Free up space to continue scanning."
            }

            combined.contains("unmounted") || combined.contains("not ready") -> {
                "Storage volume is unmounted or unavailable."
            }

            combined.contains("sqlite") || combined.contains("database") -> {
                "Failed to save discovered videos to the database. Please try again."
            }

            else -> {
                "Media scan encountered an issue. Tap rescan to try again."
            }
        }
    }

    private fun formatScanErrorMessage(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("permission") || lower.contains("access denied") ->
                "Storage access is required to scan your video library."
            lower.contains("sqlite") || lower.contains("database") ->
                "Failed to save discovered videos to the database."
            lower.contains("unmounted") ->
                "Storage volume is unavailable."
            else ->
                "Media scan failed. Tap rescan to try again."
        }
    }

    /**
     * Formats database-related failures (Room / SQLite) into safe user messages.
     */
    fun formatDatabaseError(throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            combined.contains("constraint") || combined.contains("unique") -> {
                "An item with this name or identifier already exists."
            }

            combined.contains("disk i/o") || combined.contains("enospc") -> {
                "Database storage error. Please check available device storage."
            }

            combined.contains("locked") || combined.contains("busy") -> {
                "Database is temporarily busy. Please retry in a moment."
            }

            else -> {
                "Database operation could not be completed. Please try again."
            }
        }
    }

    /**
     * Formats network playback and connection errors into understandable strings.
     */
    fun formatNetworkError(throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            throwable is UnknownHostException || combined.contains("unknownhost") || combined.contains("dns") -> {
                "Unable to resolve stream server address. Check your internet connection."
            }

            throwable is SocketTimeoutException || combined.contains("timeout") || combined.contains("timed out") -> {
                "Connection to stream timed out. The server may be busy or unreachable."
            }

            throwable is ConnectException || combined.contains("connection refused") || combined.contains("failed to connect") -> {
                "Failed to connect to stream server. Please verify the URL and try again."
            }

            combined.contains("404") || combined.contains("not found") -> {
                "Stream not found (HTTP 404). The link may have expired or been removed."
            }

            combined.contains("403") || combined.contains("forbidden") -> {
                "Access to this stream is forbidden (HTTP 403). Authentication may be required."
            }

            combined.contains("500") || combined.contains("502") || combined.contains("503") || combined.contains("server error") -> {
                "The stream server encountered an error. Please try again later."
            }

            combined.contains("cleartext") -> {
                "Insecure HTTP connection blocked. HTTPS is recommended."
            }

            else -> {
                "Unable to play network stream. Please check the URL and your connection."
            }
        }
    }

    /**
     * Formats playback, codec, container, or corruption errors.
     */
    fun formatPlaybackError(throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            combined.contains("decoder") && (combined.contains("init") || combined.contains("query")) -> {
                "Hardware decoder could not be initialized for this video format."
            }

            combined.contains("unsupported") && combined.contains("format") -> {
                "This video format is not supported on this device."
            }

            combined.contains("container") || combined.contains("parser") || combined.contains("malformed") -> {
                "This media file is corrupted or uses an unsupported container."
            }

            combined.contains("audio") && combined.contains("track") -> {
                "Audio track format is unsupported."
            }

            throwable is FileNotFoundException || combined.contains("not found") -> {
                "Media file not found. It may have been moved or deleted."
            }

            throwable is SecurityException || combined.contains("permission") -> {
                "Permission required to access this media file."
            }

            else -> {
                "Playback error encountered. Tap retry to reload."
            }
        }
    }

    /**
     * Formats subtitle parsing or loading errors.
     */
    fun formatSubtitleError(throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            combined.contains("not found") || throwable is FileNotFoundException -> {
                "Subtitle file could not be found."
            }

            combined.contains("encoding") || combined.contains("charset") -> {
                "Subtitle character encoding is unsupported. UTF-8 is recommended."
            }

            combined.contains("parse") || combined.contains("malformed") -> {
                "Subtitle file is malformed or corrupted."
            }

            else -> {
                "Failed to load subtitles. File format may be unsupported."
            }
        }
    }

    /**
     * Formats playlist operation failures (create, rename, add, remove).
     */
    fun formatPlaylistError(operation: String, throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""

        val combined = "${throwable.message.orEmpty()} ${throwable.cause?.message.orEmpty()}".lowercase()

        return when {
            combined.contains("empty") || combined.contains("blank") -> {
                "Playlist name cannot be empty."
            }

            combined.contains("already exists") || combined.contains("constraint") || combined.contains("unique") -> {
                "A playlist with this name already exists."
            }

            combined.contains("not found") -> {
                "Playlist no longer exists."
            }

            else -> {
                "$operation failed. Please try again."
            }
        }
    }

    /**
     * Formats settings validation or persistence errors.
     */
    fun formatSettingsError(throwable: Throwable?): String {
        if (throwable == null || throwable is CancellationException) return ""
        return "Failed to save settings preference. Please try again."
    }

    /**
     * Fallback formatter for any unexpected generic exception.
     */
    fun formatGenericError(throwable: Throwable?, fallback: String = "An unexpected error occurred. Please try again."): String {
        if (throwable == null || throwable is CancellationException) return ""
        return fallback
    }
}
