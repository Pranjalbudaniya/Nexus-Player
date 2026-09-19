package com.nexus.player.core.common.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePermissionHelperTest {

    @Test
    fun getRequiredPermissions_returnsNonEmptyList() {
        val permissions = StoragePermissionHelper.getRequiredPermissions()
        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty())
    }

    @Test
    fun storageAccessState_hasValidStorageAccessLogic() {
        // Case 1: Initial state (no permission, all-media mode, no folders)
        val initial = StorageAccessState(
            isOnboardingCompleted = false,
            isPermissionGranted = false,
            accessMode = StorageAccessMode.ALL_MEDIA,
            selectedFolderUris = emptySet()
        )
        assertFalse(initial.hasValidStorageAccess)

        // Case 2: Media permission granted
        val withPerm = initial.copy(isPermissionGranted = true)
        assertTrue(withPerm.hasValidStorageAccess)

        // Case 3: Permission not granted, but in SELECTED_FOLDERS mode with valid folders
        val withFolders = initial.copy(
            isPermissionGranted = false,
            accessMode = StorageAccessMode.SELECTED_FOLDERS,
            selectedFolderUris = setOf("content://com.android.externalstorage.documents/tree/Movies")
        )
        assertTrue(withFolders.hasValidStorageAccess)

        // Case 4: SELECTED_FOLDERS mode but empty set
        val emptyFolders = initial.copy(
            isPermissionGranted = false,
            accessMode = StorageAccessMode.SELECTED_FOLDERS,
            selectedFolderUris = emptySet()
        )
        assertFalse(emptyFolders.hasValidStorageAccess)
    }
}
