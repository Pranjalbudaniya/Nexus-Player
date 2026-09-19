package com.nexus.player.core.playback.model

import java.util.Locale

/**
 * Domain representation of an audio or subtitle track exposed by Media3 or external files.
 *
 * @param id Stable unique identifier for the track within the player session.
 * @param label Explicit track label from container metadata (e.g., "Director's Commentary").
 * @param language ISO-639 language code or localized language name (e.g., "en", "jpn", "eng").
 * @param isSelected Whether this track is currently active.
 * @param mimeType Sample MIME type (e.g. "audio/mp4a-latm", "application/x-subrip").
 * @param channelCount Number of audio channels (1 = Mono, 2 = Stereo, 6 = 5.1, 8 = 7.1).
 * @param codec Codec identifier if available (e.g., "AAC", "AC3", "E-AC3", "DTS", "FLAC", "Opus").
 * @param isExternal Whether this track is an imported external subtitle file.
 */
data class PlayerTrack(
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val mimeType: String? = null,
    val channelCount: Int = 0,
    val codec: String? = null,
    val isExternal: Boolean = false
) {
    /**
     * Resolves human-readable language name using device locale.
     * E.g., "eng" -> "English", "spa" -> "Spanish", "und" -> null.
     */
    val resolvedLanguageName: String?
        get() {
            if (language.isNullOrBlank() || language.equals("und", ignoreCase = true)) return null
            return try {
                val locale = Locale.forLanguageTag(language)
                val display = locale.displayLanguage
                if (display.isNotBlank()) display else language
            } catch (_: Exception) {
                language
            }
        }

    /**
     * Human-readable channel configuration (e.g., "5.1", "Stereo", "Mono", "7.1").
     */
    val channelConfiguration: String?
        get() = when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            7 -> "6.1"
            8 -> "7.1"
            else -> if (channelCount > 2) "$channelCount ch" else null
        }

    /**
     * Formatted display title for UI without inventing fictional names.
     */
    val displayTitle: String
        get() {
            val lang = resolvedLanguageName
            val hasExplicitLabel = label.isNotBlank() &&
                !label.startsWith("Audio Track", ignoreCase = true) &&
                !label.startsWith("Subtitle Track", ignoreCase = true)

            return when {
                lang != null && hasExplicitLabel -> "$lang ($label)"
                lang != null -> lang
                hasExplicitLabel -> label
                label.isNotBlank() -> label
                else -> "Track"
            }
        }

    /**
     * Secondary technical description line (e.g., "5.1 • AAC" or "Stereo • Opus").
     */
    val technicalDetails: String?
        get() {
            val parts = mutableListOf<String>()
            channelConfiguration?.let { parts.add(it) }
            codec?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            if (isExternal) parts.add("External")
            return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
        }
}
