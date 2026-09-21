package com.nexus.player.feature.library.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter
import com.nexus.player.feature.library.component.LibraryContextMenuSheet
import com.nexus.player.feature.library.component.LibraryFolderCard
import com.nexus.player.feature.library.component.LibraryFolderListItem
import com.nexus.player.feature.library.component.LibrarySortBottomSheet
import com.nexus.player.feature.library.component.LibraryVideoGridCard
import com.nexus.player.feature.library.component.LibraryVideoListItem
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortOption

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FolderRoute(
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (folderPath: String, folderName: String) -> Unit,
    onNavigateToPlayer: (videoId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    FolderScreen(
        uiState = uiState,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateBack = onNavigateBack,
        onSubfolderClick = onNavigateToFolder,
        onVideoClick = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onVideoLongClick = viewModel::onVideoLongClick,
        onLayoutModeToggle = {
            val nextMode = if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                LibraryLayoutMode.LIST
            } else {
                LibraryLayoutMode.GRID
            }
            viewModel.setLayoutMode(nextMode)
        },
        onSortClick = { viewModel.showSortSheet(true) },
        onSortOptionSelected = { option ->
            viewModel.setSortOption(option)
            viewModel.showSortSheet(false)
        },
        onDismissSortSheet = { viewModel.showSortSheet(false) },
        onDismissContextMenu = { viewModel.dismissContextMenu() },
        onToggleFavorite = viewModel::toggleFavorite,
        onShare = { video -> viewModel.shareVideo(context, video) },
        onRenameConfirm = { video, newName ->
            viewModel.renameVideo(video, newName) { result ->
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
            viewModel.moveVideo(video, targetPath) { result ->
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
            viewModel.copyVideo(video, targetPath) { result ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    uiState: FolderUiState,
    thumbnailLoader: ThumbnailLoader,
    onNavigateBack: () -> Unit,
    onSubfolderClick: (folderPath: String, folderName: String) -> Unit,
    onVideoClick: (videoId: String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    onLayoutModeToggle: () -> Unit,
    onSortClick: () -> Unit,
    onSortOptionSelected: (LibrarySortOption) -> Unit,
    onDismissSortSheet: () -> Unit,
    onDismissContextMenu: () -> Unit,
    onToggleFavorite: (MediaMetadata) -> Unit = {},
    onShare: (MediaMetadata) -> Unit = {},
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit = { _, _ -> },
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onDeleteConfirm: (video: MediaMetadata) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexusTopAppBar(
                title = uiState.folderName,
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
                    // Layout Mode Toggle (Grid <-> List)
                    IconButton(onClick = onLayoutModeToggle) {
                        Icon(
                            imageVector = if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                                Icons.AutoMirrored.Filled.List
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                                "Switch to list layout"
                            } else {
                                "Switch to grid layout"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort Action
                    IconButton(onClick = onSortClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort videos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    NexusLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.isEmpty -> {
                    EmptyFolderView(onNavigateBack = onNavigateBack, modifier = Modifier.align(Alignment.Center))
                }

                uiState.layoutMode == LibraryLayoutMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Subfolders section
                        if (uiState.subfolders.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "Folders (${uiState.subfolders.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                                )
                            }

                            items(
                                items = uiState.subfolders,
                                key = { "folder_${it.folderPath}" }
                            ) { folder ->
                                LibraryFolderCard(
                                    folder = folder,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onSubfolderClick(folder.folderPath, folder.folderName) }
                                )
                            }
                        }

                        // Videos section
                        if (uiState.videos.isNotEmpty()) {
                            if (uiState.subfolders.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "Videos (${uiState.videos.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = spacing.small, bottom = spacing.extraSmall)
                                    )
                                }
                            }

                            items(
                                items = uiState.videos,
                                key = { "video_${it.id}" }
                            ) { video ->
                                LibraryVideoGridCard(
                                    video = video,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onVideoClick(video.id) },
                                    onLongClick = { onVideoLongClick(video) }
                                )
                            }
                        }
                    }
                }

                uiState.layoutMode == LibraryLayoutMode.LIST -> {
                    LazyColumn(
                        contentPadding = PaddingValues(spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Subfolders section
                        if (uiState.subfolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Folders (${uiState.subfolders.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = spacing.extraSmall)
                                )
                            }

                            items(
                                items = uiState.subfolders,
                                key = { "folder_${it.folderPath}" }
                            ) { folder ->
                                LibraryFolderListItem(
                                    folder = folder,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onSubfolderClick(folder.folderPath, folder.folderName) }
                                )
                            }
                        }

                        // Videos section
                        if (uiState.videos.isNotEmpty()) {
                            if (uiState.subfolders.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Videos (${uiState.videos.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = spacing.small, bottom = spacing.extraSmall)
                                    )
                                }
                            }

                            items(
                                items = uiState.videos,
                                key = { "video_${it.id}" }
                            ) { video ->
                                LibraryVideoListItem(
                                    video = video,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onVideoClick(video.id) },
                                    onLongClick = { onVideoLongClick(video) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sort Bottom Sheet
        if (uiState.isSortSheetVisible) {
            LibrarySortBottomSheet(
                currentSortOption = uiState.sortOption,
                onSortOptionSelected = onSortOptionSelected,
                onDismissRequest = onDismissSortSheet
            )
        }

        // Unified Video Context Menu & Dialogs
        var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }

        VideoActionHost(
            video = uiState.selectedVideoForMenu,
            isSheetVisible = uiState.selectedVideoForMenu != null,
            folders = uiState.allFolders,
            onDismissSheet = onDismissContextMenu,
            onPlay = onVideoClick,
            onAddToPlaylist = { video -> videoForAddToPlaylist = video },
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onOpenContainingFolder = onSubfolderClick,
            onRenameConfirm = onRenameConfirm,
            onMoveConfirm = onMoveConfirm,
            onCopyConfirm = onCopyConfirm,
            onDeleteConfirm = onDeleteConfirm
        )

        if (videoForAddToPlaylist != null) {
            AddToPlaylistBottomSheet(
                videoId = videoForAddToPlaylist!!.id,
                videoTitle = videoForAddToPlaylist!!.title,
                onDismissRequest = { videoForAddToPlaylist = null }
            )
        }
    }
}

@Composable
private fun EmptyFolderView(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexusEmptyState(
        icon = Icons.Default.FolderOpen,
        title = "Folder is Empty",
        description = "There are no video files or subfolders inside this location.",
        actionText = "Return to Library",
        onActionClick = onNavigateBack,
        modifier = modifier
    )
}
