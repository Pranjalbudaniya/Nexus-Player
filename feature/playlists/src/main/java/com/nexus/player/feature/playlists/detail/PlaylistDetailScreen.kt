package com.nexus.player.feature.playlists.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusErrorState
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet
import com.nexus.player.feature.playlists.component.DeletePlaylistConfirmationDialog
import com.nexus.player.feature.playlists.component.RenamePlaylistDialog
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var videoForAddToPlaylist by remember { mutableStateOf<Pair<String, String>?>(null) }

    PlaylistDetailContent(
        uiState = uiState,
        folders = folders,
        onNavigateBack = onNavigateBack,
        onPlayAll = { viewModel.playAll(onNavigateToPlayer) },
        onShufflePlay = { viewModel.shufflePlay(onNavigateToPlayer) },
        onPlayVideo = { videoId -> viewModel.playVideo(videoId, onNavigateToPlayer) },
        onVideoLongClick = viewModel::onVideoLongClick,
        onDismissContextMenu = viewModel::dismissContextMenu,
        onToggleFavorite = viewModel::toggleFavorite,
        onShare = { video -> viewModel.shareVideo(context, video) },
        onOpenContainingFolder = onFolderClick,
        onRenameConfirm = { video, newName ->
            viewModel.renameVideo(video, newName) { result ->
                scope.launch {
                    result.onSuccess { renamed ->
                        snackbarHostState.showSnackbar("Renamed to \"${renamed.title}\"")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(
                            UserFeedbackFormatter.formatFileError("Rename", error)
                        )
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
                        snackbarHostState.showSnackbar(
                            UserFeedbackFormatter.formatFileError("Move", error)
                        )
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
                        snackbarHostState.showSnackbar(
                            UserFeedbackFormatter.formatFileError("Copy", error)
                        )
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
                                        snackbarHostState.showSnackbar(
                                            restoreResult.exceptionOrNull()?.let {
                                                UserFeedbackFormatter.formatFileError("Restore", it)
                                            } ?: "Failed to restore video"
                                        )
                                    }
                                }
                            }
                        } else {
                            viewModel.purgeStagedDeletions()
                        }
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(
                            UserFeedbackFormatter.formatFileError("Delete", error)
                        )
                    }
                }
            }
        },
        onAddToPlaylist = { video ->
            videoForAddToPlaylist = video.id to video.title
        },
        onRemoveVideo = { videoId, title ->
            viewModel.removeVideo(videoId)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Removed \"$title\"",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoRemoveVideo()
                }
            }
        },
        onMoveItem = viewModel::moveItem,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenRenameDialog = viewModel::openRenameDialog,
        onCloseRenameDialog = viewModel::closeRenameDialog,
        onRenamePlaylist = viewModel::renamePlaylist,
        onOpenDeleteDialog = viewModel::openDeleteDialog,
        onCloseDeleteDialog = viewModel::closeDeleteDialog,
        onDeletePlaylist = {
            viewModel.deletePlaylist {
                onNavigateBack()
            }
        },
        onNavigateToLibrary = onNavigateToLibrary,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (videoForAddToPlaylist != null) {
        AddToPlaylistBottomSheet(
            videoId = videoForAddToPlaylist!!.first,
            videoTitle = videoForAddToPlaylist!!.second,
            onDismissRequest = { videoForAddToPlaylist = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailContent(
    uiState: PlaylistDetailUiState,
    folders: List<VideoFolder>,
    onNavigateBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onRemoveVideo: (videoId: String, title: String) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenRenameDialog: () -> Unit,
    onCloseRenameDialog: () -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onOpenDeleteDialog: () -> Unit,
    onCloseDeleteDialog: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    onDismissContextMenu: () -> Unit,
    onToggleFavorite: (MediaMetadata) -> Unit,
    onShare: (MediaMetadata) -> Unit,
    onOpenContainingFolder: (folderPath: String, folderName: String) -> Unit,
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit,
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onDeleteConfirm: (video: MediaMetadata) -> Unit,
    onAddToPlaylist: (MediaMetadata) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    val playlist = uiState.playlist
    val items = uiState.filteredItems

    NexusScaffold(
        topBar = {
            NexusTopAppBar(
                title = playlist?.name ?: "Playlist",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search in playlist",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Playlist options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Playlist") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenRenameDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenDeleteDialog()
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Info & Play / Shuffle Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small)
            ) {
                if (playlist != null) {
                    Text(
                        text = playlist.formattedItemCount,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val description = playlist.description
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = spacing.extraSmall)
                        )
                    }

                    if (uiState.playableVideoIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.medium))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            Button(
                                onClick = onPlayAll,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text("Play All")
                            }

                            FilledTonalButton(
                                onClick = onShufflePlay,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text("Shuffle")
                            }
                        }
                    }
                }
            }

            // Search Bar (if activated)
            if (isSearchVisible) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Filter videos...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = NexusTheme.customShapes.thumbnail,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
                )
            }

            // Content, Loading, Error, or Empty
            when {
                uiState.isLoading -> {
                    NexusLoadingIndicator(label = "Loading playlist...")
                }
                playlist == null -> {
                    NexusErrorState(
                        message = "Playlist not found or has been removed.",
                        actionText = "Go Back",
                        onActionClick = onNavigateBack
                    )
                }
                uiState.isEmpty -> {
                    NexusEmptyState(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "No videos in this playlist",
                        description = "Add videos from your library using the video actions menu.",
                        actionText = "Browse Library",
                        actionIcon = Icons.Default.VideoLibrary,
                        onActionClick = onNavigateToLibrary
                    )
                }
                items.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    NexusEmptyState(
                        icon = Icons.Default.Search,
                        title = "No matching videos",
                        description = "No video in this playlist matches \"${uiState.searchQuery}\".",
                        actionText = "Clear Search",
                        actionIcon = Icons.Default.Clear,
                        onActionClick = { onSearchQueryChange("") }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = spacing.small,
                            bottom = spacing.large
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.videoId }
                        ) { index, item ->
                            PlaylistItemRow(
                                item = item,
                                index = index,
                                totalCount = items.size,
                                onClick = { onPlayVideo(item.videoId) },
                                onLongClick = {
                                    item.video?.toMediaMetadata()?.let(onVideoLongClick)
                                },
                                onRemoveClick = {
                                    onRemoveVideo(item.videoId, item.video?.title ?: item.videoId)
                                },
                                onMoveUpClick = {
                                    if (index > 0) onMoveItem(index, index - 1)
                                },
                                onMoveDownClick = {
                                    if (index < items.size - 1) onMoveItem(index, index + 1)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    RenamePlaylistDialog(
        playlist = if (uiState.isRenameDialogOpen) playlist else null,
        errorMessage = uiState.errorMessage,
        onDismiss = onCloseRenameDialog,
        onConfirm = onRenamePlaylist
    )

    // Delete Confirmation Dialog
    DeletePlaylistConfirmationDialog(
        playlist = if (uiState.isDeleteDialogOpen) playlist else null,
        onDismiss = onCloseDeleteDialog,
        onConfirm = onDeletePlaylist
    )

    // Video Context Menu & Dialogs
    VideoActionHost(
        video = uiState.selectedVideoForMenu,
        isSheetVisible = uiState.selectedVideoForMenu != null,
        folders = folders,
        onDismissSheet = onDismissContextMenu,
        onPlay = onPlayVideo,
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
