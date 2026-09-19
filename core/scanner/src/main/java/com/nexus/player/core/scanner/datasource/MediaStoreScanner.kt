package com.nexus.player.core.scanner.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.nexus.player.core.scanner.util.VideoFormatDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaStoreScanner"

/**
 * Discovers video media from the system [MediaStore] database.
 * Streams items incrementally to maintain low memory usage for thousands of files.
 */
@Singleton
open class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Performs an incremental scan of all external video media.
     * Invokes [onItemDiscovered] for each valid video file found.
     */
    open suspend fun scanMediaStore(
        onItemDiscovered: suspend (DiscoveredMediaItem) -> Unit
    ): Int {
        val contentResolver: ContentResolver = context.contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        var count = 0

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(collection, projection, null, null, sortOrder)
            if (cursor == null) {
                Log.w(TAG, "MediaStore query returned null cursor")
                return 0
            }

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()

                val rawId = cursor.getLong(idColumn)
                val mediaUri = ContentUris.withAppendedId(collection, rawId).toString()
                val stableId = "mediastore_$rawId"

                val fileName = cursor.getString(nameColumn) ?: "Video_$rawId"
                val mimeType = cursor.getString(mimeColumn)

                // Filter valid video format
                if (!VideoFormatDetector.isVideoFile(fileName, mimeType)) {
                    continue
                }

                val sizeBytes = cursor.getLong(sizeColumn)
                // Filter 0-byte corrupt/placeholder files
                if (sizeBytes <= 0) {
                    continue
                }

                val durationMs = cursor.getLong(durationColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val dateAddedSec = cursor.getLong(dateAddedColumn)
                val dateModifiedSec = cursor.getLong(dateModifiedColumn)

                val dateAdded = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                val lastModified = if (dateModifiedSec > 0) dateModifiedSec * 1000L else System.currentTimeMillis()

                val folderName = cursor.getString(bucketColumn) ?: "Videos"
                val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                val folderPath = if (!filePath.isNullOrBlank()) {
                    File(filePath).parent ?: "/storage/emulated/0/$folderName"
                } else {
                    "/storage/emulated/0/$folderName"
                }

                val title = VideoFormatDetector.extractDisplayTitle(fileName)

                val item = DiscoveredMediaItem(
                    id = stableId,
                    mediaUri = mediaUri,
                    filePath = filePath,
                    fileName = fileName,
                    title = title,
                    folderName = folderName,
                    folderPath = folderPath,
                    sizeBytes = sizeBytes,
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    dateAdded = dateAdded,
                    lastModified = lastModified,
                    mimeType = mimeType
                )

                onItemDiscovered(item)
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iterating MediaStore: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return count
    }
}
