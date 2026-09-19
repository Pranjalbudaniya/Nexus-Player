package com.nexus.player.feature.search

import com.nexus.player.core.media.model.MediaMetadata

/**
 * Layout presentation mode for search results.
 */
enum class SearchLayoutMode {
    GRID,
    LIST
}

/**
 * Reactive UI state model for Global Search screen.
 */
sealed interface SearchUiState {

    val query: String
    val layoutMode: SearchLayoutMode
    val selectedVideoForMenu: MediaMetadata?

    /**
     * Initial state before any query is typed.
     * Shows search tips / suggestions and recent videos for quick access.
     */
    data class Initial(
        override val query: String = "",
        val recentVideos: List<MediaMetadata> = emptyList(),
        override val layoutMode: SearchLayoutMode = SearchLayoutMode.LIST,
        override val selectedVideoForMenu: MediaMetadata? = null
    ) : SearchUiState

    /**
     * Intermediate searching state while debounce window or query is executing.
     */
    data class Searching(
        override val query: String,
        override val layoutMode: SearchLayoutMode = SearchLayoutMode.LIST,
        override val selectedVideoForMenu: MediaMetadata? = null
    ) : SearchUiState

    /**
     * No results found for the active query.
     */
    data class Empty(
        override val query: String,
        val trimmedQuery: String,
        override val layoutMode: SearchLayoutMode = SearchLayoutMode.LIST,
        override val selectedVideoForMenu: MediaMetadata? = null
    ) : SearchUiState

    /**
     * Matches found for active search query.
     */
    data class Success(
        override val query: String,
        val trimmedQuery: String,
        val results: List<MediaMetadata>,
        val resultCount: Int = results.size,
        override val layoutMode: SearchLayoutMode = SearchLayoutMode.LIST,
        override val selectedVideoForMenu: MediaMetadata? = null
    ) : SearchUiState
}
