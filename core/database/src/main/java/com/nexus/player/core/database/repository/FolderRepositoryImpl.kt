package com.nexus.player.core.database.repository

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.model.asDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [FolderRepository] backed by Room [VideoDao].
 */
@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : FolderRepository {

    override fun getFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> {
        return videoDao.getFolders()
            .map { summaries ->
                val folders = summaries.map { it.asDomain() }
                sortFolders(folders, sortOrder)
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootFolders(sortOrder: FolderSortOrder): Flow<List<VideoFolder>> {
        return videoDao.getFolders()
            .map { summaries ->
                val folders = summaries.map { it.asDomain() }
                val paths = folders.map { it.folderPath }.toSet()
                // Root folders: folders whose parentPath is null or not in the set of known active paths
                val roots = folders.filter { folder ->
                    val parent = folder.parentPath
                    parent == null || parent !in paths
                }
                sortFolders(roots, sortOrder)
            }
            .flowOn(ioDispatcher)
    }

    override fun getChildFolders(
        parentFolderPath: String,
        sortOrder: FolderSortOrder
    ): Flow<List<VideoFolder>> {
        val cleanParent = parentFolderPath.trimEnd('/')
        return videoDao.getFolders()
            .map { summaries ->
                val folders = summaries.map { it.asDomain() }
                val children = folders.filter { folder ->
                    folder.parentPath?.trimEnd('/') == cleanParent
                }
                sortFolders(children, sortOrder)
            }
            .flowOn(ioDispatcher)
    }

    override fun getFolderByPath(folderPath: String): Flow<VideoFolder?> {
        return videoDao.getFolders()
            .map { summaries ->
                summaries.firstOrNull { it.folderPath == folderPath }?.asDomain()
            }
            .flowOn(ioDispatcher)
    }

    override fun getVideosInFolder(
        folderPath: String,
        sortOrder: VideoSortOrder
    ): Flow<List<Video>> {
        val entityFlow = when (sortOrder) {
            VideoSortOrder.TITLE_ASC -> videoDao.getVideosByFolderTitleAsc(folderPath)
            VideoSortOrder.TITLE_DESC -> videoDao.getVideosByFolderTitleDesc(folderPath)
            VideoSortOrder.DATE_ADDED_DESC -> videoDao.getVideosByFolderDateAddedDesc(folderPath)
            VideoSortOrder.DATE_ADDED_ASC -> videoDao.getVideosByFolderDateAddedAsc(folderPath)
            VideoSortOrder.DURATION_DESC -> videoDao.getVideosByFolderDurationDesc(folderPath)
            VideoSortOrder.DURATION_ASC -> videoDao.getVideosByFolderDurationAsc(folderPath)
            VideoSortOrder.SIZE_DESC -> videoDao.getVideosByFolderSizeDesc(folderPath)
            VideoSortOrder.SIZE_ASC -> videoDao.getVideosByFolderSizeAsc(folderPath)
            VideoSortOrder.LAST_PLAYED_DESC,
            VideoSortOrder.LAST_PLAYED_ASC -> videoDao.getVideosByFolderTitleAsc(folderPath)
        }
        return entityFlow
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    private fun sortFolders(
        folders: List<VideoFolder>,
        sortOrder: FolderSortOrder
    ): List<VideoFolder> {
        return when (sortOrder) {
            FolderSortOrder.NAME_ASC -> folders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.folderName })
            FolderSortOrder.NAME_DESC -> folders.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.folderName })
            FolderSortOrder.VIDEO_COUNT_DESC -> folders.sortedByDescending { it.videoCount }
            FolderSortOrder.VIDEO_COUNT_ASC -> folders.sortedBy { it.videoCount }
            FolderSortOrder.MODIFIED_DESC -> folders.sortedByDescending { it.lastModified }
            FolderSortOrder.MODIFIED_ASC -> folders.sortedBy { it.lastModified }
        }
    }
}
