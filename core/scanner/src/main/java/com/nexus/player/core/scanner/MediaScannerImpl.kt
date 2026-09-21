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
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.scanner.model.ScanOptions
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
import kotlinx.coroutines.flow.firstOrNull
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
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository? = null
) : MediaScanner {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanMutex = Mutex()
    private var activeJob: Job? = null

    override suspend fun startScan(options: ScanOptions): ScanResult = withContext(ioDispatcher) {
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

            val currentSettings = settingsRepository?.settings?.value?.library
            val excludedFolders = (currentSettings?.excludedFolders ?: emptySet()) + accessState.excludedFolderPaths
            val includeHiddenFiles = currentSettings?.includeHiddenFiles ?: false

            var scannedCount = 0
            var insertedCount = 0
            var updatedCount = 0
            val discoveredIds = mutableSetOf<String>()
            val processedUris = mutableSetOf<String>()
            val scannedFolderPaths = mutableSetOf<String>()
            val pendingBatch = ArrayList<Video>(BATCH_SIZE)

            try {
                val initialDbCount = videoRepository.getVideosCount()

                // Pre-load lightweight in-memory lookup to avoid N+1 SQLite queries during discovery
                val scanLookup = if (options.forceMetadataRefresh) {
                    emptyMap()
                } else if (options.targetFolderUriOrPath != null && !options.targetFolderUriOrPath.startsWith("content://")) {
                    videoRepository.getScanLookupForFolder(options.targetFolderUriOrPath)
                } else {
                    videoRepository.getAllScanLookup()
                }
                val uriLookup = if (scanLookup.isNotEmpty()) {
                    scanLookup.values.associateBy { it.mediaUri }
                } else {
                    emptyMap()
                }

                val itemProcessor: suspend (DiscoveredMediaItem) -> Unit = { item ->
                    currentCoroutineContext().ensureActive()

                    val isHidden = (item.fileName.startsWith(".") ||
                        item.folderPath.split("/").any { it.startsWith(".") }) && !includeHiddenFiles

                    val isExcluded = excludedFolders.any { excluded ->
                        val cleanEx = excluded.trimEnd('/')
                        item.folderPath.equals(cleanEx, ignoreCase = true) ||
                            item.folderPath.startsWith("$cleanEx/", ignoreCase = true) ||
                            item.mediaUri.startsWith(cleanEx)
                    }

                    val matchesTarget = if (options.targetFolderUriOrPath != null) {
                        val target = options.targetFolderUriOrPath.trimEnd('/')
                        item.folderPath.equals(target, ignoreCase = true) ||
                            item.folderPath.startsWith("$target/", ignoreCase = true) ||
                            item.mediaUri.startsWith(target)
                    } else {
                        true
                    }

                    if (!isHidden && !isExcluded && matchesTarget) {
                        // Guard: Avoid processing identical URI references multiple times in a single scan pass
                        if (processedUris.add(item.mediaUri)) {
                            discoveredIds.add(item.id)
                            scannedFolderPaths.add(item.folderPath)
                            scannedCount++

                            _scanState.value = ScanState.Scanning(
                                ScanProgress(
                                    current = scannedCount,
                                    total = null,
                                    currentFile = item.fileName
                                )
                            )

                            try {
                                val cachedMeta = if (options.forceMetadataRefresh) null else (
                                    scanLookup[item.id] ?: uriLookup[item.mediaUri]
                                )

                                val isUnchanged = cachedMeta != null &&
                                    cachedMeta.lastModified == item.lastModified &&
                                    cachedMeta.sizeBytes == item.sizeBytes &&
                                    cachedMeta.fileName == item.fileName &&
                                    cachedMeta.folderPath == item.folderPath

                                if (!isUnchanged) {
                                    val existingId = cachedMeta?.id ?: item.id
                                    val existing = videoRepository.getVideoById(existingId)
                                        ?: videoRepository.getVideoByUri(item.mediaUri)

                                    val video = metadataExtractor.extractMetadata(item, existing)
                                    if (existing == null) {
                                        insertedCount++
                                    } else {
                                        updatedCount++
                                    }

                                    pendingBatch.add(video)
                                    if (pendingBatch.size >= BATCH_SIZE) {
                                        videoRepository.upsertVideos(pendingBatch)
                                        pendingBatch.clear()
                                        yield()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing media item ${item.mediaUri}: ${e.message}")
                            }
                        }
                    }
                }

                // Execute appropriate discovery strategy based on access mode and target option
                if (options.targetFolderUriOrPath != null) {
                    val target = options.targetFolderUriOrPath
                    if (target.startsWith("content://")) {
                        scannedFolderPaths.add(target)
                        safFolderScanner.scanFolders(setOf(target), itemProcessor)
                    } else {
                        scannedFolderPaths.add(target)
                        mediaStoreScanner.scanMediaStore(itemProcessor)
                    }
                } else {
                    when (accessState.accessMode) {
                        StorageAccessMode.ALL_MEDIA -> {
                            val existingFolders = try {
                                videoRepository.getFolders().firstOrNull()?.map { it.folderPath } ?: emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val nonExcluded = existingFolders.filter { f ->
                                val cleanF = f.trimEnd('/')
                                !excludedFolders.any { ex ->
                                    val cleanEx = ex.trimEnd('/')
                                    cleanF.equals(cleanEx, ignoreCase = true) ||
                                        cleanF.startsWith("$cleanEx/", ignoreCase = true)
                                }
                            }
                            scannedFolderPaths.addAll(nonExcluded)
                            mediaStoreScanner.scanMediaStore(itemProcessor)
                        }
                        StorageAccessMode.SELECTED_FOLDERS -> {
                            val foldersToScan = accessState.selectedFolderUris.filter { uri ->
                                !excludedFolders.contains(uri)
                            }.toSet()
                            scannedFolderPaths.addAll(foldersToScan)
                            safFolderScanner.scanFolders(foldersToScan, itemProcessor)
                        }
                    }
                }

                // Flush remaining items in the pending batch
                if (pendingBatch.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    videoRepository.upsertVideos(pendingBatch)
                    pendingBatch.clear()
                }

                // Scoped stale file cleanup: prune items only in locations that were actively scanned.
                // Media records in excluded folders or temporarily inaccessible folders are preserved.
                var removedCount = 0
                if (scannedFolderPaths.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    videoRepository.deleteStaleVideosInFolders(
                        validIds = discoveredIds.toList(),
                        scannedFolderPaths = scannedFolderPaths.toList()
                    )
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
