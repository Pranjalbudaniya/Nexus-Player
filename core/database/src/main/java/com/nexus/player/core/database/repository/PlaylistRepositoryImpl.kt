package com.nexus.player.core.database.repository

import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.dao.PlaylistDao
import com.nexus.player.core.database.entity.PlaylistEntity
import com.nexus.player.core.database.entity.PlaylistItemEntity
import com.nexus.player.core.database.model.Playlist
import com.nexus.player.core.database.model.PlaylistItem
import com.nexus.player.core.database.model.asDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [PlaylistRepository] backed by Room and executed on [NexusDispatchers.IO].
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observePlaylistsWithCount()
            .map { summaries ->
                summaries.map { summary ->
                    val thumbs = playlistDao.getThumbnailVideoIds(summary.id, limit = 4)
                    Playlist(
                        id = summary.id,
                        name = summary.name,
                        description = summary.description,
                        createdAt = summary.createdAt,
                        updatedAt = summary.updatedAt,
                        itemCount = summary.itemCount,
                        thumbnailVideoIds = thumbs
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun observePlaylist(playlistId: String): Flow<Playlist?> {
        return playlistDao.observePlaylistById(playlistId)
            .map { entity ->
                entity?.let {
                    val count = playlistDao.getItemCount(it.id)
                    val thumbs = playlistDao.getThumbnailVideoIds(it.id, limit = 4)
                    Playlist(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        itemCount = count,
                        thumbnailVideoIds = thumbs
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun observePlaylistVideos(playlistId: String): Flow<List<PlaylistItem>> {
        return playlistDao.observePlaylistItemsWithVideo(playlistId)
            .map { itemsWithVideo ->
                itemsWithVideo.map { rel ->
                    PlaylistItem(
                        id = rel.item.id,
                        playlistId = rel.item.playlistId,
                        videoId = rel.item.videoId,
                        position = rel.item.position,
                        addedAt = rel.item.addedAt,
                        video = rel.video?.asDomain(),
                        isAvailable = rel.video != null
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getPlaylists(): List<Playlist> = withContext(ioDispatcher) {
        val entities = playlistDao.getAllPlaylists()
        entities.map { entity ->
            val count = playlistDao.getItemCount(entity.id)
            val thumbs = playlistDao.getThumbnailVideoIds(entity.id, limit = 4)
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                itemCount = count,
                thumbnailVideoIds = thumbs
            )
        }
    }

    override suspend fun getPlaylistById(playlistId: String): Playlist? = withContext(ioDispatcher) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return@withContext null
        val count = playlistDao.getItemCount(entity.id)
        val thumbs = playlistDao.getThumbnailVideoIds(entity.id, limit = 4)
        Playlist(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            itemCount = count,
            thumbnailVideoIds = thumbs
        )
    }

    override suspend fun getPlaylistVideoIds(playlistId: String): List<String> = withContext(ioDispatcher) {
        playlistDao.getPlaylistVideoIds(playlistId)
    }

    override suspend fun createPlaylist(name: String, description: String?): Result<String> = withContext(ioDispatcher) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }
        if (trimmedName.length > 100) {
            return@withContext Result.failure(IllegalArgumentException("Playlist name cannot exceed 100 characters"))
        }
        val existing = playlistDao.getPlaylistByName(trimmedName)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("A playlist named \"$trimmedName\" already exists"))
        }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = id,
            name = trimmedName,
            description = description?.trim()?.ifEmpty { null },
            createdAt = now,
            updatedAt = now
        )
        playlistDao.insertPlaylist(entity)
        Result.success(id)
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit> = withContext(ioDispatcher) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }
        if (trimmedName.length > 100) {
            return@withContext Result.failure(IllegalArgumentException("Playlist name cannot exceed 100 characters"))
        }
        val existing = playlistDao.getPlaylistByName(trimmedName)
        if (existing != null && existing.id != playlistId) {
            return@withContext Result.failure(IllegalArgumentException("A playlist named \"$trimmedName\" already exists"))
        }
        val current = playlistDao.getPlaylistById(playlistId)
            ?: return@withContext Result.failure(NoSuchElementException("Playlist not found"))

        val now = System.currentTimeMillis()
        playlistDao.updatePlaylist(
            current.copy(
                name = trimmedName,
                updatedAt = now
            )
        )
        Result.success(Unit)
    }

    override suspend fun deletePlaylist(playlistId: String): Unit = withContext(ioDispatcher) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addVideoToPlaylist(playlistId: String, videoId: String): Result<Unit> = withContext(ioDispatcher) {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return@withContext Result.failure(NoSuchElementException("Playlist not found"))

        val existingItem = playlistDao.getPlaylistItem(playlistId, videoId)
        if (existingItem != null) {
            return@withContext Result.failure(IllegalStateException("Video is already in this playlist"))
        }

        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        val now = System.currentTimeMillis()
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoId = videoId,
                position = maxPos + 1,
                addedAt = now
            )
        )
        playlistDao.updatePlaylist(playlist.copy(updatedAt = now))
        Result.success(Unit)
    }

    override suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String): Unit = withContext(ioDispatcher) {
        playlistDao.deletePlaylistItem(playlistId, videoId)
        val remaining = playlistDao.getPlaylistItems(playlistId)
        val reordered = remaining.mapIndexed { index, item -> item.copy(position = index) }
        playlistDao.updatePlaylistItems(reordered)
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun reorderVideos(playlistId: String, orderedVideoIds: List<String>): Unit = withContext(ioDispatcher) {
        playlistDao.reorderItems(playlistId, orderedVideoIds)
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
