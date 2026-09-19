package com.nexus.player.core.scanner.datasource

/**
 * Representation of a discovered media file candidate prior to full metadata enrichment.
 */
data class DiscoveredMediaItem(
    val id: String,
    val mediaUri: String,
    val filePath: String?,
    val fileName: String,
    val title: String,
    val folderName: String,
    val folderPath: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val lastModified: Long,
    val mimeType: String? = null
)
