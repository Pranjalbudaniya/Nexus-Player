package com.nexus.player.feature.player.screenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for safely capturing and persisting video screenshots into Android's MediaStore.
 *
 * Adheres to modern scoped storage (no broad storage permissions on API 29+)
 * and handles metadata, unique names, and duplicate avoidance safely.
 */
object ScreenshotManager {

    private const val TAG = "ScreenshotManager"
    private const val DIRECTORY_NAME = "NexusPlayer"

    /**
     * Sanitizes a title string for use in safe filesystem and display names.
     */
    fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(50)
            .ifBlank { "Video" }
    }

    /**
     * Generates a descriptive, unique filename without exposing internal database identifiers.
     */
    fun generateFilename(videoTitle: String, timestamp: Long = System.currentTimeMillis()): String {
        val sanitized = sanitizeTitle(videoTitle)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val dateString = dateFormat.format(Date(timestamp))
        return "Nexus_${sanitized}_$dateString.jpg"
    }

    /**
     * Persists the given [bitmap] into Android MediaStore under Pictures/NexusPlayer.
     *
     * @param context Application or Activity context.
     * @param bitmap The video frame bitmap to save.
     * @param videoTitle Title of the video being played.
     * @return [Result] containing the saved image [Uri], or an exception on failure.
     */
    suspend fun saveScreenshot(
        context: Context,
        bitmap: Bitmap,
        videoTitle: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val filename = generateFilename(videoTitle)
        val contentResolver = context.contentResolver

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.DESCRIPTION, "Captured from $videoTitle in Nexus Player")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$DIRECTORY_NAME")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = contentResolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore record"))

            contentResolver.openOutputStream(itemUri).use { outputStream ->
                if (outputStream == null) {
                    throw IllegalStateException("Failed to open output stream for $itemUri")
                }
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                if (!compressed) {
                    throw IllegalStateException("Failed to compress screenshot bitmap to JPEG")
                }
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            }

            Log.d(TAG, "Screenshot successfully saved: $itemUri ($filename)")
            Result.success(itemUri)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save screenshot for '$videoTitle'", e)
            Result.failure(e)
        }
    }
}
