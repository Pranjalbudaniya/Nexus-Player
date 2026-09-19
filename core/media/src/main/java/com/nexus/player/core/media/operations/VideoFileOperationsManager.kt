package com.nexus.player.core.media.operations

import android.content.Context
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata
import kotlinx.coroutines.flow.Flow

/**
 * High-level manager responsible for atomic, safe filesystem and Room database operations
 * on video media items.
 */
interface VideoFileOperationsManager {

    /**
     * Renames a video file on disk and updates its corresponding Room database record.
     *
     * Validations:
     * - Rejects empty or whitespace-only names.
     * - Rejects invalid filesystem characters: \ / : * ? " < > |
     * - Preserves original file extension if missing from [newName].
     * - Checks for collisions with existing files in the containing folder.
     *
     * @param videoId The stable ID of the video to rename.
     * @param newName The desired new display name or filename.
     * @return [Result] containing updated [MediaMetadata] on success, or failure with error cause.
     */
    suspend fun renameVideo(videoId: String, newName: String): Result<MediaMetadata>

    /**
     * Moves a video file to [targetDirectoryPath] and updates Room database state.
     *
     * Maintains the stable video ID to preserve existing playlist references.
     *
     * @param videoId The stable ID of the video to move.
     * @param targetDirectoryPath Absolute path to the destination directory.
     * @return [Result] containing updated [MediaMetadata] on success, or failure.
     */
    suspend fun moveVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata>

    /**
     * Copies a video file to [targetDirectoryPath] and inserts a new Room record with a fresh ID.
     *
     * Handles name collisions gracefully by appending numeric counters if needed.
     *
     * @param videoId The stable ID of the video to copy.
     * @param targetDirectoryPath Absolute path to the destination directory.
     * @return [Result] containing the new [MediaMetadata] on success, or failure.
     */
    suspend fun copyVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata>

    /**
     * Deletes a video file from disk and removes its Room database entry.
     *
     * If [stageForUndo] is true, moves the physical file to a temporary staging cache
     * so it can be restored via [restoreDeletedVideo] within the undo window.
     *
     * @param videoId The stable ID of the video to delete.
     * @param stageForUndo Whether to preserve the file in the undo cache.
     * @return [Result] indicating success or failure.
     */
    suspend fun deleteVideo(videoId: String, stageForUndo: Boolean = true): Result<Unit>

    /**
     * Restores a recently deleted video from the staging cache back to its original location
     * and re-inserts its Room database record.
     *
     * @param videoId The stable ID of the deleted video.
     * @return [Result] containing the restored [MediaMetadata] on success, or failure.
     */
    suspend fun restoreDeletedVideo(videoId: String): Result<MediaMetadata>

    /**
     * Purges expired staged deletions permanently from the filesystem cache.
     */
    suspend fun purgeStagedDeletions()

    /**
     * Toggles or updates the favorite state for a video.
     */
    suspend fun setFavorite(videoId: String, isFavorite: Boolean): Result<Unit>

    /**
     * Observes the list of available video folders.
     */
    fun getAvailableFolders(): Flow<List<VideoFolder>>

    /**
     * Launches Android's native share sheet for the specified video.
     */
    fun shareVideo(context: Context, video: MediaMetadata)

    /**
     * Opens the containing folder of a video. Prefers in-app navigation callback [onNavigateInApp],
     * with graceful fallback to system file managers.
     */
    fun openContainingFolder(
        context: Context,
        folderPath: String,
        folderName: String,
        onNavigateInApp: (folderPath: String, folderName: String) -> Unit
    )
}
