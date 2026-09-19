package com.nexus.player.feature.library.preferences

import com.nexus.player.core.database.model.VideoSortOrder

/**
 * Display layout mode for the video library.
 */
enum class LibraryLayoutMode {
    GRID,
    LIST
}

/**
 * Sortable dimensions supported by the media library.
 */
enum class LibrarySortField(val displayName: String) {
    RECENTLY_ADDED("Recently Added"),
    NAME("Name"),
    DURATION("Duration"),
    FILE_SIZE("File Size"),
    LAST_PLAYED("Last Played")
}

/**
 * Sort direction.
 */
enum class LibrarySortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

/**
 * Combined sort configuration encapsulating field and direction.
 */
data class LibrarySortOption(
    val field: LibrarySortField = LibrarySortField.RECENTLY_ADDED,
    val direction: LibrarySortDirection = LibrarySortDirection.DESCENDING
) {
    /**
     * Maps the UI sort preference to the Room database [VideoSortOrder].
     */
    fun toVideoSortOrder(): VideoSortOrder = when (field) {
        LibrarySortField.RECENTLY_ADDED -> when (direction) {
            LibrarySortDirection.DESCENDING -> VideoSortOrder.DATE_ADDED_DESC
            LibrarySortDirection.ASCENDING -> VideoSortOrder.DATE_ADDED_ASC
        }
        LibrarySortField.NAME -> when (direction) {
            LibrarySortDirection.ASCENDING -> VideoSortOrder.TITLE_ASC
            LibrarySortDirection.DESCENDING -> VideoSortOrder.TITLE_DESC
        }
        LibrarySortField.DURATION -> when (direction) {
            LibrarySortDirection.DESCENDING -> VideoSortOrder.DURATION_DESC
            LibrarySortDirection.ASCENDING -> VideoSortOrder.DURATION_ASC
        }
        LibrarySortField.FILE_SIZE -> when (direction) {
            LibrarySortDirection.DESCENDING -> VideoSortOrder.SIZE_DESC
            LibrarySortDirection.ASCENDING -> VideoSortOrder.SIZE_ASC
        }
        LibrarySortField.LAST_PLAYED -> when (direction) {
            LibrarySortDirection.DESCENDING -> VideoSortOrder.LAST_PLAYED_DESC
            LibrarySortDirection.ASCENDING -> VideoSortOrder.LAST_PLAYED_ASC
        }
    }
}

/**
 * Top-level Library browsing view mode.
 */
enum class LibraryTab(val displayName: String) {
    VIDEOS("Videos"),
    FOLDERS("Folders")
}

/**
 * Sortable dimensions supported for folders.
 */
enum class FolderSortField(val displayName: String) {
    NAME("Name"),
    VIDEO_COUNT("Video Count"),
    RECENTLY_MODIFIED("Recently Updated")
}

/**
 * Sort direction for folders.
 */
enum class FolderSortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

/**
 * Combined sort configuration for folders.
 */
data class FolderSortOption(
    val field: FolderSortField = FolderSortField.NAME,
    val direction: FolderSortDirection = FolderSortDirection.ASCENDING
) {
    fun toFolderSortOrder(): com.nexus.player.core.database.repository.FolderSortOrder = when (field) {
        FolderSortField.NAME -> when (direction) {
            FolderSortDirection.ASCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.NAME_ASC
            FolderSortDirection.DESCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.NAME_DESC
        }
        FolderSortField.VIDEO_COUNT -> when (direction) {
            FolderSortDirection.DESCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.VIDEO_COUNT_DESC
            FolderSortDirection.ASCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.VIDEO_COUNT_ASC
        }
        FolderSortField.RECENTLY_MODIFIED -> when (direction) {
            FolderSortDirection.DESCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.MODIFIED_DESC
            FolderSortDirection.ASCENDING -> com.nexus.player.core.database.repository.FolderSortOrder.MODIFIED_ASC
        }
    }
}
