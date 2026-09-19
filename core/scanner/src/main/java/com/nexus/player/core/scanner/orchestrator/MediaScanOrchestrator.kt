package com.nexus.player.core.scanner.orchestrator

import com.nexus.player.core.scanner.model.ScanState
import kotlinx.coroutines.flow.StateFlow

/**
 * Application-level coordinator for local media library scanning.
 * Manages concurrency, startup discovery triggers, manual rescan requests,
 * and lifecycle-safe execution.
 */
interface MediaScanOrchestrator {

    /**
     * Observable stream representing the scanning lifecycle, progress, or error.
     */
    val scanState: StateFlow<ScanState>

    /**
     * Convenience observable indicating whether a scan is actively running.
     */
    val isScanning: StateFlow<Boolean>

    /**
     * Triggers the initial startup scan if valid storage access is granted.
     * Guaranteed to be non-blocking. If a scan is already running, safely no-ops.
     *
     * @return true if a new scan was started; false if ignored (e.g. already scanning or no access).
     */
    fun triggerStartupScan(): Boolean

    /**
     * Manually triggers a full library rescan (e.g., from Settings or pull-to-refresh).
     *
     * @return true if a new scan was started; false if a scan is already in progress.
     */
    fun triggerManualScan(): Boolean

    /**
     * Cancels an active scan cooperatively.
     */
    fun cancelScan()
}
