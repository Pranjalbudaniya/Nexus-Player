package com.nexus.player.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.playlists.component.CreatePlaylistDialog
import com.nexus.player.feature.playlists.component.DeletePlaylistConfirmationDialog
import com.nexus.player.feature.playlists.component.PlaylistCard
import com.nexus.player.feature.playlists.component.RenamePlaylistDialog

@Composable
fun PlaylistsScreen(
    onNavigateToPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    PlaylistsContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenCreateDialog = viewModel::openCreateDialog,
        onCloseCreateDialog = viewModel::closeCreateDialog,
        onCreatePlaylist = { name, desc ->
            viewModel.createPlaylist(name, desc) { newId ->
                onNavigateToPlaylist(newId)
            }
        },
        onPlaylistClick = onNavigateToPlaylist,
        onOpenRenameDialog = viewModel::openRenameDialog,
        onCloseRenameDialog = viewModel::closeRenameDialog,
        onRenamePlaylist = viewModel::renamePlaylist,
        onOpenDeleteDialog = viewModel::openDeleteConfirmation,
        onCloseDeleteDialog = viewModel::closeDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDeletePlaylist,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsContent(
    uiState: PlaylistsUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onCloseCreateDialog: () -> Unit,
    onCreatePlaylist: (name: String, description: String?) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onOpenRenameDialog: (com.nexus.player.core.database.model.Playlist) -> Unit,
    onCloseRenameDialog: () -> Unit,
    onRenamePlaylist: (playlistId: String, newName: String) -> Unit,
    onOpenDeleteDialog: (com.nexus.player.core.database.model.Playlist) -> Unit,
    onCloseDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCreateDialog) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreateDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Create Playlist"
                )
            }
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
            // Search field when playlists exist
            if (uiState.playlists.isNotEmpty()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search playlists...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
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

            when {
                uiState.isEmpty -> {
                    // Empty State
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
                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No playlists yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Create custom playlists to organize and queue your favorite videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            Button(onClick = onOpenCreateDialog) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(spacing.small))
                                Text("Create Playlist")
                            }
                        }
                    }
                }
                uiState.isSearchEmpty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No playlists matching \"${uiState.searchQuery}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Playlists List
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = spacing.medium,
                            end = spacing.medium,
                            top = spacing.small,
                            bottom = 88.dp // Space for FAB and bottom nav
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.smallMedium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.filteredPlaylists,
                            key = { it.id }
                        ) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.id) },
                                onRenameClick = { onOpenRenameDialog(playlist) },
                                onDeleteClick = { onOpenDeleteDialog(playlist) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    CreatePlaylistDialog(
        isOpen = uiState.isCreateDialogOpen,
        errorMessage = uiState.errorMessage,
        onDismiss = onCloseCreateDialog,
        onConfirm = onCreatePlaylist
    )

    RenamePlaylistDialog(
        playlist = uiState.playlistToRename,
        errorMessage = uiState.errorMessage,
        onDismiss = onCloseRenameDialog,
        onConfirm = { newName ->
            uiState.playlistToRename?.let { onRenamePlaylist(it.id, newName) }
        }
    )

    DeletePlaylistConfirmationDialog(
        playlist = uiState.playlistToDelete,
        onDismiss = onCloseDeleteDialog,
        onConfirm = onConfirmDelete
    )
}
