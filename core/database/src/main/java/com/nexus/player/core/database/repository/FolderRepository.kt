package com.nexus.player.core.database.repository

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Sorting orders supported for folder collections.
 */
enum class FolderSortOrder {
    NAME_ASC,
    NAME_DESC,
    VIDEO_COUNT_DESC,
    VIDEO_COUNT_ASC,
    MODIFIED_DESC,
    MODIFIED_ASC
}

/**
 * Repository interface defining operations on video folders.
 */
interface FolderRepository {

    /**
     * Observe all discovered folders with the specified [sortOrder].
     */
    fun getFolders(sortOrder: FolderSortOrder = FolderSortOrder.NAME_ASC): Flow<List<VideoFolder>>

    /**
     * Observe root-level folders (folders that do not have an ancestor in the active library).
     */
    fun getRootFolders(sortOrder: FolderSortOrder = FolderSortOrder.NAME_ASC): Flow<List<VideoFolder>>

    /**
     * Observe direct child folders under [parentFolderPath].
     */
    fun getChildFolders(
        parentFolderPath: String,
        sortOrder: FolderSortOrder = FolderSortOrder.NAME_ASC
    ): Flow<List<VideoFolder>>

    /**
     * Look up a single folder by its unique [folderPath].
     */
    fun getFolderByPath(folderPath: String): Flow<VideoFolder?>

    /**
     * Observe videos directly contained in [folderPath] with specified [sortOrder].
     */
    fun getVideosInFolder(
        folderPath: String,
        sortOrder: VideoSortOrder = VideoSortOrder.TITLE_ASC
    ): Flow<List<Video>>
}
