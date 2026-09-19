package com.nexus.player.core.common.storage

/**
 * Modes of storage access supported by Nexus Player.
 */
enum class StorageAccessMode {
    /**
     * Scans all video files accessible via the Android MediaStore API.
     */
    ALL_MEDIA,

    /**
     * Scans strictly within folders selected by the user via the Storage Access Framework (SAF).
     */
    SELECTED_FOLDERS
}

/**
 * Storage and media access state.
 */
data class StorageAccessState(
    val isOnboardingCompleted: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val accessMode: StorageAccessMode = StorageAccessMode.ALL_MEDIA,
    val selectedFolderUris: Set<String> = emptySet()
) {
    /**
     * Returns true if Nexus Player has valid, actionable access to local media.
     */
    val hasValidStorageAccess: Boolean
        get() = isPermissionGranted || (accessMode == StorageAccessMode.SELECTED_FOLDERS && selectedFolderUris.isNotEmpty())
}
