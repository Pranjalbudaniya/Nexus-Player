package com.nexus.player.core.database.repository

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.SearchSortOrder
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.model.asDomain
import com.nexus.player.core.database.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    override fun searchVideos(query: String): Flow<List<Video>> {
        return searchVideos(SearchFilter(query = query))
    }

    override fun searchVideos(filter: SearchFilter): Flow<List<Video>> {
        val trimmedQuery = filter.query.trim()
        val hasFilters = filter.resolution != null ||
            filter.minDurationMs != null ||
            filter.maxDurationMs != null ||
            filter.folderPath != null

        if (trimmedQuery.isEmpty() && !hasFilters) {
            return flowOf(emptyList())
        }

        return videoDao.getAllVideosByDateAddedDesc()
            .map { entities ->
                val domainList = entities.map { it.asDomain() }
                val filtered = domainList.filter { video ->
                    matchesSearch(video, trimmedQuery, filter)
                }
                rankAndSort(filtered, trimmedQuery, filter.sortOrder)
            }
            .flowOn(ioDispatcher)
    }

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

    private fun matchesSearch(video: Video, query: String, filter: SearchFilter): Boolean {
        // Filter constraints
        if (filter.resolution != null && !video.resolutionLabel.equals(filter.resolution, ignoreCase = true)) {
            return false
        }
        if (filter.minDurationMs != null && video.durationMs < filter.minDurationMs) {
            return false
        }
        if (filter.maxDurationMs != null && video.durationMs > filter.maxDurationMs) {
            return false
        }
        if (filter.folderPath != null && !video.folderPath.equals(filter.folderPath, ignoreCase = true)) {
            return false
        }

        if (query.isEmpty()) return true

        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true

        val titleLower = video.title.lowercase()
        val fileNameLower = video.fileName.lowercase()
        val folderNameLower = video.folderName.lowercase()
        val folderPathLower = video.folderPath.lowercase()
        val resLower = video.resolutionLabel.lowercase()
        val vCodecLower = video.videoCodec?.lowercase().orEmpty()
        val aCodecLower = video.audioCodec?.lowercase().orEmpty()
        val durationFormatted = formatDurationInternal(video.durationMs)
        val dateYear = getYearFromEpoch(video.dateAdded)
        val dateFormatted = formatDateInternal(video.dateAdded).lowercase()

        return tokens.all { token ->
            titleLower.contains(token) ||
            fileNameLower.contains(token) ||
            folderNameLower.contains(token) ||
            folderPathLower.contains(token) ||
            resLower.contains(token) ||
            vCodecLower.contains(token) ||
            aCodecLower.contains(token) ||
            durationFormatted.contains(token) ||
            dateYear == token ||
            dateFormatted.contains(token) ||
            matchesDurationToken(video.durationMs, token)
        }
    }

    private fun rankAndSort(
        videos: List<Video>,
        query: String,
        sortOrder: SearchSortOrder
    ): List<Video> {
        return when (sortOrder) {
            SearchSortOrder.DATE_ADDED_DESC -> videos.sortedByDescending { it.dateAdded }
            SearchSortOrder.TITLE_ASC -> videos.sortedBy { it.title.lowercase() }
            SearchSortOrder.DURATION_DESC -> videos.sortedByDescending { it.durationMs }
            SearchSortOrder.SIZE_DESC -> videos.sortedByDescending { it.sizeBytes }
            SearchSortOrder.RELEVANCE -> {
                if (query.isEmpty()) {
                    videos.sortedByDescending { it.dateAdded }
                } else {
                    videos.sortedWith(
                        compareBy<Video> { video ->
                            when {
                                video.title.equals(query, ignoreCase = true) -> 0
                                video.title.startsWith(query, ignoreCase = true) -> 1
                                video.title.contains(query, ignoreCase = true) -> 2
                                video.fileName.startsWith(query, ignoreCase = true) -> 3
                                video.fileName.contains(query, ignoreCase = true) -> 4
                                else -> 5
                            }
                        }.thenByDescending { it.dateAdded }
                    )
                }
            }
        }
    }

    private fun matchesDurationToken(durationMs: Long, token: String): Boolean {
        if (durationMs <= 0L) return false
        val durationMinutes = durationMs / 60000
        val durationHours = durationMs / 3600000
        return when {
            token == "${durationMinutes}m" || token == "${durationMinutes}min" -> true
            token == "${durationHours}h" || token == "${durationHours}hr" -> true
            token == "short" && durationMinutes < 5 -> true
            token == "medium" && durationMinutes in 5..20 -> true
            token == "long" && durationMinutes > 20 -> true
            else -> false
        }
    }

    private fun formatDurationInternal(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatDateInternal(epochMillis: Long): String {
        if (epochMillis <= 0) return ""
        return try {
            SimpleDateFormat("MMMM d yyyy", Locale.US).format(Date(epochMillis))
        } catch (_: Exception) {
            ""
        }
    }

    private fun getYearFromEpoch(epochMillis: Long): String {
        if (epochMillis <= 0) return ""
        return try {
            SimpleDateFormat("yyyy", Locale.US).format(Date(epochMillis))
        } catch (_: Exception) {
            ""
        }
    }
}
