package com.nexus.player.feature.library.folder

import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortOption

/**
 * UI state for a specific folder browsing screen.
 */
data class FolderUiState(
    val folderPath: String = "",
    val folderName: String = "",
    val subfolders: List<VideoFolder> = emptyList(),
    val videos: List<MediaMetadata> = emptyList(),
    val layoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID,
    val sortOption: LibrarySortOption = LibrarySortOption(),
    val isLoading: Boolean = true,
    val selectedVideoForMenu: MediaMetadata? = null,
    val isSortSheetVisible: Boolean = false,
    val allFolders: List<VideoFolder> = emptyList()
) {
    val isEmpty: Boolean
        get() = !isLoading && subfolders.isEmpty() && videos.isEmpty()
}
