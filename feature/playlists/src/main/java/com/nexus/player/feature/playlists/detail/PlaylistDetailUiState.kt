package com.nexus.player.feature.playlists.detail

import com.nexus.player.core.database.model.Playlist
import com.nexus.player.core.database.model.PlaylistItem
import com.nexus.player.core.media.model.MediaMetadata

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val items: List<PlaylistItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRenameDialogOpen: Boolean = false,
    val isDeleteDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val selectedVideoForMenu: MediaMetadata? = null
) {
    val filteredItems: List<PlaylistItem>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            val query = searchQuery.trim().lowercase()
            items.filter { item ->
                val title = item.video?.title?.lowercase() ?: ""
                val fileName = item.video?.fileName?.lowercase() ?: ""
                title.contains(query) || fileName.contains(query)
            }
        }

    val playableVideoIds: List<String>
        get() = filteredItems.filter { it.isAvailable }.map { it.videoId }

    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty()
}
