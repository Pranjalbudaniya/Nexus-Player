package com.nexus.player.feature.more.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import com.nexus.player.feature.more.MoreUiState
import com.nexus.player.feature.more.MoreViewModel
import com.nexus.player.feature.more.component.ClearHistoryDialog
import com.nexus.player.feature.more.component.HistoryItemRow
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet

@Composable
fun HistoryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedVideoForMenu by viewModel.selectedVideoForMenu.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    HistoryScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedVideoForMenu = selectedVideoForMenu,
        folders = folders,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateBack = onNavigateBack,
        onNavigateToPlayer = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onClearItem = { videoId -> viewModel.clearHistoryItem(videoId) },
        onClearAllClick = { viewModel.setClearAllDialogOpen(true) },
        onConfirmClearAll = { viewModel.clearAllHistory() },
        onDismissClearAll = { viewModel.setClearAllDialogOpen(false) },
        onVideoLongClick = { videoId -> viewModel.onVideoLongClick(videoId) },
        onDismissContextMenu = { viewModel.onDismissContextMenu() },
        onAddToPlaylist = { video -> videoForAddToPlaylist = video },
        onToggleFavorite = { video -> viewModel.onToggleFavorite(video) },
        onShare = { },
        onOpenContainingFolder = { path, name -> onFolderClick(path, name) },
        onRenameConfirm = { video, newName -> viewModel.onRenameConfirm(video, newName) },
        onMoveConfirm = { video, targetPath -> viewModel.onMoveConfirm(video, targetPath) },
        onCopyConfirm = { video, targetPath -> viewModel.onCopyConfirm(video, targetPath) },
        onDeleteConfirm = { video -> viewModel.onDeleteConfirm(video) },
        snackbarHostState = snackbarHostState,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: MoreUiState,
    searchQuery: String,
    selectedVideoForMenu: MediaMetadata?,
    folders: List<VideoFolder>,
    thumbnailLoader: ThumbnailLoader?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearItem: (String) -> Unit,
    onClearAllClick: () -> Unit,
    onConfirmClearAll: () -> Unit,
    onDismissClearAll: () -> Unit,
    onVideoLongClick: (String) -> Unit,
    onDismissContextMenu: () -> Unit,
    onAddToPlaylist: (MediaMetadata) -> Unit,
    onToggleFavorite: (MediaMetadata) -> Unit,
    onShare: (MediaMetadata) -> Unit,
    onOpenContainingFolder: (folderPath: String, folderName: String) -> Unit,
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit,
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onDeleteConfirm: (video: MediaMetadata) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexusTopAppBar(
                title = "Watch History",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    val hasHistory = (uiState as? MoreUiState.Success)?.history?.isNotEmpty() == true
                    if (hasHistory) {
                        IconButton(
                            onClick = onClearAllClick,
                            modifier = Modifier.semantics { contentDescription = "Clear All History" }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is MoreUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NexusLoadingIndicator()
                    }
                }

                is MoreUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is MoreUiState.Success -> {
                    val allHistory = uiState.history
                    val filteredHistory = remember(allHistory, searchQuery) {
                        if (searchQuery.isBlank()) {
                            allHistory
                        } else {
                            val query = searchQuery.trim().lowercase()
                            allHistory.filter {
                                it.title.lowercase().contains(query) ||
                                it.folderName.lowercase().contains(query)
                            }
                        }
                    }

                    if (allHistory.isNotEmpty()) {
                        // History Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.medium, vertical = spacing.small),
                            placeholder = {
                                Text(
                                    text = "Search watch history...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = NexusTheme.customShapes.pill,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (allHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                VerticalSpacer(spacing.medium)
                                Text(
                                    text = "No Watch History",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VerticalSpacer(spacing.extraSmall)
                                Text(
                                    text = "Videos you start watching will automatically appear here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (filteredHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                VerticalSpacer(spacing.medium)
                                Text(
                                    text = "No matching videos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VerticalSpacer(spacing.extraSmall)
                                Text(
                                    text = "No history entry matches \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = spacing.medium,
                                end = spacing.medium,
                                top = spacing.extraSmall,
                                bottom = spacing.large
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.smallMedium)
                        ) {
                            items(
                                items = filteredHistory,
                                key = { it.id }
                            ) { item ->
                                HistoryItemRow(
                                    item = item,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onNavigateToPlayer(item.id) },
                                    onLongClick = { onVideoLongClick(item.id) },
                                    onClearClick = { onClearItem(item.id) }
                                )
                            }
                        }
                    }

                    if (uiState.isClearAllDialogOpen) {
                        ClearHistoryDialog(
                            onConfirm = onConfirmClearAll,
                            onDismiss = onDismissClearAll
                        )
                    }
                }
            }
        }

        // Long-Press Context Menu & Dialogs
        VideoActionHost(
            video = selectedVideoForMenu,
            isSheetVisible = selectedVideoForMenu != null,
            folders = folders,
            onDismissSheet = onDismissContextMenu,
            onPlay = onNavigateToPlayer,
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
