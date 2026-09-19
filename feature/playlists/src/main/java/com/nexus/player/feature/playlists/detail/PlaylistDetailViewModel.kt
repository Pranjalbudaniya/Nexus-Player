package com.nexus.player.feature.playlists.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.database.model.PlaylistItem
import com.nexus.player.core.database.repository.PlaylistRepository
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.QueueSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    val playbackQueueManager: PlaybackQueueManager
) : ViewModel() {

    val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _searchQuery = MutableStateFlow("")
    private val _isRenameDialogOpen = MutableStateFlow(false)
    private val _isDeleteDialogOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // For Undo action
    private var lastRemovedVideoId: String? = null

    private data class DialogState(
        val isRenameOpen: Boolean,
        val isDeleteOpen: Boolean,
        val error: String?
    )

    private val dialogFlow = combine(
        _isRenameDialogOpen,
        _isDeleteDialogOpen,
        _errorMessage
    ) { isRename, isDelete, error ->
        DialogState(isRename, isDelete, error)
    }

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        playlistRepository.observePlaylist(playlistId),
        playlistRepository.observePlaylistVideos(playlistId),
        _searchQuery,
        dialogFlow
    ) { playlist, items, query, dialog ->
        PlaylistDetailUiState(
            playlist = playlist,
            items = items,
            searchQuery = query,
            isLoading = false,
            isRenameDialogOpen = dialog.isRenameOpen,
            isDeleteDialogOpen = dialog.isDeleteOpen,
            errorMessage = dialog.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistDetailUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun playAll(onStartPlayback: (String) -> Unit) {
        val state = uiState.value
        val playable = state.playableVideoIds
        if (playable.isEmpty()) return

        val firstVideoId = playable.first()
        playbackQueueManager.setQueue(
            items = playable,
            initialVideoId = firstVideoId,
            source = QueueSource.Playlist(playlistId, state.playlist?.name ?: "")
        )
        onStartPlayback(firstVideoId)
    }

    fun shufflePlay(onStartPlayback: (String) -> Unit) {
        val state = uiState.value
        val playable = state.playableVideoIds
        if (playable.isEmpty()) return

        val firstVideoId = playable.first()
        playbackQueueManager.setQueue(
            items = playable,
            initialVideoId = firstVideoId,
            source = QueueSource.Playlist(playlistId, state.playlist?.name ?: "")
        )
        playbackQueueManager.setShuffleEnabled(true)
        val activeVideoId = playbackQueueManager.queueState.value.currentVideoId ?: firstVideoId
        onStartPlayback(activeVideoId)
    }

    fun playVideo(videoId: String, onStartPlayback: (String) -> Unit) {
        val state = uiState.value
        val playable = state.playableVideoIds
        if (playable.isEmpty() || !playable.contains(videoId)) return

        playbackQueueManager.setQueue(
            items = playable,
            initialVideoId = videoId,
            source = QueueSource.Playlist(playlistId, state.playlist?.name ?: "")
        )
        onStartPlayback(videoId)
    }

    fun removeVideo(videoId: String) {
        lastRemovedVideoId = videoId
        viewModelScope.launch {
            playlistRepository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    fun undoRemoveVideo() {
        val idToRestore = lastRemovedVideoId ?: return
        viewModelScope.launch {
            playlistRepository.addVideoToPlaylist(playlistId, idToRestore)
            lastRemovedVideoId = null
        }
    }

    fun reorderVideos(orderedVideoIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.reorderVideos(playlistId, orderedVideoIds)
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val items = uiState.value.items.toMutableList()
        if (fromIndex in items.indices && toIndex in items.indices) {
            val moved = items.removeAt(fromIndex)
            items.add(toIndex, moved)
            reorderVideos(items.map { it.videoId })
        }
    }

    fun openRenameDialog() {
        _isRenameDialogOpen.value = true
    }

    fun closeRenameDialog() {
        _isRenameDialogOpen.value = false
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            val result = playlistRepository.renamePlaylist(playlistId, newName)
            result.onSuccess {
                _isRenameDialogOpen.value = false
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to rename playlist"
            }
        }
    }

    fun openDeleteDialog() {
        _isDeleteDialogOpen.value = true
    }

    fun closeDeleteDialog() {
        _isDeleteDialogOpen.value = false
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _isDeleteDialogOpen.value = false
            onDeleted()
        }
    }
}
