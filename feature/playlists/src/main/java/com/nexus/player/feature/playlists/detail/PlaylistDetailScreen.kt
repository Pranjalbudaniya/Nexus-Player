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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.playlists.component.DeletePlaylistConfirmationDialog
import com.nexus.player.feature.playlists.component.RenamePlaylistDialog
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    PlaylistDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onPlayAll = { viewModel.playAll(onNavigateToPlayer) },
        onShufflePlay = { viewModel.shufflePlay(onNavigateToPlayer) },
        onPlayVideo = { videoId -> viewModel.playVideo(videoId, onNavigateToPlayer) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailContent(
    uiState: PlaylistDetailUiState,
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
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    val playlist = uiState.playlist
    val items = uiState.filteredItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                            modifier = Modifier.padding(top = 2.dp)
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

            // Content or Empty
            when {
                uiState.isEmpty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No videos in this playlist",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add videos from your library using the long-press menu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            Button(onClick = onNavigateToLibrary) {
                                Text("Browse Library")
                            }
                        }
                    }
                }
                items.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos matching \"${uiState.searchQuery}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
}
