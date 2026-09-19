package com.nexus.player.core.media

import android.graphics.Bitmap
import android.os.CancellationSignal
import com.nexus.player.core.media.thumbnail.ThumbnailDecoder
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.media.thumbnail.ThumbnailLoaderImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ThumbnailLoaderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDecoder: FakeThumbnailDecoder
    private lateinit var thumbnailLoader: ThumbnailLoader

    @Before
    fun setup() {
        fakeDecoder = FakeThumbnailDecoder()
        thumbnailLoader = ThumbnailLoaderImpl(
            decoder = fakeDecoder,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun thumbnailSuccessAndCaching() = runTest(testDispatcher) {
        val sampleBitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        fakeDecoder.resultMap["content://media/1"] = sampleBitmap

        // First load - decodes and caches
        val bitmap = thumbnailLoader.loadThumbnail("content://media/1", 320, 180)
        assertNotNull(bitmap)
        assertEquals(1, fakeDecoder.decodeCount.get())
        assertEquals(1, thumbnailLoader.getCachedEntriesCount())

        // Second load - retrieved from LRU cache
        val cachedBitmap = thumbnailLoader.loadThumbnail("content://media/1", 320, 180)
        assertNotNull(cachedBitmap)
        assertEquals(1, fakeDecoder.decodeCount.get()) // No second decode!
        assertEquals(bitmap, cachedBitmap)
    }

    @Test
    fun thumbnailFailureGracefulNull() = runTest(testDispatcher) {
        // Not in resultMap -> returns null
        val bitmap = thumbnailLoader.loadThumbnail("content://media/corrupt_or_missing", 320, 180)
        assertNull(bitmap)
        assertEquals(0, thumbnailLoader.getCachedEntriesCount())
    }

    @Test
    fun contentUriHandling() = runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888)
        val contentUri = "content://media/external/video/media/9876"
        fakeDecoder.resultMap[contentUri] = bitmap

        val loaded = thumbnailLoader.loadThumbnail(contentUri, 160, 90)
        assertNotNull(loaded)
        assertEquals(contentUri, fakeDecoder.lastRequestedUri)
        assertEquals(160, fakeDecoder.lastRequestedWidth)
        assertEquals(90, fakeDecoder.lastRequestedHeight)
    }

    @Test
    fun clearMemoryCacheEvictsEntries() = runTest(testDispatcher) {
        fakeDecoder.resultMap["uri_1"] = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        fakeDecoder.resultMap["uri_2"] = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        thumbnailLoader.loadThumbnail("uri_1", 10, 10)
        thumbnailLoader.loadThumbnail("uri_2", 10, 10)
        assertEquals(2, thumbnailLoader.getCachedEntriesCount())

        thumbnailLoader.clearMemoryCache()
        assertEquals(0, thumbnailLoader.getCachedEntriesCount())
    }

    @Test
    fun concurrentRequestLimiting() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledDecoder = ControlledThumbnailDecoder()
        val customLoader = ThumbnailLoaderImpl(
            decoder = controlledDecoder,
            ioDispatcher = standardDispatcher
        )

        // Launch 5 parallel decode requests
        val jobs = (1..5).map { index ->
            async(standardDispatcher) {
                customLoader.loadThumbnail("uri_$index", 100, 100)
            }
        }

        testScheduler.runCurrent()

        // Max concurrent decodes is 3 (Semaphore(3))
        assertEquals(3, controlledDecoder.activeDecodes.get())
        assertTrue("Max concurrent decodes must not exceed 3", controlledDecoder.peakDecodes.get() <= 3)

        // Complete one item
        controlledDecoder.completeOne()
        testScheduler.runCurrent()

        // The 4th item enters
        assertTrue("Max concurrent decodes must not exceed 3", controlledDecoder.peakDecodes.get() <= 3)

        // Complete remaining items
        controlledDecoder.completeAll()
        testScheduler.advanceUntilIdle()

        jobs.forEach { job ->
            assertNotNull(job.await())
        }
    }

    @Test
    fun cancellationHaltsDecoding() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        val controlledDecoder = ControlledThumbnailDecoder()
        val customLoader = ThumbnailLoaderImpl(
            decoder = controlledDecoder,
            ioDispatcher = standardDispatcher
        )

        val job = launch(standardDispatcher) {
            try {
                customLoader.loadThumbnail("uri_cancel", 100, 100)
            } catch (ignored: CancellationException) {
            }
        }

        testScheduler.runCurrent()
        assertEquals(1, controlledDecoder.activeDecodes.get())

        // Cancel job
        job.cancel()
        testScheduler.advanceUntilIdle()

        assertTrue(controlledDecoder.wasSignalCancelled)
    }
}

// --- Test Fakes ---

class FakeThumbnailDecoder : ThumbnailDecoder {
    val resultMap = mutableMapOf<String, Bitmap>()
    val decodeCount = AtomicInteger(0)
    var lastRequestedUri: String? = null
    var lastRequestedWidth: Int = 0
    var lastRequestedHeight: Int = 0

    override suspend fun decodeFrame(
        mediaUri: String,
        targetWidth: Int,
        targetHeight: Int,
        cancellationSignal: CancellationSignal?
    ): Bitmap? {
        decodeCount.incrementAndGet()
        lastRequestedUri = mediaUri
        lastRequestedWidth = targetWidth
        lastRequestedHeight = targetHeight
        return resultMap[mediaUri]
    }
}

class ControlledThumbnailDecoder : ThumbnailDecoder {
    val activeDecodes = AtomicInteger(0)
    val peakDecodes = AtomicInteger(0)
    var wasSignalCancelled = false
    private var autoComplete = false
    private val pendingCompletions = mutableListOf<CompletableDeferred<Bitmap>>()

    override suspend fun decodeFrame(
        mediaUri: String,
        targetWidth: Int,
        targetHeight: Int,
        cancellationSignal: CancellationSignal?
    ): Bitmap? {
        val active = activeDecodes.incrementAndGet()
        peakDecodes.updateAndGet { current -> maxOf(current, active) }

        cancellationSignal?.setOnCancelListener {
            wasSignalCancelled = true
        }

        synchronized(pendingCompletions) {
            if (autoComplete) {
                activeDecodes.decrementAndGet()
                return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            }
        }

        val deferred = CompletableDeferred<Bitmap>()
        synchronized(pendingCompletions) {
            pendingCompletions.add(deferred)
        }

        return try {
            deferred.await()
        } finally {
            activeDecodes.decrementAndGet()
        }
    }

    fun completeOne() {
        val item = synchronized(pendingCompletions) {
            if (pendingCompletions.isNotEmpty()) pendingCompletions.removeAt(0) else null
        }
        item?.complete(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    }

    fun completeAll() {
        val list = synchronized(pendingCompletions) {
            autoComplete = true
            val copy = pendingCompletions.toList()
            pendingCompletions.clear()
            copy
        }
        for (item in list) {
            item.complete(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        }
    }
}
