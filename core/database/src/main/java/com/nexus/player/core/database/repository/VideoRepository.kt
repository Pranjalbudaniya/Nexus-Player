package com.nexus.player.core.database.repository

import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations on local video media records.
 *
 * Exposes reactive [Flow] streams of pure domain [Video] objects, decoupling
 * data-access logic from consumers.
 */
interface VideoRepository {

    /**
     * Search indexed videos by text query across title, fileName, folder, resolution,
     * duration, date added, and codecs.
     */
    fun searchVideos(query: String): Flow<List<Video>> = searchVideos(SearchFilter(query = query))

    /**
     * Search indexed videos with advanced filter specifications (resolution, duration, folder, sort).
     */
    fun searchVideos(filter: SearchFilter): Flow<List<Video>> = kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Observe all videos with the requested [sortOrder].
     */
    fun getAllVideos(sortOrder: VideoSortOrder = VideoSortOrder.TITLE_ASC): Flow<List<Video>>

    /**
     * Observe recently added videos up to [limit].
     */
    fun getRecentlyAddedVideos(limit: Int = 20): Flow<List<Video>>

    /**
     * Observe favorite videos.
     */
    fun getFavoriteVideos(): Flow<List<Video>>

    /**
     * Observe favorite videos up to [limit].
     */
    fun getFavoriteVideos(limit: Int): Flow<List<Video>> = getFavoriteVideos()

    /**
     * Observe in-progress videos eligible for "Continue Watching".
     */
    fun getContinueWatchingVideos(limit: Int = 10): Flow<List<Video>>

    /**
     * Observe playback history.
     */
    fun getHistoryVideos(limit: Int = 20): Flow<List<Video>>

    /**
     * Observe all playback history.
     */
    fun getAllHistoryVideos(): Flow<List<Video>> = getHistoryVideos(limit = 1000)

    /**
     * Observe videos within a specific [folderPath].
     */
    fun getVideosByFolder(folderPath: String): Flow<List<Video>>

    /**
     * Observe available folders with counts.
     */
    fun getFolders(): Flow<List<VideoFolder>>

    /**
     * Retrieve a video by its stable unique [id].
     */
    suspend fun getVideoById(id: String): Video?

    /**
     * Retrieve a video by its content/file [mediaUri].
     */
    suspend fun getVideoByUri(mediaUri: String): Video?

    /**
     * Total number of indexed videos.
     */
    suspend fun getVideosCount(): Int

    /**
     * Strictly insert a video. Fails if duplicate exists.
     */
    suspend fun insertVideo(video: Video): Long

    /**
     * Upsert a video (inserts or updates).
     */
    suspend fun upsertVideo(video: Video)

    /**
     * Bulk upsert videos (batch scanning).
     */
    suspend fun upsertVideos(videos: List<Video>)

    /**
     * Update playback progress and completion metrics.
     */
    suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        percentage: Float,
        lastPlayedAt: Long
    ) {
        updatePlaybackProgress(id, positionMs, percentage, lastPlayedAt, false)
    }

    /**
     * Update playback progress and completion metrics with explicit completion status.
     */
    suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        percentage: Float,
        lastPlayedAt: Long,
        isCompleted: Boolean
    ) {
        updatePlaybackProgress(id, positionMs, percentage, lastPlayedAt)
    }

    /**
     * Restarts playback session for a video (e.g. when reopening a completed video).
     * Sets position to 0, completion to false, updates lastPlayedAt, and increments watchCount.
     */
    suspend fun restartPlayback(id: String, startTimeMs: Long = System.currentTimeMillis()) {}

    /**
     * Clear history record for a single video. Resets playback position, percentage, and completion status.
     */
    suspend fun clearHistoryForVideo(id: String) {}

    /**
     * Clear all playback history. Resets playback position, percentage, and completion status for all played videos.
     */
    suspend fun clearAllHistory() {}

    /**
     * Toggle or set favorite state.
     */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /**
     * Delete a video record by stable [id].
     */
    suspend fun deleteVideo(id: String)

    /**
     * Delete a video record by [mediaUri].
     */
    suspend fun deleteVideoByUri(mediaUri: String)

    /**
     * Clean up stale records no longer found in [validIds].
     */
    suspend fun deleteStaleVideos(validIds: List<String>)

    /**
     * Clear all video records.
     */
    suspend fun clearAll()
}
