package com.nexus.player.core.playback.model

import java.util.UUID

/**
 * Domain representation of an external subtitle file imported by the user.
 *
 * @param id Unique identifier for this external subtitle.
 * @param uri URI pointing to the subtitle source (e.g., content:// or file://).
 * @param label Display label for the subtitle (e.g. filename "movie.eng.srt").
 * @param language Optional language code (e.g., "en", "spa").
 * @param mimeType Resolved subtitle MIME type (e.g., "application/x-subrip", "text/vtt", "text/x-ssa").
 */
data class ExternalSubtitle(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val label: String,
    val language: String? = null,
    val mimeType: String
)
