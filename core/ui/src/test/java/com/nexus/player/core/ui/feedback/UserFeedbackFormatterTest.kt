package com.nexus.player.core.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.FileAlreadyExistsException
import java.util.concurrent.CancellationException

class UserFeedbackFormatterTest {

    @Test
    fun formatFileError_cancellation_returnsEmpty() {
        val result = UserFeedbackFormatter.formatFileError("Delete", CancellationException())
        assertEquals("", result)
    }

    @Test
    fun formatFileError_null_returnsEmpty() {
        val result = UserFeedbackFormatter.formatFileError("Delete", null)
        assertEquals("", result)
    }

    @Test
    fun formatFileError_securityException_returnsPermissionMessage() {
        val message = UserFeedbackFormatter.formatFileError("Rename", SecurityException("Permission denied for /sdcard"))
        assertTrue(message.contains("permission", ignoreCase = true))
        assertFalse(message.contains("/sdcard"))
    }

    @Test
    fun formatFileError_fileNotFound_returnsFriendlyMessage() {
        val message = UserFeedbackFormatter.formatFileError("Move", FileNotFoundException("No such file: /storage/video.mp4"))
        assertTrue(message.contains("could not be found", ignoreCase = true))
        assertFalse(message.contains("/storage/video.mp4"))
    }

    @Test
    fun formatFileError_fileAlreadyExists_returnsCollisionMessage() {
        val message = UserFeedbackFormatter.formatFileError("Copy", FileAlreadyExistsException("/dest/video.mp4"))
        assertTrue(message.contains("already exists", ignoreCase = true))
        assertFalse(message.contains("/dest/video.mp4"))
    }

    @Test
    fun formatFileError_invalidCharacters_returnsAllowedCharacters() {
        val message = UserFeedbackFormatter.formatFileError("Rename", IllegalArgumentException("Invalid characters in name: /:*?"))
        assertTrue(message.contains("cannot contain", ignoreCase = true))
    }

    @Test
    fun formatFileError_emptyName_returnsEmptyNameWarning() {
        val message = UserFeedbackFormatter.formatFileError("Rename", IllegalArgumentException("File name is empty"))
        assertTrue(message.contains("empty", ignoreCase = true))
    }

    @Test
    fun formatFileError_noSpace_returnsStorageFullMessage() {
        val message = UserFeedbackFormatter.formatFileError("Copy", IOException("write failed: ENOSPC (No space left on device)"))
        assertTrue(message.contains("space", ignoreCase = true))
        assertFalse(message.contains("ENOSPC"))
    }

    @Test
    fun formatFileError_readOnly_returnsReadOnlyMessage() {
        val message = UserFeedbackFormatter.formatFileError("Delete", IOException("Read-only file system (EROFS)"))
        assertTrue(message.contains("read-only", ignoreCase = true))
    }

    @Test
    fun formatFileError_fileBusy_returnsBusyMessage() {
        val message = UserFeedbackFormatter.formatFileError("Delete", IOException("File resource busy (EBUSY)"))
        assertTrue(message.contains("in use by another application", ignoreCase = true))
    }

    @Test
    fun formatScanError_permission_returnsStorageAccessMessage() {
        val message = UserFeedbackFormatter.formatScanError(SecurityException("Media scanner permission denied"))
        assertTrue(message.contains("Storage access is required", ignoreCase = true))
    }

    @Test
    fun formatScanError_unmountedStorage_returnsUnmountedMessage() {
        val message = UserFeedbackFormatter.formatScanError(IOException("Volume unmounted"))
        assertTrue(message.contains("unmounted", ignoreCase = true))
    }

    @Test
    fun formatScanError_databaseIssue_returnsDatabaseMessage() {
        val message = UserFeedbackFormatter.formatScanError(null, "Failed to save to database")
        assertTrue(message.contains("database", ignoreCase = true))
    }

    @Test
    fun formatDatabaseError_constraintViolation_returnsItemExists() {
        val message = UserFeedbackFormatter.formatDatabaseError(IllegalStateException("UNIQUE constraint failed: videos.id"))
        assertTrue(message.contains("already exists", ignoreCase = true))
        assertFalse(message.contains("UNIQUE constraint"))
    }

    @Test
    fun formatDatabaseError_diskIoFailure_returnsCheckStorage() {
        val message = UserFeedbackFormatter.formatDatabaseError(IOException("SQLiteDiskIOException: disk I/O error"))
        assertTrue(message.contains("storage", ignoreCase = true))
    }

    @Test
    fun formatNetworkError_unknownHost_returnsDnsMessage() {
        val message = UserFeedbackFormatter.formatNetworkError(UnknownHostException("stream.nexus.internal"))
        assertTrue(message.contains("resolve stream server address", ignoreCase = true))
        assertFalse(message.contains("stream.nexus.internal"))
    }

    @Test
    fun formatNetworkError_timeout_returnsTimeoutMessage() {
        val message = UserFeedbackFormatter.formatNetworkError(SocketTimeoutException("Read timed out"))
        assertTrue(message.contains("timed out", ignoreCase = true))
    }

    @Test
    fun formatNetworkError_connectRefused_returnsVerificationMessage() {
        val message = UserFeedbackFormatter.formatNetworkError(ConnectException("Connection refused"))
        assertTrue(message.contains("Failed to connect", ignoreCase = true))
    }

    @Test
    fun formatNetworkError_http404_returnsNotFoundMessage() {
        val message = UserFeedbackFormatter.formatNetworkError(IOException("Invalid response status: 404 Not Found"))
        assertTrue(message.contains("404", ignoreCase = true))
        assertTrue(message.contains("not found", ignoreCase = true))
    }

    @Test
    fun formatNetworkError_http403_returnsForbiddenMessage() {
        val message = UserFeedbackFormatter.formatNetworkError(IOException("HTTP 403 Forbidden"))
        assertTrue(message.contains("forbidden", ignoreCase = true))
    }

    @Test
    fun formatPlaybackError_unsupportedCodec_returnsHardwareMessage() {
        val message = UserFeedbackFormatter.formatPlaybackError(IllegalStateException("MediaCodecVideoRenderer: decoder init failed"))
        assertTrue(message.contains("Hardware decoder", ignoreCase = true))
    }

    @Test
    fun formatPlaybackError_corruptContainer_returnsContainerMessage() {
        val message = UserFeedbackFormatter.formatPlaybackError(IllegalStateException("MatroskaExtractor: parser malformed header"))
        assertTrue(message.contains("corrupted or uses an unsupported container", ignoreCase = true))
    }

    @Test
    fun formatSubtitleError_malformed_returnsCleanMessage() {
        val message = UserFeedbackFormatter.formatSubtitleError(IllegalArgumentException("Malformed SRT subtitle file"))
        assertTrue(message.contains("malformed", ignoreCase = true))
    }

    @Test
    fun formatPlaylistError_emptyName_returnsEmptyWarning() {
        val message = UserFeedbackFormatter.formatPlaylistError("Create Playlist", IllegalArgumentException("Playlist name cannot be empty"))
        assertTrue(message.contains("empty", ignoreCase = true))
    }

    @Test
    fun formatGenericError_unexpectedException_neverLeaksStackTrace() {
        val rawException = RuntimeException("Fatal crash at com.nexus.internal.InternalWorker.execute(InternalWorker.kt:142)")
        val message = UserFeedbackFormatter.formatGenericError(rawException)
        assertEquals("An unexpected error occurred. Please try again.", message)
        assertFalse(message.contains("InternalWorker"))
    }
}
