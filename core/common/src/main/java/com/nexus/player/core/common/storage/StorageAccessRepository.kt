package com.nexus.player.core.common.storage

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface governing storage access decisions, DataStore preferences,
 * and version-aware media permission queries.
 *
 * Prepared for consumption by both the Onboarding flow and future Settings -> Storage Access UI.
 */
interface StorageAccessRepository {

    /**
     * Observable stream of the current storage access state and preferences.
     */
    val storageAccessState: Flow<StorageAccessState>

    /**
     * Persists the onboarding completion flag.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Persists the active storage access mode (ALL_MEDIA vs SELECTED_FOLDERS).
     */
    suspend fun setStorageAccessMode(mode: StorageAccessMode)

    /**
     * Adds a persistent SAF folder URI to the indexed directories.
     */
    suspend fun addSelectedFolderUri(uriString: String)

    /**
     * Removes an indexed folder URI.
     */
    suspend fun removeSelectedFolderUri(uriString: String)

    /**
     * Clears all selected folder URIs.
     */
    suspend fun clearSelectedFolders()

    /**
     * Checks if the required media permission is currently granted.
     */
    fun isPermissionGranted(): Boolean

    /**
     * Returns the required permissions for the device's Android version.
     */
    fun getRequiredPermissions(): List<String>
}
