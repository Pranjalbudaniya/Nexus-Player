package com.nexus.player.core.database.repository

import com.nexus.player.core.database.model.Playlist
import com.nexus.player.core.database.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository defining lifecycle-safe operations on user playlists and their video entries.
 */
interface PlaylistRepository {

    /**
     * Reactively observe all playlists with their video counts and preview thumbnail IDs.
     */
    fun observePlaylists(): Flow<List<Playlist>>

    /**
     * Reactively observe a single playlist by [playlistId].
     */
    fun observePlaylist(playlistId: String): Flow<Playlist?>

    /**
     * Reactively observe ordered items within a playlist, resolving video metadata.
     * Missing or deleted videos have their item [PlaylistItem.isAvailable] set to false.
     */
    fun observePlaylistVideos(playlistId: String): Flow<List<PlaylistItem>>

    /**
     * Retrieve a snapshot of all user playlists.
     */
    suspend fun getPlaylists(): List<Playlist>

    /**
     * Retrieve a single playlist by [playlistId].
     */
    suspend fun getPlaylistById(playlistId: String): Playlist?

    /**
     * Retrieve ordered video IDs for an active playlist.
     */
    suspend fun getPlaylistVideoIds(playlistId: String): List<String>

    /**
     * Create a new playlist.
     * Validates that name is not blank, not too long (<= 100 chars),
     * and not a duplicate of an existing playlist (case-insensitive).
     *
     * @return [Result] containing the generated playlist ID on success, or an error message on failure.
     */
    suspend fun createPlaylist(name: String, description: String? = null): Result<String>

    /**
     * Rename an existing playlist.
     * Validates that the new name is not blank, not too long, and not a duplicate.
     * Preserves playlist ID and its items.
     */
    suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit>

    /**
     * Delete a playlist and its associations.
     * Does NOT delete underlying video files or remove them from the library.
     */
    suspend fun deletePlaylist(playlistId: String)

    /**
     * Add a video to a playlist.
     * Enforces duplicate prevention: if the video is already in the playlist,
     * returns a failure Result.
     */
    suspend fun addVideoToPlaylist(playlistId: String, videoId: String): Result<Unit>

    /**
     * Remove a video from a playlist.
     * Re-compacts remaining item positions.
     */
    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String)

    /**
     * Reorder videos inside a playlist with persistent index updates.
     */
    suspend fun reorderVideos(playlistId: String, orderedVideoIds: List<String>)
}
