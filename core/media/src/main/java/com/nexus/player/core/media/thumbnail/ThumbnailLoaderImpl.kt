package com.nexus.player.core.media.thumbnail

import android.graphics.Bitmap
import android.os.CancellationSignal
import android.util.Log
import android.util.LruCache
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ThumbnailLoader"
private const val MAX_CONCURRENT_DECODES = 3

/**
 * Production implementation of [ThumbnailLoader].
 * Provides memory-bounded LRU caching and limits simultaneous decoding pipelines
 * to prevent frame drops and high memory pressure during fast lazy grid scrolling.
 */
@Singleton
class ThumbnailLoaderImpl @Inject constructor(
    private val decoder: ThumbnailDecoder,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ThumbnailLoader {

    // Calculate in-memory cache limit: 1/8th of available application heap
    private val cacheLimitKb: Int = run {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        maxOf(1024 * 8, maxMemoryKb / 8) // Minimum 8 MB, default 1/8th of max heap
    }

    private val memoryCache = object : LruCache<String, Bitmap>(cacheLimitKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return maxOf(1, value.byteCount / 1024)
        }
    }

    // Limit concurrent decoding operations to avoid pegging CPU/GPU
    private val decodingSemaphore = Semaphore(MAX_CONCURRENT_DECODES)

    override suspend fun loadThumbnail(
        mediaUri: String,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? = withContext(ioDispatcher) {
        if (mediaUri.isBlank()) return@withContext null

        val cacheKey = buildCacheKey(mediaUri, targetWidth, targetHeight)

        // Step 1: Check in-memory LRU cache
        memoryCache.get(cacheKey)?.let { cached ->
            return@withContext cached
        }

        // Step 2: Decode under concurrency gate
        val cancellationSignal = CancellationSignal()
        try {
            decodingSemaphore.withPermit {
                currentCoroutineContext().ensureActive()

                // Double check cache in case a previous parallel request decoded it
                memoryCache.get(cacheKey)?.let { cached ->
                    return@withContext cached
                }

                val decoded = decoder.decodeFrame(
                    mediaUri = mediaUri,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight,
                    cancellationSignal = cancellationSignal
                )

                if (decoded != null) {
                    memoryCache.put(cacheKey, decoded)
                }
                decoded
            }
        } catch (e: CancellationException) {
            cancellationSignal.cancel()
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail decoding failed for $mediaUri: ${e.message}")
            null
        }
    }

    override fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    override fun getCachedEntriesCount(): Int {
        return memoryCache.snapshot().size
    }

    private fun buildCacheKey(mediaUri: String, width: Int, height: Int): String {
        return "$mediaUri-$width-$height"
    }
}
