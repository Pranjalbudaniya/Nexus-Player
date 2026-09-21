package com.nexus.player.feature.more.storage

import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.scanner.model.ScanState

/**
 * Domain representation of a storage folder displayed on the Storage Locations screen.
 */
data class StorageFolderItem(
    val uriOrPath: String,
    val displayName: String,
    val isSafFolder: Boolean,
    val videoCount: Int = 0,
    val isAccessible: Boolean = true,
    val lastModified: Long = 0L
)

/**
 * UI State for the Storage Locations management screen.
 */
data class StorageLocationsUiState(
    val accessMode: StorageAccessMode = StorageAccessMode.ALL_MEDIA,
    val isPermissionGranted: Boolean = false,
    val indexedFolders: List<StorageFolderItem> = emptyList(),
    val excludedFolders: Set<String> = emptySet(),
    val scanState: ScanState = ScanState.Idle,
    val isScanning: Boolean = false,
    val folderPendingRemoval: StorageFolderItem? = null,
    val isRemoveDialogConfirmedDeleteVideos: Boolean = false,
    val userMessage: String? = null
)
