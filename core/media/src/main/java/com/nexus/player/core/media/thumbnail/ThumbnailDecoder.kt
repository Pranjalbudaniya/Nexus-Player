package com.nexus.player.core.media.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ThumbnailDecoder"

/**
 * Abstraction for platform-level video frame decoding.
 * Allows unit test isolation without coupling to Android framework hardware decoders.
 */
interface ThumbnailDecoder {

    /**
     * Decodes a representative video frame scaled to [targetWidth] and [targetHeight].
     */
    suspend fun decodeFrame(
        mediaUri: String,
        targetWidth: Int,
        targetHeight: Int,
        cancellationSignal: CancellationSignal? = null
    ): Bitmap?
}

/**
 * Default Android implementation using [android.content.ContentResolver.loadThumbnail] on Android 10+
 * with fallback to [MediaMetadataRetriever.getScaledFrameAtTime].
 */
@Singleton
class DefaultThumbnailDecoder @Inject constructor(
    @ApplicationContext private val context: Context
) : ThumbnailDecoder {

    override suspend fun decodeFrame(
        mediaUri: String,
        targetWidth: Int,
        targetHeight: Int,
        cancellationSignal: CancellationSignal?
    ): Bitmap? {
        val uri = Uri.parse(mediaUri)

        // Android 10+ ContentResolver thumbnail API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaUri.startsWith("content://")) {
            try {
                return context.contentResolver.loadThumbnail(
                    uri,
                    Size(targetWidth, targetHeight),
                    cancellationSignal
                )
            } catch (e: Exception) {
                Log.d(TAG, "ContentResolver.loadThumbnail failed for $mediaUri: ${e.message}; attempting retriever")
            }
        }

        // Universal fallback via MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            if (mediaUri.startsWith("content://")) {
                retriever.setDataSource(context, uri)
            } else {
                val file = File(mediaUri)
                if (file.canRead()) {
                    retriever.setDataSource(file.absolutePath)
                } else {
                    retriever.setDataSource(context, uri)
                }
            }

            // Seek ~1 second in (1,000,000 microseconds) to avoid black introductory frames
            val targetTimeUs = 1_000_000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val scaled = retriever.getScaledFrameAtTime(
                    targetTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetWidth,
                    targetHeight
                )
                if (scaled != null) return scaled
            }

            if (cancellationSignal?.isCanceled == true) return null

            // Standard fallback
            val frame = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (cancellationSignal?.isCanceled == true) {
                frame?.recycle()
                return null
            }

            if (frame != null && (frame.width > targetWidth || frame.height > targetHeight)) {
                val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                if (scaled != frame) {
                    frame.recycle()
                }
                return scaled
            }
            return frame
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed to extract frame for $mediaUri: ${e.message}")
            return null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
            }
        }
    }
}
