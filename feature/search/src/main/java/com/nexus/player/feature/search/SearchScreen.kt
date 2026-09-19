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
import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }

    SearchScreenContent(
        uiState = uiState,
        folders = folders,
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
        onToggleFavorite = viewModel::toggleFavorite,
        onShare = { video -> viewModel.shareVideo(context, video) },
        onOpenContainingFolder = onFolderClick,
        onRenameConfirm = { video, newName ->
            viewModel.renameVideo(video, newName) { result ->
                scope.launch {
                    result.onSuccess { renamed ->
                        snackbarHostState.showSnackbar("Renamed to \"${renamed.title}\"")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Rename failed: ${error.message ?: "Unknown error"}")
                    }
                }
            }
        },
        onMoveConfirm = { video, targetPath ->
            viewModel.moveVideo(video, targetPath) { result ->
                scope.launch {
                    result.onSuccess {
                        val folderName = File(targetPath).name.ifEmpty { "selected folder" }
                        snackbarHostState.showSnackbar("Moved to $folderName")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Move failed: ${error.message ?: "Unknown error"}")
                    }
                }
            }
        },
        onCopyConfirm = { video, targetPath ->
            viewModel.copyVideo(video, targetPath) { result ->
                scope.launch {
                    result.onSuccess {
                        val folderName = File(targetPath).name.ifEmpty { "selected folder" }
                        snackbarHostState.showSnackbar("Copied to $folderName")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Copy failed: ${error.message ?: "Unknown error"}")
                    }
                }
            }
        },
        onDeleteConfirm = { video ->
            viewModel.deleteVideo(video) { result ->
                scope.launch {
                    result.onSuccess {
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Deleted \"${video.title}\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            viewModel.restoreDeletedVideo(video.id) { restoreResult ->
                                if (restoreResult.isFailure) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to restore video")
                                    }
                                }
                            }
                        } else {
                            viewModel.purgeStagedDeletions()
                        }
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Delete failed: ${error.message ?: "Unknown error"}")
                    }
                }
            }
        },
        snackbarHostState = snackbarHostState,
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
    folders: List<VideoFolder>,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onToggleLayoutMode: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    onDismissContextMenu: () -> Unit,
    onAddToPlaylist: (MediaMetadata) -> Unit,
    onToggleFavorite: (MediaMetadata) -> Unit = {},
    onShare: (MediaMetadata) -> Unit = {},
    onOpenContainingFolder: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit = { _, _ -> },
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onDeleteConfirm: (video: MediaMetadata) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    thumbnailLoader: com.nexus.player.core.media.thumbnail.ThumbnailLoader,
    modifier: Modifier = Modifier
) {
    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

        // Long-Press Context Menu & Dialogs
        VideoActionHost(
            video = uiState.selectedVideoForMenu,
            isSheetVisible = uiState.selectedVideoForMenu != null,
            folders = folders,
            onDismissSheet = onDismissContextMenu,
            onPlay = onVideoClick,
            onAddToPlaylist = onAddToPlaylist,
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onOpenContainingFolder = onOpenContainingFolder,
            onRenameConfirm = onRenameConfirm,
            onMoveConfirm = onMoveConfirm,
            onCopyConfirm = onCopyConfirm,
            onDeleteConfirm = onDeleteConfirm
        )
    }
}
