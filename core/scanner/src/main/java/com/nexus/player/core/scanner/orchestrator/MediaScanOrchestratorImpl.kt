package com.nexus.player.core.scanner.orchestrator

import android.util.Log
import com.nexus.player.core.common.network.ApplicationScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.scanner.MediaScanner
import com.nexus.player.core.scanner.model.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.scanner.model.ScanOptions
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaScanOrchestrator"

/**
 * Production implementation of [MediaScanOrchestrator].
 * Ensures scans run asynchronously on [ApplicationScope] without blocking the main thread,
 * prevents concurrent scans, and safely handles permission revocation or scan errors.
 */
@Singleton
class MediaScanOrchestratorImpl @Inject constructor(
    private val storageAccessRepository: StorageAccessRepository,
    private val mediaScanner: MediaScanner,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository? = null
) : MediaScanOrchestrator {

    override val scanState: StateFlow<ScanState> = mediaScanner.scanState

    override val isScanning: StateFlow<Boolean> = scanState
        .map { it is ScanState.Scanning }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val launchMutex = Mutex()
    private var activeJob: Job? = null

    override fun triggerStartupScan(): Boolean {
        synchronized(this) {
            if (activeJob?.isActive == true) {
                Log.d(TAG, "Startup scan skipped: an active scan is already running")
                return false
            }

            activeJob = applicationScope.launch(ioDispatcher) {
                launchMutex.withLock {
                    try {
                        val settings = settingsRepository?.settings?.first()
                        if (settings != null && !settings.library.scanOnAppLaunch) {
                            Log.i(TAG, "Startup scan skipped: scanOnAppLaunch is disabled in settings")
                            return@withLock
                        }

                        val accessState = storageAccessRepository.storageAccessState.first()
                        if (!accessState.hasValidStorageAccess) {
                            Log.w(TAG, "Startup scan skipped: storage access is not granted or incomplete")
                            return@withLock
                        }

                        Log.i(TAG, "Triggering asynchronous startup media scan")
                        mediaScanner.startScan(ScanOptions(isIncremental = true))
                    } catch (e: CancellationException) {
                        Log.i(TAG, "Startup scan job was cancelled")
                    } catch (e: Exception) {
                        Log.e(TAG, "Non-fatal error encountered during startup scan: ${e.message}", e)
                    }
                }
            }
            return true
        }
    }

    override fun triggerManualScan(): Boolean {
        return launchScanJob(ScanOptions(isIncremental = false, forceMetadataRefresh = false), "manual media rescan")
    }

    override fun triggerIncrementalScan(): Boolean {
        return launchScanJob(ScanOptions(isIncremental = true), "incremental media refresh")
    }

    override fun triggerLocationScan(folderUriOrPath: String): Boolean {
        return launchScanJob(
            ScanOptions(isIncremental = false, targetFolderUriOrPath = folderUriOrPath),
            "target location scan for $folderUriOrPath"
        )
    }

    private fun launchScanJob(options: ScanOptions, description: String): Boolean {
        synchronized(this) {
            if (activeJob?.isActive == true) {
                Log.d(TAG, "Scan skipped ($description): an active scan is already running")
                return false
            }

            activeJob = applicationScope.launch(ioDispatcher) {
                launchMutex.withLock {
                    try {
                        val accessState = storageAccessRepository.storageAccessState.first()
                        if (!accessState.hasValidStorageAccess) {
                            Log.w(TAG, "Scan skipped ($description): storage access is not granted")
                            return@withLock
                        }

                        Log.i(TAG, "Triggering $description")
                        mediaScanner.startScan(options)
                    } catch (e: CancellationException) {
                        Log.i(TAG, "Scan job ($description) was cancelled")
                    } catch (e: Exception) {
                        Log.e(TAG, "Non-fatal error encountered during $description: ${e.message}", e)
                    }
                }
            }
            return true
        }
    }

    override fun cancelScan() {
        synchronized(this) {
            mediaScanner.cancelScan()
            activeJob?.cancel(CancellationException("Scan cancelled by user or orchestrator"))
            activeJob = null
        }
    }
}
