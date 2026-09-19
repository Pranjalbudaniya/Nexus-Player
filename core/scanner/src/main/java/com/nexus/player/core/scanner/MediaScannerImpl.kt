package com.nexus.player.core.scanner

import android.util.Log
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.scanner.datasource.DiscoveredMediaItem
import com.nexus.player.core.scanner.datasource.MediaStoreScanner
import com.nexus.player.core.scanner.datasource.SafFolderScanner
import com.nexus.player.core.scanner.extractor.VideoMetadataExtractor
import com.nexus.player.core.scanner.model.ScanProgress
import com.nexus.player.core.scanner.model.ScanResult
import com.nexus.player.core.scanner.model.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaScanner"
private const val BATCH_SIZE = 50

/**
 * Production implementation of [MediaScanner].
 * Orchestrates multi-source discovery, incremental batching, change detection,
 * duplicate filtering, and stale record pruning.
 */
@Singleton
class MediaScannerImpl @Inject constructor(
    private val storageAccessRepository: StorageAccessRepository,
    private val mediaStoreScanner: MediaStoreScanner,
    private val safFolderScanner: SafFolderScanner,
    private val metadataExtractor: VideoMetadataExtractor,
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MediaScanner {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanMutex = Mutex()
    private var activeJob: Job? = null

    override suspend fun startScan(): ScanResult = withContext(ioDispatcher) {
        scanMutex.withLock {
            activeJob = currentCoroutineContext()[Job]
            val startTime = System.currentTimeMillis()
            _scanState.value = ScanState.Scanning(ScanProgress(current = 0, total = null))

            val accessState = storageAccessRepository.storageAccessState.first()
            if (!accessState.hasValidStorageAccess) {
                Log.w(TAG, "Cannot scan: valid storage access is not granted")
                val emptyResult = ScanResult(0, 0, 0, 0, System.currentTimeMillis() - startTime)
                _scanState.value = ScanState.Completed(emptyResult)
                return@withContext emptyResult
            }

            var scannedCount = 0
            var insertedCount = 0
            var updatedCount = 0
            val discoveredIds = mutableSetOf<String>()
            val processedUris = mutableSetOf<String>()
            val pendingBatch = ArrayList<Video>(BATCH_SIZE)

            try {
                val initialDbCount = videoRepository.getVideosCount()

                val itemProcessor: suspend (DiscoveredMediaItem) -> Unit = { item ->
                    currentCoroutineContext().ensureActive()

                    // Guard: Avoid processing identical URI references multiple times in a single scan pass
                    if (processedUris.add(item.mediaUri)) {
                        discoveredIds.add(item.id)
                        scannedCount++

                        _scanState.value = ScanState.Scanning(
                            ScanProgress(
                                current = scannedCount,
                                total = null,
                                currentFile = item.fileName
                            )
                        )

                        try {
                            val existing = videoRepository.getVideoById(item.id)
                                ?: videoRepository.getVideoByUri(item.mediaUri)

                            val video = metadataExtractor.extractMetadata(item, existing)
                            if (existing == null) {
                                insertedCount++
                            } else if (existing.lastModified != item.lastModified || existing.sizeBytes != item.sizeBytes) {
                                updatedCount++
                            }

                            pendingBatch.add(video)
                            if (pendingBatch.size >= BATCH_SIZE) {
                                videoRepository.upsertVideos(pendingBatch)
                                pendingBatch.clear()
                                yield()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing media item ${item.mediaUri}: ${e.message}")
                        }
                    }
                }

                // Execute appropriate discovery strategy based on access mode
                when (accessState.accessMode) {
                    StorageAccessMode.ALL_MEDIA -> {
                        mediaStoreScanner.scanMediaStore(itemProcessor)
                    }
                    StorageAccessMode.SELECTED_FOLDERS -> {
                        safFolderScanner.scanFolders(accessState.selectedFolderUris, itemProcessor)
                    }
                }

                // Flush remaining items in the pending batch
                if (pendingBatch.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    videoRepository.upsertVideos(pendingBatch)
                    pendingBatch.clear()
                }

                // Stale file cleanup: prune items no longer present on device
                var removedCount = 0
                if (discoveredIds.isNotEmpty() || initialDbCount > 0) {
                    currentCoroutineContext().ensureActive()
                    videoRepository.deleteStaleVideos(discoveredIds.toList())
                    val finalDbCount = videoRepository.getVideosCount()
                    removedCount = maxOf(0, (initialDbCount + insertedCount) - finalDbCount)
                }

                val duration = System.currentTimeMillis() - startTime
                val result = ScanResult(
                    totalScanned = scannedCount,
                    inserted = insertedCount,
                    updated = updatedCount,
                    removed = removedCount,
                    durationMs = duration
                )

                _scanState.value = ScanState.Completed(result)
                result
            } catch (e: CancellationException) {
                Log.i(TAG, "Scan was cancelled")
                _scanState.value = ScanState.Cancelled
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fatal scan error: ${e.message}", e)
                _scanState.value = ScanState.Error("Media scan failed: ${e.message}", e)
                throw e
            } finally {
                activeJob = null
            }
        }
    }

    override fun cancelScan() {
        activeJob?.cancel(CancellationException("Media scan explicitly cancelled"))
        _scanState.value = ScanState.Cancelled
    }
}
