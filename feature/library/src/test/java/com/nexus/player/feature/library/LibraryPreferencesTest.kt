package com.nexus.player.feature.library

import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortDirection
import com.nexus.player.feature.library.preferences.LibrarySortField
import com.nexus.player.feature.library.preferences.LibrarySortOption
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPreferencesTest {

    @Test
    fun defaultSortOption_mapsToDateAddedDesc() {
        val option = LibrarySortOption()
        assertEquals(LibrarySortField.RECENTLY_ADDED, option.field)
        assertEquals(LibrarySortDirection.DESCENDING, option.direction)
        assertEquals(VideoSortOrder.DATE_ADDED_DESC, option.toVideoSortOrder())
    }

    @Test
    fun sortOptionMapping_allFieldsAndDirections() {
        // RECENTLY_ADDED
        assertEquals(
            VideoSortOrder.DATE_ADDED_DESC,
            LibrarySortOption(LibrarySortField.RECENTLY_ADDED, LibrarySortDirection.DESCENDING).toVideoSortOrder()
        )
        assertEquals(
            VideoSortOrder.DATE_ADDED_ASC,
            LibrarySortOption(LibrarySortField.RECENTLY_ADDED, LibrarySortDirection.ASCENDING).toVideoSortOrder()
        )

        // NAME
        assertEquals(
            VideoSortOrder.TITLE_ASC,
            LibrarySortOption(LibrarySortField.NAME, LibrarySortDirection.ASCENDING).toVideoSortOrder()
        )
        assertEquals(
            VideoSortOrder.TITLE_DESC,
            LibrarySortOption(LibrarySortField.NAME, LibrarySortDirection.DESCENDING).toVideoSortOrder()
        )

        // DURATION
        assertEquals(
            VideoSortOrder.DURATION_DESC,
            LibrarySortOption(LibrarySortField.DURATION, LibrarySortDirection.DESCENDING).toVideoSortOrder()
        )
        assertEquals(
            VideoSortOrder.DURATION_ASC,
            LibrarySortOption(LibrarySortField.DURATION, LibrarySortDirection.ASCENDING).toVideoSortOrder()
        )

        // FILE_SIZE
        assertEquals(
            VideoSortOrder.SIZE_DESC,
            LibrarySortOption(LibrarySortField.FILE_SIZE, LibrarySortDirection.DESCENDING).toVideoSortOrder()
        )
        assertEquals(
            VideoSortOrder.SIZE_ASC,
            LibrarySortOption(LibrarySortField.FILE_SIZE, LibrarySortDirection.ASCENDING).toVideoSortOrder()
        )

        // LAST_PLAYED
        assertEquals(
            VideoSortOrder.LAST_PLAYED_DESC,
            LibrarySortOption(LibrarySortField.LAST_PLAYED, LibrarySortDirection.DESCENDING).toVideoSortOrder()
        )
        assertEquals(
            VideoSortOrder.LAST_PLAYED_ASC,
            LibrarySortOption(LibrarySortField.LAST_PLAYED, LibrarySortDirection.ASCENDING).toVideoSortOrder()
        )
    }

    @Test
    fun layoutModes_exist() {
        assertEquals(2, LibraryLayoutMode.entries.size)
        assertEquals(LibraryLayoutMode.GRID, LibraryLayoutMode.valueOf("GRID"))
        assertEquals(LibraryLayoutMode.LIST, LibraryLayoutMode.valueOf("LIST"))
    }

    @Test
    fun libraryTabs_exist() {
        assertEquals(2, com.nexus.player.feature.library.preferences.LibraryTab.entries.size)
        assertEquals(com.nexus.player.feature.library.preferences.LibraryTab.VIDEOS, com.nexus.player.feature.library.preferences.LibraryTab.valueOf("VIDEOS"))
        assertEquals(com.nexus.player.feature.library.preferences.LibraryTab.FOLDERS, com.nexus.player.feature.library.preferences.LibraryTab.valueOf("FOLDERS"))
    }

    @Test
    fun folderSortOptionMapping_allFieldsAndDirections() {
        // NAME
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.NAME_ASC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.NAME,
                com.nexus.player.feature.library.preferences.FolderSortDirection.ASCENDING
            ).toFolderSortOrder()
        )
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.NAME_DESC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.NAME,
                com.nexus.player.feature.library.preferences.FolderSortDirection.DESCENDING
            ).toFolderSortOrder()
        )

        // VIDEO_COUNT
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.VIDEO_COUNT_DESC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.VIDEO_COUNT,
                com.nexus.player.feature.library.preferences.FolderSortDirection.DESCENDING
            ).toFolderSortOrder()
        )
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.VIDEO_COUNT_ASC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.VIDEO_COUNT,
                com.nexus.player.feature.library.preferences.FolderSortDirection.ASCENDING
            ).toFolderSortOrder()
        )

        // RECENTLY_MODIFIED
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.MODIFIED_DESC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.RECENTLY_MODIFIED,
                com.nexus.player.feature.library.preferences.FolderSortDirection.DESCENDING
            ).toFolderSortOrder()
        )
        assertEquals(
            com.nexus.player.core.database.repository.FolderSortOrder.MODIFIED_ASC,
            com.nexus.player.feature.library.preferences.FolderSortOption(
                com.nexus.player.feature.library.preferences.FolderSortField.RECENTLY_MODIFIED,
                com.nexus.player.feature.library.preferences.FolderSortDirection.ASCENDING
            ).toFolderSortOrder()
        )
    }
}
