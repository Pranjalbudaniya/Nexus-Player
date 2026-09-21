package com.nexus.player.core.scanner

import com.nexus.player.core.scanner.model.ScanOptions
import com.nexus.player.core.scanner.model.ScanResult
import com.nexus.player.core.scanner.model.ScanState
import kotlinx.coroutines.flow.StateFlow

/**
 * Main contract for the local video media scanner.
 * Observes storage access permissions, discovers local videos, extracts technical metadata,
 * and synchronizes the Room database.
 */
interface MediaScanner {

    /**
     * Observable stream representing the real-time scanning lifecycle and progress.
     */
    val scanState: StateFlow<ScanState>

    /**
     * Executes a library scan asynchronously with the specified [options].
     * Returns the [ScanResult] summary on completion.
     */
    suspend fun startScan(options: ScanOptions = ScanOptions()): ScanResult

    /**
     * Cancels an ongoing scan cooperatively.
     */
    fun cancelScan()
}
