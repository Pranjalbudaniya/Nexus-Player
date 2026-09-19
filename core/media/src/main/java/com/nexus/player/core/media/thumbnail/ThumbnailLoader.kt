package com.nexus.player.core.media.thumbnail

import android.graphics.Bitmap

/**
 * High-performance, memory-controlled thumbnail provider for video items.
 * Utilizes an in-memory LRU cache and downscaled asynchronous decoding.
 */
interface ThumbnailLoader {

    /**
     * Loads or generates a downsampled thumbnail for [mediaUri].
     * Returns cached bitmap immediately if available; otherwise decodes asynchronously.
     *
     * @param mediaUri Content URI or file path of the video.
     * @param targetWidth Desired preview width in pixels (default 320).
     * @param targetHeight Desired preview height in pixels (default 180).
     * @return Decoded [Bitmap], or null if decoding failed or was cancelled.
     */
    suspend fun loadThumbnail(
        mediaUri: String,
        targetWidth: Int = 320,
        targetHeight: Int = 180
    ): Bitmap?

    /**
     * Evicts all entries from the temporary in-memory LRU cache.
     */
    fun clearMemoryCache()

    /**
     * Returns the current number of cached bitmap entries in memory.
     */
    fun getCachedEntriesCount(): Int
}
