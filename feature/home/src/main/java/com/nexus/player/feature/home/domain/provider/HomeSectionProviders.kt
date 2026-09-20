package com.nexus.player.feature.home.domain.provider

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.feature.home.domain.model.ContinueWatchingVideo
import com.nexus.player.feature.home.domain.model.FavoriteVideo
import com.nexus.player.feature.home.domain.model.HomeFolder
import com.nexus.player.feature.home.domain.model.RecentlyAddedVideo
import com.nexus.player.feature.home.domain.model.toContinueWatchingVideo
import com.nexus.player.feature.home.domain.model.toFavoriteVideo
import com.nexus.player.feature.home.domain.model.toHomeFolder
import com.nexus.player.feature.home.domain.model.toRecentlyAddedVideo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section provider for Continue Watching items.
 */
interface ContinueWatchingSectionProvider {
    fun getContinueWatching(limit: Int = 10): Flow<List<ContinueWatchingVideo>>
}

/**
 * Section provider for Recently Added video items.
 */
interface RecentlyAddedSectionProvider {
    fun getRecentlyAdded(limit: Int = 10): Flow<List<RecentlyAddedVideo>>
}

/**
 * Section provider for Favorite video items.
 */
interface FavoritesSectionProvider {
    fun getFavorites(limit: Int = 10): Flow<List<FavoriteVideo>>
}

/**
 * Section provider for root media folders on Home.
 */
interface FoldersSectionProvider {
    fun getHomeFolders(limit: Int = 10): Flow<List<HomeFolder>>
}

/**
 * Production implementation of [ContinueWatchingSectionProvider].
 * Strictly enforces meaningful progress, zero-duration exclusion, missing timestamp exclusion,
 * and the 95% completion exclusion rule.
 */
@Singleton
class ContinueWatchingSectionProviderImpl @Inject constructor(
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ContinueWatchingSectionProvider {

    override fun getContinueWatching(limit: Int): Flow<List<ContinueWatchingVideo>> {
        return videoRepository.getContinueWatchingVideos(limit)
            .map { videos ->
                videos
                    .asSequence()
                    .filter { video ->
                        val lastPlayed = video.lastPlayedAt
                        // Exclude deleted or invalid media
                        video.mediaUri.isNotBlank() &&
                        // Exclude zero-duration or negative duration
                        video.durationMs > 0L &&
                        // Must have meaningful saved playback progress (> 0 and not trivial)
                        video.playbackPositionMs > 0L &&
                        // Must have valid last played timestamp
                        lastPlayed != null && lastPlayed > 0L &&
                        // Respect 95% completion exclusion rule and isCompleted flag
                        !video.isCompleted &&
                        video.playbackPercentage < 0.95f &&
                        // Sanity check: position cannot exceed or equal 95% of duration
                        video.playbackPositionMs < (video.durationMs * 0.95f).toLong()
                    }
                    .sortedByDescending { it.lastPlayedAt ?: 0L }
                    .take(limit)
                    .map { it.toContinueWatchingVideo() }
                    .toList()
            }
            .flowOn(ioDispatcher)
    }
}

/**
 * Production implementation of [RecentlyAddedSectionProvider].
 * Provides recently discovered videos sorted by discovery/addition timestamp.
 */
@Singleton
class RecentlyAddedSectionProviderImpl @Inject constructor(
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : RecentlyAddedSectionProvider {

    override fun getRecentlyAdded(limit: Int): Flow<List<RecentlyAddedVideo>> {
        return videoRepository.getRecentlyAddedVideos(limit)
            .map { videos ->
                val now = System.currentTimeMillis()
                videos
                    .asSequence()
                    .filter { it.mediaUri.isNotBlank() && it.durationMs >= 0L }
                    .sortedByDescending { it.dateAdded }
                    .take(limit)
                    .map { it.toRecentlyAddedVideo(currentTimeMillis = now) }
                    .toList()
            }
            .flowOn(ioDispatcher)
    }
}

/**
 * Production implementation of [FavoritesSectionProvider].
 * Reacts automatically when favorite state changes.
 */
@Singleton
class FavoritesSectionProviderImpl @Inject constructor(
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : FavoritesSectionProvider {

    override fun getFavorites(limit: Int): Flow<List<FavoriteVideo>> {
        return videoRepository.getFavoriteVideos(limit)
            .map { videos ->
                videos
                    .asSequence()
                    .filter { it.isFavorite && it.mediaUri.isNotBlank() }
                    .take(limit)
                    .map { it.toFavoriteVideo() }
                    .toList()
            }
            .flowOn(ioDispatcher)
    }
}

/**
 * Production implementation of [FoldersSectionProvider].
 * Provides root-level folders with video count and representative preview media without scanning filesystem.
 */
@Singleton
class FoldersSectionProviderImpl @Inject constructor(
    private val folderRepository: FolderRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : FoldersSectionProvider {

    override fun getHomeFolders(limit: Int): Flow<List<HomeFolder>> {
        return folderRepository.getRootFolders(FolderSortOrder.NAME_ASC)
            .map { folders ->
                folders
                    .asSequence()
                    .filter { it.videoCount > 0 && it.folderPath.isNotBlank() }
                    .take(limit)
                    .map { it.toHomeFolder() }
                    .toList()
            }
            .flowOn(ioDispatcher)
    }
}
