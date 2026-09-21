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
import androidx.compose.ui.platform.LocalContext
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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusErrorState
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HistoryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        onClearItem = { videoId ->
            viewModel.clearHistoryItem(videoId) { item ->
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Removed \"${item.title}\" from history",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoClearHistoryItem()
                    }
                }
            }
        },
        onClearAllClick = { viewModel.setClearAllDialogOpen(true) },
        onConfirmClearAll = { viewModel.clearAllHistory() },
        onDismissClearAll = { viewModel.setClearAllDialogOpen(false) },
        onVideoLongClick = { videoId -> viewModel.onVideoLongClick(videoId) },
        onDismissContextMenu = { viewModel.onDismissContextMenu() },
        onAddToPlaylist = { video -> videoForAddToPlaylist = video },
        onToggleFavorite = { video -> viewModel.onToggleFavorite(video) },
        onShare = { video -> viewModel.fileOperationsManager.shareVideo(context, video) },
        onOpenContainingFolder = { path, name -> onFolderClick(path, name) },
        onRenameConfirm = { video, newName ->
            viewModel.onRenameConfirm(video, newName) { result ->
                scope.launch {
                    result.onSuccess { renamed ->
                        snackbarHostState.showSnackbar("Renamed to \"${renamed.title}\"")
                    }.onFailure { error ->
                        val msg = UserFeedbackFormatter.formatFileError("Rename", error)
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        },
        onMoveConfirm = { video, targetPath ->
            viewModel.onMoveConfirm(video, targetPath) { result ->
                scope.launch {
                    result.onSuccess {
                        val folderName = File(targetPath).name.ifEmpty { "selected folder" }
                        snackbarHostState.showSnackbar("Moved to $folderName")
                    }.onFailure { error ->
                        val msg = UserFeedbackFormatter.formatFileError("Move", error)
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        },
        onCopyConfirm = { video, targetPath ->
            viewModel.onCopyConfirm(video, targetPath) { result ->
                scope.launch {
                    result.onSuccess {
                        val folderName = File(targetPath).name.ifEmpty { "selected folder" }
                        snackbarHostState.showSnackbar("Copied to $folderName")
                    }.onFailure { error ->
                        val msg = UserFeedbackFormatter.formatFileError("Copy", error)
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        },
        onDeleteConfirm = { video ->
            viewModel.onDeleteConfirm(video) { result ->
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
                                        val restoreMsg = UserFeedbackFormatter.formatFileError("Restore", restoreResult.exceptionOrNull())
                                        snackbarHostState.showSnackbar(restoreMsg.ifBlank { "Failed to restore video" })
                                    }
                                }
                            }
                        } else {
                            viewModel.purgeStagedDeletions()
                        }
                    }.onFailure { error ->
                        val msg = UserFeedbackFormatter.formatFileError("Delete", error)
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        },
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
                    NexusErrorState(
                        message = uiState.message,
                        onActionClick = onNavigateBack,
                        actionText = "Back"
                    )
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
                        NexusEmptyState(
                            icon = Icons.Default.History,
                            title = "No Watch History",
                            description = "Videos you start watching will automatically appear here for easy resumption.",
                            actionText = "Browse Library",
                            onActionClick = onNavigateBack
                        )
                    } else if (filteredHistory.isEmpty()) {
                        NexusEmptyState(
                            icon = Icons.Default.Search,
                            title = "No Matching Videos",
                            description = "No history entry matches \"$searchQuery\".",
                            actionText = "Clear Search",
                            actionIcon = Icons.Default.Clear,
                            onActionClick = { onSearchQueryChange("") }
                        )
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
