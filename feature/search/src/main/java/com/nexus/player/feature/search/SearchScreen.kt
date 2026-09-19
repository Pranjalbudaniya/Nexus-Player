package com.nexus.player.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.feature.library.component.LibraryContextMenuSheet
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet
import com.nexus.player.feature.search.component.SearchEmptyContent
import com.nexus.player.feature.search.component.SearchInitialContent
import com.nexus.player.feature.search.component.SearchResultContent
import com.nexus.player.feature.search.component.SearchTopBar

/**
 * Global Search Screen for Nexus Player.
 */
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }

    SearchScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onQueryChange = viewModel::onSearchQueryChange,
        onClearQuery = viewModel::clearSearchQuery,
        onToggleLayoutMode = viewModel::toggleLayoutMode,
        onSuggestionClick = { suggestion ->
            viewModel.onSearchQueryChange(suggestion)
        },
        onVideoClick = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onVideoLongClick = viewModel::onVideoLongClick,
        onDismissContextMenu = viewModel::dismissContextMenu,
        onAddToPlaylist = { video ->
            videoForAddToPlaylist = video
        },
        thumbnailLoader = viewModel.thumbnailLoader,
        modifier = modifier
    )

    if (videoForAddToPlaylist != null) {
        AddToPlaylistBottomSheet(
            videoId = videoForAddToPlaylist!!.id,
            videoTitle = videoForAddToPlaylist!!.title,
            onDismissRequest = { videoForAddToPlaylist = null }
        )
    }
}

@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    onDismissContextMenu: () -> Unit,
    onAddToPlaylist: (MediaMetadata) -> Unit,
    thumbnailLoader: com.nexus.player.core.media.thumbnail.ThumbnailLoader,
    modifier: Modifier = Modifier
) {
    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SearchTopBar(
                query = uiState.query,
                layoutMode = uiState.layoutMode,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                onToggleLayoutMode = onToggleLayoutMode,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState) {
                    is SearchUiState.Initial -> {
                        SearchInitialContent(
                            recentVideos = uiState.recentVideos,
                            layoutMode = uiState.layoutMode,
                            thumbnailLoader = thumbnailLoader,
                            onSuggestionClick = onSuggestionClick,
                            onVideoClick = onVideoClick,
                            onVideoLongClick = onVideoLongClick
                        )
                    }
                    is SearchUiState.Searching -> {
                        NexusLoadingIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is SearchUiState.Empty -> {
                        SearchEmptyContent(
                            query = uiState.trimmedQuery,
                            onClearQuery = onClearQuery,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is SearchUiState.Success -> {
                        SearchResultContent(
                            results = uiState.results,
                            resultCount = uiState.resultCount,
                            layoutMode = uiState.layoutMode,
                            thumbnailLoader = thumbnailLoader,
                            onVideoClick = onVideoClick,
                            onVideoLongClick = onVideoLongClick
                        )
                    }
                }
            }
        }

        // Long-Press Context Menu
        val selectedVideo = uiState.selectedVideoForMenu
        if (selectedVideo != null) {
            LibraryContextMenuSheet(
                video = selectedVideo,
                onPlay = onVideoClick,
                onDismissRequest = onDismissContextMenu,
                onAddToPlaylist = onAddToPlaylist
            )
        }
    }
}
