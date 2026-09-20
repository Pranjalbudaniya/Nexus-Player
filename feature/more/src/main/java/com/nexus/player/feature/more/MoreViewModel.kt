package com.nexus.player.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.operations.VideoFileOperationsManager
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.QueueSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    val playbackQueueManager: PlaybackQueueManager,
    val thumbnailLoader: ThumbnailLoader,
    val fileOperationsManager: VideoFileOperationsManager,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _isClearAllDialogOpen = MutableStateFlow(false)
    val isClearAllDialogOpen: StateFlow<Boolean> = _isClearAllDialogOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVideoForMenu = MutableStateFlow<MediaMetadata?>(null)
    val selectedVideoForMenu: StateFlow<MediaMetadata?> = _selectedVideoForMenu.asStateFlow()

    val folders: StateFlow<List<VideoFolder>> = fileOperationsManager
        .getAvailableFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val uiState: StateFlow<MoreUiState> = combine(
        videoRepository.getAllHistoryVideos(),
        videoRepository.getFavoriteVideos(),
        _isClearAllDialogOpen
    ) { historyVideos, favoriteVideos, isDialogOpen ->
        val historyItems = historyVideos.map { it.toHistoryItem() }
        val completedCount = historyVideos.count { it.isCompleted || it.playbackPercentage >= 0.95f }
        val stats = WatchStats(
            totalWatched = historyVideos.size,
            completedCount = completedCount,
            favoritesCount = favoriteVideos.size
        )
        MoreUiState.Success(
            history = historyItems,
            stats = stats,
            isClearAllDialogOpen = isDialogOpen
        ) as MoreUiState
    }.catch { error ->
        emit(MoreUiState.Error(error.message ?: "Failed to load watch history"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MoreUiState.Loading
    )

    fun playVideo(videoId: String) {
        val currentState = uiState.value
        if (currentState is MoreUiState.Success && currentState.history.isNotEmpty()) {
            val videoIds = currentState.history.map { it.id }
            playbackQueueManager.setQueue(
                items = videoIds,
                initialVideoId = videoId,
                source = QueueSource.History
            )
        } else {
            playbackQueueManager.setQueue(
                items = listOf(videoId),
                initialVideoId = videoId,
                source = QueueSource.History
            )
        }
    }

    fun clearHistoryItem(videoId: String) {
        viewModelScope.launch(ioDispatcher) {
            videoRepository.clearHistoryForVideo(videoId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(ioDispatcher) {
            videoRepository.clearAllHistory()
            _isClearAllDialogOpen.value = false
        }
    }

    fun setClearAllDialogOpen(open: Boolean) {
        _isClearAllDialogOpen.value = open
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Context Actions & Menu Integration ---

    fun onVideoLongClick(videoId: String) {
        viewModelScope.launch(ioDispatcher) {
            val video = videoRepository.getVideoById(videoId)
            _selectedVideoForMenu.value = video?.toMediaMetadata()
        }
    }

    fun onDismissContextMenu() {
        _selectedVideoForMenu.value = null
    }

    fun onToggleFavorite(metadata: MediaMetadata) {
        viewModelScope.launch(ioDispatcher) {
            val newFavorite = !metadata.isFavorite
            videoRepository.setFavorite(metadata.id, newFavorite)
            _selectedVideoForMenu.value = _selectedVideoForMenu.value?.copy(isFavorite = newFavorite)
        }
    }

    fun onRenameConfirm(metadata: MediaMetadata, newName: String) {
        viewModelScope.launch(ioDispatcher) {
            fileOperationsManager.renameVideo(metadata.id, newName)
            _selectedVideoForMenu.value = null
        }
    }

    fun onMoveConfirm(metadata: MediaMetadata, targetFolderPath: String) {
        viewModelScope.launch(ioDispatcher) {
            fileOperationsManager.moveVideo(metadata.id, targetFolderPath)
            _selectedVideoForMenu.value = null
        }
    }

    fun onCopyConfirm(metadata: MediaMetadata, targetFolderPath: String) {
        viewModelScope.launch(ioDispatcher) {
            fileOperationsManager.copyVideo(metadata.id, targetFolderPath)
            _selectedVideoForMenu.value = null
        }
    }

    fun onDeleteConfirm(metadata: MediaMetadata) {
        viewModelScope.launch(ioDispatcher) {
            fileOperationsManager.deleteVideo(metadata.id)
            _selectedVideoForMenu.value = null
        }
    }

    private fun Video.toHistoryItem(): HistoryItem {
        val now = System.currentTimeMillis()
        val playedAt = lastPlayedAt ?: 0L
        return HistoryItem(
            id = id,
            title = title.ifBlank { fileName },
            duration = formatDuration(durationMs),
            durationMs = durationMs,
            progress = playbackPercentage.coerceIn(0f, 1f),
            playbackPositionMs = playbackPositionMs,
            isCompleted = isCompleted || playbackPercentage >= 0.95f,
            quality = resolutionLabel.ifBlank { "HD" },
            mediaUri = mediaUri,
            folderName = folderName.ifBlank { "Videos" },
            lastPlayedRelative = formatRelativeTime(playedAt, now),
            lastPlayedAt = playedAt,
            watchCount = watchCount
        )
    }

    private fun formatRelativeTime(epochMillis: Long, now: Long): String {
        if (epochMillis <= 0L) return "Unknown"
        val diffMs = now - epochMillis
        if (diffMs < 0L) return "Just now"
        val diffSeconds = diffMs / 1000L
        val diffMinutes = diffSeconds / 60L
        val diffHours = diffMinutes / 60L
        val diffDays = diffHours / 24L

        return when {
            diffMinutes < 1L -> "Just now"
            diffMinutes < 60L -> "${diffMinutes}m ago"
            diffHours < 24L -> "${diffHours}h ago"
            diffDays == 1L -> "Yesterday"
            diffDays < 7L -> "${diffDays}d ago"
            else -> {
                try {
                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMillis))
                } catch (_: Exception) {
                    "Recently"
                }
            }
        }
    }
}
