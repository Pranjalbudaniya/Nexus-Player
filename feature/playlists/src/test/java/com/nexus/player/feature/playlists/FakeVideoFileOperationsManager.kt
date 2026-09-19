package com.nexus.player.feature.playlists

import android.content.Context
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.operations.VideoFileOperationsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeVideoFileOperationsManager : VideoFileOperationsManager {
    val renamedVideos = mutableListOf<Pair<String, String>>()
    val movedVideos = mutableListOf<Pair<String, String>>()
    val copiedVideos = mutableListOf<Pair<String, String>>()
    val deletedVideos = mutableListOf<String>()
    val restoredVideos = mutableListOf<String>()
    val favoriteVideos = mutableMapOf<String, Boolean>()

    var renameResult: Result<MediaMetadata>? = null
    var moveResult: Result<MediaMetadata>? = null
    var copyResult: Result<MediaMetadata>? = null
    var deleteResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<MediaMetadata>? = null

    private val _foldersFlow = MutableStateFlow<List<VideoFolder>>(emptyList())

    fun setFolders(folders: List<VideoFolder>) {
        _foldersFlow.value = folders
    }

    override suspend fun renameVideo(videoId: String, newName: String): Result<MediaMetadata> {
        renamedVideos.add(videoId to newName)
        return renameResult ?: Result.success(
            dummyMetadata(videoId, newName)
        )
    }

    override suspend fun moveVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> {
        movedVideos.add(videoId to targetDirectoryPath)
        return moveResult ?: Result.success(
            dummyMetadata(videoId, "Moved Video", targetDirectoryPath)
        )
    }

    override suspend fun copyVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> {
        copiedVideos.add(videoId to targetDirectoryPath)
        return copyResult ?: Result.success(
            dummyMetadata("copy-$videoId", "Copied Video", targetDirectoryPath)
        )
    }

    override suspend fun deleteVideo(videoId: String, stageForUndo: Boolean): Result<Unit> {
        deletedVideos.add(videoId)
        return deleteResult
    }

    override suspend fun restoreDeletedVideo(videoId: String): Result<MediaMetadata> {
        restoredVideos.add(videoId)
        return restoreResult ?: Result.success(
            dummyMetadata(videoId, "Restored Video")
        )
    }

    override suspend fun purgeStagedDeletions() {}

    override suspend fun setFavorite(videoId: String, isFavorite: Boolean): Result<Unit> {
        favoriteVideos[videoId] = isFavorite
        return Result.success(Unit)
    }

    override fun getAvailableFolders(): Flow<List<VideoFolder>> = _foldersFlow.asStateFlow()

    override fun shareVideo(context: Context, video: MediaMetadata) {}

    override fun openContainingFolder(
        context: Context,
        folderPath: String,
        folderName: String,
        onNavigateInApp: (folderPath: String, folderName: String) -> Unit
    ) {
        onNavigateInApp(folderPath, folderName)
    }

    private fun dummyMetadata(id: String, title: String, folderPath: String = "/Movies"): MediaMetadata {
        return MediaMetadata(
            id = id,
            mediaUri = "file://$folderPath/$title.mp4",
            filePath = "$folderPath/$title.mp4",
            fileName = "$title.mp4",
            title = title,
            folderName = folderPath.substringAfterLast('/'),
            folderPath = folderPath,
            formattedDuration = "02:00",
            durationMs = 120_000L,
            resolutionLabel = "1080p",
            dimensionsLabel = "1920x1080",
            width = 1920,
            height = 1080,
            videoCodec = "H.264",
            audioCodec = "AAC",
            formattedFps = "30 fps",
            frameRate = 30f,
            formattedBitrate = "2 Mbps",
            videoBitrate = 2_000_000L,
            formattedSize = "50 MB",
            sizeBytes = 50_000_000L,
            formattedModifiedDate = "Today",
            lastModified = System.currentTimeMillis(),
            audioTrackCount = 1,
            subtitleTrackCount = 0,
            isFavorite = favoriteVideos[id] ?: false,
            playbackPositionMs = 0L,
            playbackPercentage = 0f,
            watchCount = 0,
            lastPlayedAt = null
        )
    }
}
