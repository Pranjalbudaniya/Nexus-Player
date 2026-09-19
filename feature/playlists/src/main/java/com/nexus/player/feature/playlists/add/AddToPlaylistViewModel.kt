package com.nexus.player.feature.playlists.add

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

data class AddToPlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val playlistsContainingVideo: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isCreatingNew: Boolean = false,
    val newPlaylistName: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _playlistsContainingVideo = MutableStateFlow<Set<String>>(emptySet())
    private val _isCreatingNew = MutableStateFlow(false)
    private val _newPlaylistName = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    private data class FormState(
        val isCreating: Boolean,
        val newName: String,
        val error: String?,
        val success: String?
    )

    private val formFlow = combine(
        _isCreatingNew,
        _newPlaylistName,
        _errorMessage,
        _successMessage
    ) { isCreating, newName, error, success ->
        FormState(isCreating, newName, error, success)
    }

    val uiState: StateFlow<AddToPlaylistUiState> = combine(
        playlistRepository.observePlaylists(),
        _playlistsContainingVideo,
        formFlow
    ) { playlists, containing, form ->
        AddToPlaylistUiState(
            playlists = playlists,
            playlistsContainingVideo = containing,
            isLoading = false,
            isCreatingNew = form.isCreating,
            newPlaylistName = form.newName,
            errorMessage = form.error,
            successMessage = form.success
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddToPlaylistUiState(isLoading = true)
    )

    fun loadExistingMemberships(videoId: String) {
        viewModelScope.launch {
            val allPlaylists = playlistRepository.getPlaylists()
            val containing = mutableSetOf<String>()
            for (playlist in allPlaylists) {
                val ids = playlistRepository.getPlaylistVideoIds(playlist.id)
                if (ids.contains(videoId)) {
                    containing.add(playlist.id)
                }
            }
            _playlistsContainingVideo.value = containing
        }
    }

    fun addVideoToPlaylist(playlist: Playlist, videoId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = playlistRepository.addVideoToPlaylist(playlist.id, videoId)
            result.onSuccess {
                _successMessage.value = "Added to \"${playlist.name}\""
                _errorMessage.value = null
                _playlistsContainingVideo.value += playlist.id
                onComplete()
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to add video"
            }
        }
    }

    fun openCreateNew() {
        _isCreatingNew.value = true
        _newPlaylistName.value = ""
        _errorMessage.value = null
    }

    fun closeCreateNew() {
        _isCreatingNew.value = false
        _errorMessage.value = null
    }

    fun onNewPlaylistNameChange(name: String) {
        _newPlaylistName.value = name
    }

    fun createAndAddVideo(name: String, videoId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val createResult = playlistRepository.createPlaylist(name)
            createResult.onSuccess { newId ->
                val addResult = playlistRepository.addVideoToPlaylist(newId, videoId)
                if (addResult.isSuccess) {
                    _successMessage.value = "Created \"${name.trim()}\" and added video"
                    _errorMessage.value = null
                    _isCreatingNew.value = false
                    _playlistsContainingVideo.value += newId
                    onComplete()
                } else {
                    _errorMessage.value = addResult.exceptionOrNull()?.message
                }
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to create playlist"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
