package com.nexus.player.core.playback.internal

import androidx.media3.common.PlaybackException
import com.nexus.player.core.playback.model.ErrorCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests [PlayerErrorMapper] — verifying that Media3 error codes
 * produce the correct [ErrorCategory] and user-friendly messages.
 *
 * Uses Robolectric because [PlaybackException] constructor calls
 * [android.os.SystemClock.elapsedRealtime] internally.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerErrorMapperTest {

    // =========================================================================
    // Error code → ErrorCategory mapping
    // =========================================================================

    @Test
    fun decoderInitFailed_mapsToDecoderInitFailure() {
        assertEquals(
            ErrorCategory.DecoderInitFailure,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)
        )
    }

    @Test
    fun decoderQueryFailed_mapsToDecoderInitFailure() {
        assertEquals(
            ErrorCategory.DecoderInitFailure,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED)
        )
    }

    @Test
    fun decodingFormatUnsupported_mapsToUnsupportedCodec() {
        assertEquals(
            ErrorCategory.UnsupportedCodec,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED)
        )
    }

    @Test
    fun decodingFormatExceedsCapabilities_mapsToUnsupportedCodec() {
        assertEquals(
            ErrorCategory.UnsupportedCodec,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES)
        )
    }

    @Test
    fun parsingContainerMalformed_mapsToUnsupportedContainer() {
        assertEquals(
            ErrorCategory.UnsupportedContainer,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        )
    }

    @Test
    fun parsingContainerUnsupported_mapsToUnsupportedContainer() {
        assertEquals(
            ErrorCategory.UnsupportedContainer,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
        )
    }

    @Test
    fun parsingManifestMalformed_mapsToUnsupportedContainer() {
        assertEquals(
            ErrorCategory.UnsupportedContainer,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED)
        )
    }

    @Test
    fun parsingManifestUnsupported_mapsToUnsupportedContainer() {
        assertEquals(
            ErrorCategory.UnsupportedContainer,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED)
        )
    }

    @Test
    fun ioFileNotFound_mapsToMissingFile() {
        assertEquals(
            ErrorCategory.MissingFile,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        )
    }

    @Test
    fun ioNoPermission_mapsToPermissionDenied() {
        assertEquals(
            ErrorCategory.PermissionDenied,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_NO_PERMISSION)
        )
    }

    @Test
    fun ioNetworkConnectionFailed_mapsToNetworkFailure() {
        assertEquals(
            ErrorCategory.NetworkFailure,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        )
    }

    @Test
    fun ioNetworkConnectionTimeout_mapsToNetworkFailure() {
        assertEquals(
            ErrorCategory.NetworkFailure,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        )
    }

    @Test
    fun ioInvalidHttpContentType_mapsToInvalidUrl() {
        assertEquals(
            ErrorCategory.InvalidUrl,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE)
        )
    }

    @Test
    fun ioBadHttpStatus_mapsToInvalidUrl() {
        assertEquals(
            ErrorCategory.InvalidUrl,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        )
    }

    @Test
    fun decodingFailed_mapsToCorruptMedia() {
        assertEquals(
            ErrorCategory.CorruptMedia,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_DECODING_FAILED)
        )
    }

    @Test
    fun ioReadPositionOutOfRange_mapsToCorruptMedia() {
        assertEquals(
            ErrorCategory.CorruptMedia,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        )
    }

    @Test
    fun ioUnspecified_mapsToCorruptMedia() {
        assertEquals(
            ErrorCategory.CorruptMedia,
            PlayerErrorMapper.mapErrorCode(PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        )
    }

    @Test
    fun unknownErrorCode_mapsToUnknown() {
        assertEquals(
            ErrorCategory.Unknown,
            PlayerErrorMapper.mapErrorCode(9999)
        )
    }

    // =========================================================================
    // Full mapException() tests
    // =========================================================================

    @Test
    fun mapException_producesUserFriendlyMessage() {
        val exception = PlaybackException(
            "Decoder init failed",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertEquals(ErrorCategory.DecoderInitFailure, error.category)
        assertEquals("Video decoder failed to initialize", error.userMessage)
        assertNotNull(error.technicalDetail)
        assertEquals(exception, error.cause)
    }

    @Test
    fun mapException_missingFile_userMessage() {
        val exception = PlaybackException(
            "File not found: /storage/video.mp4",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertEquals("Video file not found", error.userMessage)
        assertTrue(error.technicalDetail!!.contains("ERROR_CODE_IO_FILE_NOT_FOUND"))
    }

    @Test
    fun mapException_networkFailure_userMessage() {
        val exception = PlaybackException(
            "Connection refused",
            java.io.IOException("Connection refused"),
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertEquals("Network connection failed", error.userMessage)
        assertTrue(error.technicalDetail!!.contains("IOException"))
    }

    @Test
    fun mapException_unsupportedCodec_userMessage() {
        val exception = PlaybackException(
            "Format not supported",
            null,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertEquals("This video format is not supported on your device", error.userMessage)
    }

    @Test
    fun mapException_permissionDenied_userMessage() {
        val exception = PlaybackException(
            "Permission denied",
            SecurityException("No READ_EXTERNAL_STORAGE"),
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertEquals("Permission required to access this file", error.userMessage)
        assertTrue(error.technicalDetail!!.contains("SecurityException"))
    }

    @Test
    fun mapException_technicalDetail_containsErrorCodeName() {
        val exception = PlaybackException(
            "test",
            null,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )
        val error = PlayerErrorMapper.mapException(exception)

        assertTrue(error.technicalDetail!!.contains("Error code:"))
    }

    @Test
    fun mapException_allCategories_haveNonEmptyUserMessages() {
        for (category in ErrorCategory.entries) {
            // Just verify all categories can be handled without exception
            val errorCode = when (category) {
                ErrorCategory.UnsupportedCodec -> PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                ErrorCategory.UnsupportedContainer -> PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                ErrorCategory.CorruptMedia -> PlaybackException.ERROR_CODE_DECODING_FAILED
                ErrorCategory.MissingFile -> PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                ErrorCategory.PermissionDenied -> PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                ErrorCategory.NetworkFailure -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                ErrorCategory.InvalidUrl -> PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE
                ErrorCategory.DecoderInitFailure -> PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                ErrorCategory.Unknown -> 9999
            }

            val exception = PlaybackException("test", null, errorCode)
            val error = PlayerErrorMapper.mapException(exception)

            assertTrue(
                "Category $category should have non-empty user message",
                error.userMessage.isNotBlank()
            )
        }
    }
}
