package com.nexus.player.core.database.repository

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.model.asDomain
import com.nexus.player.core.database.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [VideoRepository] backed by Room and executed on [NexusDispatchers.IO].
 */
@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : VideoRepository {

    override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> {
        val entityFlow = when (sortOrder) {
            VideoSortOrder.TITLE_ASC -> videoDao.getAllVideosByTitleAsc()
            VideoSortOrder.TITLE_DESC -> videoDao.getAllVideosByTitleDesc()
            VideoSortOrder.DATE_ADDED_DESC -> videoDao.getAllVideosByDateAddedDesc()
            VideoSortOrder.DATE_ADDED_ASC -> videoDao.getAllVideosByDateAddedAsc()
            VideoSortOrder.DURATION_DESC -> videoDao.getAllVideosByDurationDesc()
            VideoSortOrder.DURATION_ASC -> videoDao.getAllVideosByDurationAsc()
            VideoSortOrder.SIZE_DESC -> videoDao.getAllVideosBySizeDesc()
            VideoSortOrder.SIZE_ASC -> videoDao.getAllVideosBySizeAsc()
            VideoSortOrder.LAST_PLAYED_DESC -> videoDao.getAllVideosByLastPlayedDesc()
            VideoSortOrder.LAST_PLAYED_ASC -> videoDao.getAllVideosByLastPlayedAsc()
        }
        return entityFlow
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> {
        return videoDao.getRecentlyAddedVideos(limit)
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getFavoriteVideos(): Flow<List<Video>> {
        return videoDao.getFavoriteVideos()
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getFavoriteVideos(limit: Int): Flow<List<Video>> {
        return videoDao.getFavoriteVideosWithLimit(limit)
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> {
        return videoDao.getContinueWatchingVideos(limit)
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getHistoryVideos(limit: Int): Flow<List<Video>> {
        return videoDao.getHistoryVideos(limit)
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getVideosByFolder(folderPath: String): Flow<List<Video>> {
        return videoDao.getVideosByFolder(folderPath)
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getFolders(): Flow<List<VideoFolder>> {
        return videoDao.getFolders()
            .map { summaries -> summaries.map { it.asDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getVideoById(id: String): Video? = withContext(ioDispatcher) {
        videoDao.getVideoById(id)?.asDomain()
    }

    override suspend fun getVideoByUri(mediaUri: String): Video? = withContext(ioDispatcher) {
        videoDao.getVideoByUri(mediaUri)?.asDomain()
    }

    override suspend fun getVideosCount(): Int = withContext(ioDispatcher) {
        videoDao.getVideosCount()
    }

    override suspend fun insertVideo(video: Video): Long = withContext(ioDispatcher) {
        videoDao.insertVideo(video.asEntity())
    }

    override suspend fun upsertVideo(video: Video) = withContext(ioDispatcher) {
        videoDao.upsertVideo(video.asEntity())
    }

    override suspend fun upsertVideos(videos: List<Video>) = withContext(ioDispatcher) {
        videoDao.upsertVideos(videos.map { it.asEntity() })
    }

    override suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        percentage: Float,
        lastPlayedAt: Long
    ) = withContext(ioDispatcher) {
        videoDao.updatePlaybackProgress(id, positionMs, percentage, lastPlayedAt)
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        videoDao.updateFavorite(id, isFavorite)
    }

    override suspend fun deleteVideo(id: String) = withContext(ioDispatcher) {
        videoDao.deleteById(id)
    }

    override suspend fun deleteVideoByUri(mediaUri: String) = withContext(ioDispatcher) {
        videoDao.deleteByUri(mediaUri)
    }

    override suspend fun deleteStaleVideos(validIds: List<String>) = withContext(ioDispatcher) {
        videoDao.deleteStaleVideos(validIds)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        videoDao.clearAll()
    }
}
