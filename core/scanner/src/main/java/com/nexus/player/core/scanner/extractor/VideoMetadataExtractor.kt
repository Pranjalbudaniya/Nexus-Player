package com.nexus.player.core.scanner.extractor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.scanner.datasource.DiscoveredMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VideoMetadataExtractor"

/**
 * Technical metadata extraction engine.
 * Safely inspects video streams using [MediaMetadataRetriever] and [MediaExtractor],
 * providing robust fallbacks if files are damaged or unreadable.
 */
@Singleton
class VideoMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Enriches a [DiscoveredMediaItem] with detailed audio/video codec and track counts.
     * Guaranteed to never throw; corrupted files return a valid [Video] with fallback metrics.
     */
    fun extractMetadata(
        item: DiscoveredMediaItem,
        existingVideo: Video? = null
    ): Video {
        // If file is unchanged, preserve existing technical metadata and user state
        if (existingVideo != null &&
            existingVideo.lastModified == item.lastModified &&
            existingVideo.sizeBytes == item.sizeBytes &&
            existingVideo.durationMs > 0
        ) {
            return existingVideo.copy(
                fileName = item.fileName,
                title = item.title,
                folderName = item.folderName,
                folderPath = item.folderPath,
                filePath = item.filePath ?: existingVideo.filePath
            )
        }

        var durationMs = item.durationMs
        var width = item.width
        var height = item.height
        var videoCodec: String? = null
        var videoBitrate: Long? = null
        var frameRate: Float? = null
        var audioCodec: String? = null
        var audioTrackCount = 0
        var subtitleTrackCount = 0

        // Step 1: Query MediaMetadataRetriever
        val uri = Uri.parse(item.mediaUri)
        val retriever = MediaMetadataRetriever()
        try {
            setRetrieverDataSource(retriever, uri, item.filePath)

            val retDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (retDuration != null && retDuration > 0) {
                durationMs = retDuration
            }

            val retWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val retHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            if (retWidth != null && retWidth > 0) width = retWidth
            if (retHeight != null && retHeight > 0) height = retHeight

            videoBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mime.isNullOrBlank()) {
                videoCodec = cleanCodecName(mime)
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever error on ${item.mediaUri}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
            }
        }

        // Step 2: Query MediaExtractor for precise track counts and codecs
        val extractor = MediaExtractor()
        try {
            setExtractorDataSource(extractor, uri, item.filePath)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue

                when {
                    trackMime.startsWith("video/") -> {
                        if (videoCodec == null) {
                            videoCodec = cleanCodecName(trackMime)
                        }
                        if (frameRate == null && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = try {
                                format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                            } catch (e: ClassCastException) {
                                format.getFloat(MediaFormat.KEY_FRAME_RATE)
                            }
                        }
                    }
                    trackMime.startsWith("audio/") -> {
                        audioTrackCount++
                        if (audioCodec == null) {
                            audioCodec = cleanCodecName(trackMime)
                        }
                    }
                    trackMime.startsWith("text/") ||
                            trackMime.contains("subrip") ||
                            trackMime.contains("vtt") ||
                            trackMime.contains("subtitles") -> {
                        subtitleTrackCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaExtractor error on ${item.mediaUri}: ${e.message}")
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {
            }
        }

        val resolution = computeResolutionLabel(width, height)

        return Video(
            id = item.id,
            mediaUri = item.mediaUri,
            filePath = item.filePath,
            fileName = item.fileName,
            title = item.title,
            folderName = item.folderName,
            folderPath = item.folderPath,
            sizeBytes = item.sizeBytes,
            durationMs = durationMs,
            width = width,
            height = height,
            resolutionLabel = resolution,
            videoCodec = videoCodec,
            videoBitrate = videoBitrate,
            frameRate = frameRate,
            audioCodec = audioCodec,
            audioTrackCount = if (audioTrackCount > 0) audioTrackCount else 1,
            subtitleTrackCount = subtitleTrackCount,
            dateAdded = item.dateAdded,
            lastModified = item.lastModified,
            lastPlayedAt = existingVideo?.lastPlayedAt,
            playbackPositionMs = existingVideo?.playbackPositionMs ?: 0L,
            playbackPercentage = existingVideo?.playbackPercentage ?: 0.0f,
            isFavorite = existingVideo?.isFavorite ?: false,
            watchCount = existingVideo?.watchCount ?: 0,
            extraMetadata = existingVideo?.extraMetadata
        )
    }

    private fun setRetrieverDataSource(retriever: MediaMetadataRetriever, uri: Uri, filePath: String?) {
        if (!filePath.isNullOrBlank() && File(filePath).canRead()) {
            retriever.setDataSource(filePath)
        } else {
            retriever.setDataSource(context, uri)
        }
    }

    private fun setExtractorDataSource(extractor: MediaExtractor, uri: Uri, filePath: String?) {
        if (!filePath.isNullOrBlank() && File(filePath).canRead()) {
            extractor.setDataSource(filePath)
        } else {
            extractor.setDataSource(context, uri, null)
        }
    }

    companion object {
        /**
         * Determines standard resolution tier label based on pixel dimensions.
         */
        fun computeResolutionLabel(width: Int, height: Int): String {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)
            return when {
                minDim >= 2160 || maxDim >= 3840 -> "4K"
                minDim >= 1440 || maxDim >= 2560 -> "2K"
                minDim >= 1080 || maxDim >= 1920 -> "1080p"
                minDim >= 720 || maxDim >= 1280 -> "720p"
                minDim >= 480 || maxDim >= 640 -> "480p"
                minDim > 0 && maxDim > 0 -> "SD"
                else -> "SD"
            }
        }

        private fun cleanCodecName(mime: String): String {
            return when (mime.lowercase()) {
                "video/avc" -> "H.264"
                "video/hevc" -> "HEVC"
                "video/x-vnd.on2.vp9", "video/vp9" -> "VP9"
                "video/x-vnd.on2.vp8", "video/vp8" -> "VP8"
                "video/av01" -> "AV1"
                "video/mp4v-es" -> "MPEG-4"
                "video/3gpp" -> "H.263"
                "audio/mp4a-latm" -> "AAC"
                "audio/ac3" -> "AC3"
                "audio/eac3" -> "E-AC3"
                "audio/opus" -> "Opus"
                "audio/vorbis" -> "Vorbis"
                "audio/flac" -> "FLAC"
                else -> mime.substringAfterLast('/').uppercase()
            }
        }
    }
}
