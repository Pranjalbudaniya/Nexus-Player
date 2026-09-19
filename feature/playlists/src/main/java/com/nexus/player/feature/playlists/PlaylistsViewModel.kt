package com.nexus.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.database.model.Playlist
import com.nexus.player.core.database.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isCreateDialogOpen = MutableStateFlow(false)
    private val _playlistToRename = MutableStateFlow<Playlist?>(null)
    private val _playlistToDelete = MutableStateFlow<Playlist?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private data class DialogState(
        val isCreateOpen: Boolean,
        val renameTarget: Playlist?,
        val deleteTarget: Playlist?,
        val error: String?
    )

    private val dialogFlow = combine(
        _isCreateDialogOpen,
        _playlistToRename,
        _playlistToDelete,
        _errorMessage
    ) { isCreateOpen, renameTarget, deleteTarget, error ->
        DialogState(isCreateOpen, renameTarget, deleteTarget, error)
    }

    val uiState: StateFlow<PlaylistsUiState> = combine(
        playlistRepository.observePlaylists(),
        _searchQuery,
        dialogFlow
    ) { playlists, query, dialog ->
        PlaylistsUiState(
            playlists = playlists,
            searchQuery = query,
            isLoading = false,
            isCreateDialogOpen = dialog.isCreateOpen,
            playlistToRename = dialog.renameTarget,
            playlistToDelete = dialog.deleteTarget,
            errorMessage = dialog.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistsUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openCreateDialog() {
        _errorMessage.value = null
        _isCreateDialogOpen.value = true
    }

    fun closeCreateDialog() {
        _errorMessage.value = null
        _isCreateDialogOpen.value = false
    }

    fun createPlaylist(name: String, description: String?, onSuccess: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = playlistRepository.createPlaylist(name, description)
            result.onSuccess { newId ->
                _errorMessage.value = null
                _isCreateDialogOpen.value = false
                onSuccess?.invoke(newId)
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to create playlist"
            }
        }
    }

    fun openRenameDialog(playlist: Playlist) {
        _errorMessage.value = null
        _playlistToRename.value = playlist
    }

    fun closeRenameDialog() {
        _errorMessage.value = null
        _playlistToRename.value = null
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            val result = playlistRepository.renamePlaylist(playlistId, newName)
            result.onSuccess {
                _errorMessage.value = null
                _playlistToRename.value = null
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to rename playlist"
            }
        }
    }

    fun openDeleteConfirmation(playlist: Playlist) {
        _playlistToDelete.value = playlist
    }

    fun closeDeleteConfirmation() {
        _playlistToDelete.value = null
    }

    fun confirmDeletePlaylist() {
        val target = _playlistToDelete.value ?: return
        viewModelScope.launch {
            playlistRepository.deletePlaylist(target.id)
            _playlistToDelete.value = null
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
