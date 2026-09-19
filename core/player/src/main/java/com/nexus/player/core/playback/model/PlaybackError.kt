package com.nexus.player.core.playback.model

/**
 * Structured playback error with user-readable messaging and debug details.
 *
 * Consumer UI shows [userMessage]; logging/analytics uses [technicalDetail] and [cause].
 */
data class PlaybackError(
    val category: ErrorCategory,
    val userMessage: String,
    val technicalDetail: String? = null,
    val cause: Throwable? = null
)

/**
 * Categorization of playback failures for structured error handling.
 *
 * Each category maps to a distinct user-facing error message and
 * potential recovery action in the future player UI.
 */
enum class ErrorCategory {
    /** Codec not supported by device hardware or software decoders. */
    UnsupportedCodec,

    /** Container format cannot be parsed by Media3. */
    UnsupportedContainer,

    /** File exists but media data is corrupt or unreadable. */
    CorruptMedia,

    /** File or content URI does not resolve to an existing resource. */
    MissingFile,

    /** Storage or content access denied (missing permission). */
    PermissionDenied,

    /** Network I/O failure (timeout, DNS, connection refused, etc.). */
    NetworkFailure,

    /** URL format is invalid or HTTP content type unsupported. */
    InvalidUrl,

    /** Hardware or software decoder failed to initialize for this format. */
    DecoderInitFailure,

    /** Catch-all for unmapped error codes. */
    Unknown
}
