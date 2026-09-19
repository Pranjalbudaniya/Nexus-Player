package com.nexus.player.core.scanner.model

/**
 * Detailed progress information during an ongoing media scan.
 */
data class ScanProgress(
    val current: Int,
    val total: Int? = null,
    val currentFile: String? = null
)

/**
 * Summary outcome of a completed media scan.
 */
data class ScanResult(
    val totalScanned: Int,
    val inserted: Int,
    val updated: Int,
    val removed: Int,
    val durationMs: Long
)

/**
 * Observable lifecycle state of the media scanner.
 */
sealed interface ScanState {
    /** Scanner is ready and inactive. */
    data object Idle : ScanState

    /** Scanner is actively searching and extracting media. */
    data class Scanning(val progress: ScanProgress? = null) : ScanState

    /** Scan completed successfully. */
    data class Completed(val result: ScanResult) : ScanState

    /** Scan was cancelled before finishing. */
    data object Cancelled : ScanState

    /** Scan encountered a fatal error preventing completion. */
    data class Error(val message: String, val cause: Throwable? = null) : ScanState
}
