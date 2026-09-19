package com.nexus.player.feature.playlists

import com.nexus.player.core.database.model.Playlist

/**
 * UI state for the Playlists overview screen.
 */
data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isCreateDialogOpen: Boolean = false,
    val playlistToRename: Playlist? = null,
    val playlistToDelete: Playlist? = null,
    val errorMessage: String? = null
) {
    val filteredPlaylists: List<Playlist>
        get() = if (searchQuery.isBlank()) {
            playlists
        } else {
            val query = searchQuery.trim().lowercase()
            playlists.filter {
                it.name.lowercase().contains(query) ||
                    (it.description?.lowercase()?.contains(query) == true)
            }
        }

    val isEmpty: Boolean
        get() = !isLoading && playlists.isEmpty()

    val isSearchEmpty: Boolean
        get() = !isLoading && playlists.isNotEmpty() && filteredPlaylists.isEmpty()
}
