package com.nexus.player.core.scanner.model

/**
 * Execution parameters and constraints for a media scan.
 */
data class ScanOptions(
    /**
     * If true, performs an incremental refresh prioritizing changed/new items.
     * If false, performs a full re-scan of configured locations.
     */
    val isIncremental: Boolean = false,

    /**
     * If specified, restricts media scanning strictly to this folder path or SAF tree URI.
     * Other configured locations are untouched.
     */
    val targetFolderUriOrPath: String? = null,

    /**
     * If true, forces metadata re-extraction even if file size and modified timestamp match.
     */
    val forceMetadataRefresh: Boolean = false
)
