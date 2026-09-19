package com.nexus.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nexus.player.core.database.entity.PlaylistEntity
import com.nexus.player.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Summary projection for playlists including calculated video counts.
 */
data class PlaylistSummary(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int
)

/**
 * Data Access Object for playlists and playlist-to-video relationships.
 */
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.description, p.createdAt, p.updatedAt, COUNT(pi.id) AS itemCount
        FROM playlists p
        LEFT JOIN playlist_items pi ON p.id = pi.playlistId
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
    """)
    fun observePlaylistsWithCount(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun observePlaylistById(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    // --- Playlist Items ---

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun observeItemCount(playlistId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getItemCount(playlistId: String): Int

    @Query("SELECT MAX(position) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId LIMIT 1")
    suspend fun getPlaylistItem(playlistId: String, videoId: String): PlaylistItemEntity?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistItems(playlistId: String): List<PlaylistItemEntity>

    @Query("SELECT videoId FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistVideoIds(playlistId: String): List<String>

    @Query("SELECT videoId FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC LIMIT :limit")
    suspend fun getThumbnailVideoIds(playlistId: String, limit: Int = 4): List<String>

    @Transaction
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistItemsWithVideo(playlistId: String): Flow<List<PlaylistItemWithVideo>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistItem(playlistId: String, videoId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItems(playlistId: String)

    @Update
    suspend fun updatePlaylistItems(items: List<PlaylistItemEntity>)

    @Transaction
    suspend fun reorderItems(playlistId: String, orderedVideoIds: List<String>) {
        val currentItems = getPlaylistItems(playlistId).associateBy { it.videoId }
        val updatedList = orderedVideoIds.mapIndexedNotNull { index, videoId ->
            currentItems[videoId]?.copy(position = index)
        }
        if (updatedList.isNotEmpty()) {
            updatePlaylistItems(updatedList)
        }
    }
}
