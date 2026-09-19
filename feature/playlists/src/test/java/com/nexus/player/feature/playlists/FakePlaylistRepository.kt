package com.nexus.player.feature.playlists

import com.nexus.player.core.database.model.Playlist
import com.nexus.player.core.database.model.PlaylistItem
import com.nexus.player.core.database.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FakePlaylistRepository : PlaylistRepository {

    private val playlists = mutableListOf<Playlist>()
    private val playlistItems = mutableMapOf<String, MutableList<PlaylistItem>>()

    private val playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    private val itemsFlow = MutableStateFlow<Map<String, List<PlaylistItem>>>(emptyMap())

    private fun emit() {
        playlistsFlow.value = playlists.map { p ->
            val items = playlistItems[p.id] ?: emptyList()
            p.copy(itemCount = items.size, thumbnailVideoIds = items.take(4).map { it.videoId })
        }
        itemsFlow.value = playlistItems.mapValues { it.value.toList() }
    }

    override fun observePlaylists(): Flow<List<Playlist>> = playlistsFlow

    override fun observePlaylist(playlistId: String): Flow<Playlist?> {
        return playlistsFlow.map { list -> list.find { it.id == playlistId } }
    }

    override fun observePlaylistVideos(playlistId: String): Flow<List<PlaylistItem>> {
        return itemsFlow.map { map -> map[playlistId] ?: emptyList() }
    }

    override suspend fun getPlaylists(): List<Playlist> = playlistsFlow.value

    override suspend fun getPlaylistById(playlistId: String): Playlist? {
        return playlistsFlow.value.find { it.id == playlistId }
    }

    override suspend fun getPlaylistVideoIds(playlistId: String): List<String> {
        return playlistItems[playlistId]?.map { it.videoId } ?: emptyList()
    }

    override suspend fun createPlaylist(name: String, description: String?): Result<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        if (trimmed.length > 100) return Result.failure(IllegalArgumentException("Playlist name cannot exceed 100 characters"))
        if (playlists.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("A playlist named \"$trimmed\" already exists"))
        }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val playlist = Playlist(
            id = id,
            name = trimmed,
            description = description?.trim()?.ifEmpty { null },
            createdAt = now,
            updatedAt = now,
            itemCount = 0
        )
        playlists.add(playlist)
        playlistItems[id] = mutableListOf()
        emit()
        return Result.success(id)
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        if (trimmed.length > 100) return Result.failure(IllegalArgumentException("Playlist name cannot exceed 100 characters"))
        if (playlists.any { it.name.equals(trimmed, ignoreCase = true) && it.id != playlistId }) {
            return Result.failure(IllegalArgumentException("A playlist named \"$trimmed\" already exists"))
        }
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return Result.failure(NoSuchElementException("Playlist not found"))

        playlists[idx] = playlists[idx].copy(name = trimmed, updatedAt = System.currentTimeMillis())
        emit()
        return Result.success(Unit)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlists.removeAll { it.id == playlistId }
        playlistItems.remove(playlistId)
        emit()
    }

    override suspend fun addVideoToPlaylist(playlistId: String, videoId: String): Result<Unit> {
        val list = playlistItems[playlistId] ?: return Result.failure(NoSuchElementException("Playlist not found"))
        if (list.any { it.videoId == videoId }) {
            return Result.failure(IllegalStateException("Video is already in this playlist"))
        }
        val pos = list.size
        list.add(
            PlaylistItem(
                id = (pos + 1).toLong(),
                playlistId = playlistId,
                videoId = videoId,
                position = pos,
                addedAt = System.currentTimeMillis(),
                video = null,
                isAvailable = true
            )
        )
        emit()
        return Result.success(Unit)
    }

    override suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        val list = playlistItems[playlistId] ?: return
        list.removeAll { it.videoId == videoId }
        val reindexed = list.mapIndexed { index, item -> item.copy(position = index) }
        playlistItems[playlistId] = reindexed.toMutableList()
        emit()
    }

    override suspend fun reorderVideos(playlistId: String, orderedVideoIds: List<String>) {
        val list = playlistItems[playlistId] ?: return
        val map = list.associateBy { it.videoId }
        val reordered = orderedVideoIds.mapIndexedNotNull { index, id ->
            map[id]?.copy(position = index)
        }
        playlistItems[playlistId] = reordered.toMutableList()
        emit()
    }
}
