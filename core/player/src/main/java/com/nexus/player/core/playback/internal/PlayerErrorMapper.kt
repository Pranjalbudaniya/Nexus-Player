package com.nexus.player.core.playback.internal

import androidx.media3.common.PlaybackException
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.PlaybackError

/**
 * Maps Media3 [PlaybackException] instances to structured [PlaybackError] models.
 *
 * Produces both a user-friendly message (suitable for UI) and a technical
 * detail string (suitable for logging/debugging).
 */
internal object PlayerErrorMapper {

    fun mapException(exception: PlaybackException): PlaybackError {
        val category = mapErrorCode(exception.errorCode)
        return PlaybackError(
            category = category,
            userMessage = userMessageFor(category, exception.errorCode),
            technicalDetail = buildTechnicalDetail(exception),
            cause = exception
        )
    }

    /**
     * Maps a Media3 error code to the appropriate [ErrorCategory].
     */
    internal fun mapErrorCode(errorCode: Int): ErrorCategory {
        return when (errorCode) {
            // Decoder failures
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                ErrorCategory.DecoderInitFailure

            // Unsupported codec / format
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ->
                ErrorCategory.UnsupportedCodec

            // Container / parsing errors
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                ErrorCategory.UnsupportedContainer

            // File not found
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                ErrorCategory.MissingFile

            // Permission denied
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                ErrorCategory.PermissionDenied

            // Network failures
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                ErrorCategory.NetworkFailure

            // Invalid URL / HTTP content type
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                ErrorCategory.InvalidUrl

            // General I/O read failure — could be corrupt media
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                ErrorCategory.CorruptMedia

            // Decoding errors at runtime
            PlaybackException.ERROR_CODE_DECODING_FAILED ->
                ErrorCategory.CorruptMedia

            // Everything else
            else -> ErrorCategory.Unknown
        }
    }

    private fun userMessageFor(category: ErrorCategory, errorCode: Int = 0): String {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Connection timed out. Check your internet connection."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Server returned an error while loading video stream."
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                "The URL did not return a supported video stream."
            else -> when (category) {
                ErrorCategory.UnsupportedCodec ->
                    "This video format is not supported on your device"
                ErrorCategory.UnsupportedContainer ->
                    "This file type is not supported"
                ErrorCategory.CorruptMedia ->
                    "This video file appears to be damaged"
                ErrorCategory.MissingFile ->
                    "Video file not found"
                ErrorCategory.PermissionDenied ->
                    "Permission required to access this file"
                ErrorCategory.NetworkFailure ->
                    "Network connection failed"
                ErrorCategory.InvalidUrl ->
                    "Invalid video URL"
                ErrorCategory.DecoderInitFailure ->
                    "Video decoder failed to initialize"
                ErrorCategory.Unknown ->
                    "Unable to play this video"
            }
        }
    }

    private fun buildTechnicalDetail(exception: PlaybackException): String {
        return buildString {
            append("Error code: ${exception.errorCode}")
            append(" (${exception.errorCodeName})")
            exception.message?.let { append(" — $it") }
            exception.cause?.let { cause ->
                append("\nCaused by: ${cause::class.java.simpleName}")
                cause.message?.let { append(": $it") }
            }
        }
    }
}
