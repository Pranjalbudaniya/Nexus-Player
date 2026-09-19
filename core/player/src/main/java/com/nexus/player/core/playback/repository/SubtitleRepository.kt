package com.nexus.player.core.playback.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MimeTypes
import com.nexus.player.core.playback.model.ExternalSubtitle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for validating and importing external subtitle files.
 */
interface SubtitleRepository {
    /**
     * Checks whether the given URI or filename corresponds to a supported subtitle format.
     */
    fun isSupportedSubtitle(uri: Uri): Boolean

    /**
     * Creates an [ExternalSubtitle] from a user-provided [Uri].
     * Returns null if the format is unsupported or the URI cannot be parsed.
     */
    fun createExternalSubtitle(uri: Uri): ExternalSubtitle?
}

@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SubtitleRepository {

    override fun isSupportedSubtitle(uri: Uri): Boolean {
        val fileName = resolveDisplayName(uri) ?: uri.lastPathSegment ?: return false
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in SUPPORTED_EXTENSIONS
    }

    override fun createExternalSubtitle(uri: Uri): ExternalSubtitle? {
        val fileName = resolveDisplayName(uri) ?: uri.lastPathSegment ?: return null
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeType = when (ext) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            else -> return null
        }

        val language = parseLanguageFromFileName(fileName)

        return ExternalSubtitle(
            uri = uri.toString(),
            label = fileName,
            language = language,
            mimeType = mimeType
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to path segment
            }
        }
        return uri.lastPathSegment?.let { File(it).name }
    }

    private fun parseLanguageFromFileName(fileName: String): String? {
        // Look for common patterns: name.en.srt, name.eng.srt, name.spa.vtt
        val baseWithoutExt = fileName.substringBeforeLast('.')
        val possibleLang = baseWithoutExt.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (possibleLang.length in 2..3) {
            val isoLanguages = Locale.getISOLanguages()
            if (possibleLang in isoLanguages) {
                return possibleLang
            }
        }
        return null
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")
    }
}
