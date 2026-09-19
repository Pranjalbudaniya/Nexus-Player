package com.nexus.player.feature.library

import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.feature.library.preferences.FolderSortOption
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortOption
import com.nexus.player.feature.library.preferences.LibraryTab

/**
 * Presentation state for the Nexus Player Library screen.
 */
data class LibraryUiState(
    val videos: List<MediaMetadata> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.VIDEOS,
    val layoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID,
    val sortOption: LibrarySortOption = LibrarySortOption(),
    val folderSortOption: FolderSortOption = FolderSortOption(),
    val scanState: ScanState = ScanState.Idle,
    val isScanning: Boolean = false,
    val isStorageAccessGranted: Boolean = true,
    val isLoading: Boolean = true,
    val selectedVideoForMenu: MediaMetadata? = null,
    val isSortSheetVisible: Boolean = false,
    val isFolderSortSheetVisible: Boolean = false
) {
    /**
     * True if the library contains no items for the current active tab and is not in an initial loading state.
     */
    val isEmpty: Boolean
        get() = !isLoading && when (selectedTab) {
            LibraryTab.VIDEOS -> videos.isEmpty()
            LibraryTab.FOLDERS -> folders.isEmpty()
        }
}
