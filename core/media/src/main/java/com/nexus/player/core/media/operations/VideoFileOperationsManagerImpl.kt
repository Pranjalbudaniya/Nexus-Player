package com.nexus.player.core.media.operations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoFileOperationsManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : VideoFileOperationsManager {

    private data class StagedDeletion(
        val video: Video,
        val originalPath: String,
        val stagedFile: File,
        val timestamp: Long
    )

    private val stagedDeletions = ConcurrentHashMap<String, StagedDeletion>()

    companion object {
        private val INVALID_CHARS_REGEX = Regex("[\\\\/:*?\"<>|]")
        private const val BUFFER_SIZE = 64 * 1024
        private const val STAGING_EXPIRATION_MS = 60_000L // 1 minute

        private fun isNetworkVideo(videoId: String): Boolean {
            return videoId.startsWith("http://", ignoreCase = true) || videoId.startsWith("https://", ignoreCase = true)
        }
    }

    override suspend fun renameVideo(videoId: String, newName: String): Result<MediaMetadata> =
        withContext(ioDispatcher) {
            if (isNetworkVideo(videoId)) {
                return@withContext Result.failure(
                    UnsupportedOperationException("File operations are not supported for network streams.")
                )
            }
            val cleanName = newName.trim()
            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Filename cannot be blank."))
            }
            if (cleanName.contains(INVALID_CHARS_REGEX)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Filename contains invalid characters (\\ / : * ? \" < > |)")
                )
            }

            val video = videoRepository.getVideoById(videoId)
                ?: return@withContext Result.failure(FileNotFoundException("Video record not found in database."))

            val currentFilePath = video.filePath
            if (currentFilePath.isNullOrBlank()) {
                return@withContext Result.failure(FileNotFoundException("File path is not specified for this video."))
            }

            val currentFile = File(currentFilePath)
            if (!currentFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Video file does not exist on storage at: $currentFilePath"))
            }

            if (!currentFile.canWrite()) {
                return@withContext Result.failure(SecurityException("Cannot rename: file is read-only or permission denied."))
            }

            val originalExt = currentFile.extension
            val targetFileName = if (originalExt.isNotEmpty() && !cleanName.endsWith(".$originalExt", ignoreCase = true)) {
                "$cleanName.$originalExt"
            } else {
                cleanName
            }
            val targetTitle = if (originalExt.isNotEmpty() && cleanName.endsWith(".$originalExt", ignoreCase = true)) {
                cleanName.removeSuffix(".$originalExt")
            } else {
                cleanName
            }

            val targetFile = File(currentFile.parentFile, targetFileName)
            if (targetFile.exists() && targetFile.canonicalPath != currentFile.canonicalPath) {
                return@withContext Result.failure(IllegalStateException("A file named '$targetFileName' already exists in this folder."))
            }

            val success = currentFile.renameTo(targetFile)
            if (!success) {
                return@withContext Result.failure(IOException("Failed to rename file on storage."))
            }

            val updatedVideo = video.copy(
                fileName = targetFile.name,
                title = targetTitle,
                filePath = targetFile.absolutePath,
                mediaUri = Uri.fromFile(targetFile).toString(),
                lastModified = targetFile.lastModified()
            )
            videoRepository.upsertVideo(updatedVideo)
            Result.success(updatedVideo.toMediaMetadata())
        }

    override suspend fun moveVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> =
        withContext(ioDispatcher) {
            if (isNetworkVideo(videoId)) {
                return@withContext Result.failure(
                    UnsupportedOperationException("File operations are not supported for network streams.")
                )
            }
            val targetDir = File(targetDirectoryPath)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return@withContext Result.failure(IOException("Could not create target directory: $targetDirectoryPath"))
            }
            if (!targetDir.isDirectory || !targetDir.canWrite()) {
                return@withContext Result.failure(IllegalArgumentException("Target location is not a writable directory."))
            }

            val video = videoRepository.getVideoById(videoId)
                ?: return@withContext Result.failure(FileNotFoundException("Video record not found in database."))

            val currentFilePath = video.filePath
            if (currentFilePath.isNullOrBlank()) {
                return@withContext Result.failure(FileNotFoundException("File path is not specified for this video."))
            }

            val sourceFile = File(currentFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Video file does not exist on storage at: $currentFilePath"))
            }

            val destFile = File(targetDir, sourceFile.name)
            if (destFile.canonicalPath == sourceFile.canonicalPath) {
                return@withContext Result.success(video.toMediaMetadata())
            }

            if (destFile.exists()) {
                return@withContext Result.failure(IllegalStateException("A file named '${sourceFile.name}' already exists in target destination."))
            }

            // Attempt direct rename, fallback to stream copy + delete for cross-volume
            var moved = sourceFile.renameTo(destFile)
            if (!moved) {
                try {
                    copyStreamCancellable(sourceFile, destFile)
                    if (destFile.length() == sourceFile.length()) {
                        sourceFile.delete()
                        moved = true
                    } else {
                        destFile.delete()
                        return@withContext Result.failure(IOException("Move failed during cross-volume copy: file size mismatch."))
                    }
                } catch (e: Exception) {
                    destFile.delete()
                    return@withContext Result.failure(e)
                }
            }

            val updatedVideo = video.copy(
                filePath = destFile.absolutePath,
                folderPath = targetDir.absolutePath,
                folderName = targetDir.name,
                mediaUri = Uri.fromFile(destFile).toString(),
                lastModified = destFile.lastModified()
            )
            videoRepository.upsertVideo(updatedVideo)
            Result.success(updatedVideo.toMediaMetadata())
        }

    override suspend fun copyVideo(videoId: String, targetDirectoryPath: String): Result<MediaMetadata> =
        withContext(ioDispatcher) {
            if (isNetworkVideo(videoId)) {
                return@withContext Result.failure(
                    UnsupportedOperationException("File operations are not supported for network streams.")
                )
            }
            val targetDir = File(targetDirectoryPath)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return@withContext Result.failure(IOException("Could not create target directory: $targetDirectoryPath"))
            }
            if (!targetDir.isDirectory || !targetDir.canWrite()) {
                return@withContext Result.failure(IllegalArgumentException("Target location is not a writable directory."))
            }

            val video = videoRepository.getVideoById(videoId)
                ?: return@withContext Result.failure(FileNotFoundException("Video record not found in database."))

            val currentFilePath = video.filePath
            if (currentFilePath.isNullOrBlank()) {
                return@withContext Result.failure(FileNotFoundException("File path is not specified for this video."))
            }

            val sourceFile = File(currentFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Video file does not exist on storage at: $currentFilePath"))
            }

            var destFile = File(targetDir, sourceFile.name)
            if (destFile.exists()) {
                val base = sourceFile.nameWithoutExtension
                val ext = sourceFile.extension
                var counter = 1
                while (destFile.exists()) {
                    val candidateName = if (ext.isNotEmpty()) "$base ($counter).$ext" else "$base ($counter)"
                    destFile = File(targetDir, candidateName)
                    counter++
                }
            }

            try {
                copyStreamCancellable(sourceFile, destFile)
            } catch (e: Exception) {
                destFile.delete()
                return@withContext Result.failure(e)
            }

            val newId = UUID.randomUUID().toString()
            val newVideo = video.copy(
                id = newId,
                fileName = destFile.name,
                title = destFile.nameWithoutExtension,
                filePath = destFile.absolutePath,
                folderPath = targetDir.absolutePath,
                folderName = targetDir.name,
                mediaUri = Uri.fromFile(destFile).toString(),
                sizeBytes = destFile.length(),
                dateAdded = System.currentTimeMillis(),
                lastModified = destFile.lastModified(),
                playbackPositionMs = 0L,
                playbackPercentage = 0.0f,
                isFavorite = false,
                watchCount = 0,
                lastPlayedAt = null
            )
            videoRepository.insertVideo(newVideo)
            Result.success(newVideo.toMediaMetadata())
        }

    override suspend fun deleteVideo(videoId: String, stageForUndo: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            if (isNetworkVideo(videoId)) {
                return@withContext Result.failure(
                    UnsupportedOperationException("File operations are not supported for network streams.")
                )
            }
            val video = videoRepository.getVideoById(videoId)
            if (video == null) {
                // Record already removed from database
                return@withContext Result.success(Unit)
            }

            val filePath = video.filePath
            if (!filePath.isNullOrBlank()) {
                val file = File(filePath)
                if (file.exists()) {
                    if (stageForUndo) {
                        val trashDir = File(context.cacheDir, "nexus_trash/$videoId")
                        trashDir.mkdirs()
                        val stagedFile = File(trashDir, file.name)
                        val moved = file.renameTo(stagedFile)
                        if (!moved) {
                            try {
                                copyStreamCancellable(file, stagedFile)
                                file.delete()
                            } catch (e: Exception) {
                                // Fallback: delete directly if staging fails
                                file.delete()
                            }
                        }
                        if (stagedFile.exists()) {
                            stagedDeletions[videoId] = StagedDeletion(
                                video = video,
                                originalPath = file.absolutePath,
                                stagedFile = stagedFile,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    } else {
                        file.delete()
                    }
                }
            }

            videoRepository.deleteVideo(videoId)
            Result.success(Unit)
        }

    override suspend fun restoreDeletedVideo(videoId: String): Result<MediaMetadata> =
        withContext(ioDispatcher) {
            val staged = stagedDeletions.remove(videoId)
                ?: return@withContext Result.failure(IllegalStateException("No staged deletion found for this video."))

            val originalFile = File(staged.originalPath)
            originalFile.parentFile?.mkdirs()

            val restored = staged.stagedFile.renameTo(originalFile)
            if (!restored && staged.stagedFile.exists()) {
                try {
                    staged.stagedFile.inputStream().buffered().use { input ->
                        originalFile.outputStream().buffered().use { output ->
                            input.copyTo(output, bufferSize = BUFFER_SIZE)
                        }
                    }
                    staged.stagedFile.delete()
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
            }
            staged.stagedFile.parentFile?.delete() // clean trash subfolder

            videoRepository.insertVideo(staged.video)
            Result.success(staged.video.toMediaMetadata())
        }

    override suspend fun purgeStagedDeletions(): Unit = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val expiredEntries = stagedDeletions.filter { (now - it.value.timestamp) > STAGING_EXPIRATION_MS }
        expiredEntries.forEach { (id, staged) ->
            if (staged.stagedFile.exists()) {
                staged.stagedFile.delete()
            }
            staged.stagedFile.parentFile?.delete()
            stagedDeletions.remove(id)
        }
    }

    override suspend fun setFavorite(videoId: String, isFavorite: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                videoRepository.setFavorite(videoId, isFavorite)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun getAvailableFolders(): Flow<List<VideoFolder>> = videoRepository.getFolders()

    override fun shareVideo(context: Context, video: MediaMetadata) {
        val file = video.filePath?.let { File(it) }
        val uri = if (file != null && file.exists()) {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                Uri.parse(video.mediaUri)
            }
        } else {
            Uri.parse(video.mediaUri)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share \"${video.title}\"")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    override fun openContainingFolder(
        context: Context,
        folderPath: String,
        folderName: String,
        onNavigateInApp: (folderPath: String, folderName: String) -> Unit
    ) {
        if (folderPath.isNotBlank()) {
            onNavigateInApp(folderPath, folderName)
        }
    }

    private suspend fun copyStreamCancellable(source: File, destination: File) {
        source.inputStream().buffered(BUFFER_SIZE).use { input ->
            destination.outputStream().buffered(BUFFER_SIZE).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    currentCoroutineContext().ensureActive()
                    output.write(buffer, 0, bytes)
                    bytes = input.read(buffer)
                }
                output.flush()
            }
        }
    }
}
