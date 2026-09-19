package com.nexus.player.core.playback.model

/**
 * Nexus Player's own media item abstraction, decoupled from Media3's [androidx.media3.common.MediaItem].
 *
 * Consumers build this to request playback. The internal player implementation
 * converts it to the appropriate Media3 type.
 *
 * @param mediaId Stable identifier for this media (e.g., database video ID).
 * @param uri Playback URI — supports `content://`, `file://`, `http://`, `https://`, or raw file paths.
 * @param title Optional display title (used in notifications / metadata if needed later).
 * @param mimeType Optional MIME type hint to help the extractor; null lets Media3 auto-detect.
 */
data class NexusMediaItem(
    val mediaId: String,
    val uri: String,
    val title: String? = null,
    val mimeType: String? = null,
    val externalSubtitles: List<ExternalSubtitle> = emptyList()
)
