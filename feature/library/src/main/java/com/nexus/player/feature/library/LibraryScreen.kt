package com.nexus.player.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter
import com.nexus.player.feature.library.component.FolderSortBottomSheet
import com.nexus.player.feature.library.component.LibraryContextMenuSheet
import com.nexus.player.feature.library.component.LibraryFolderCard
import com.nexus.player.feature.library.component.LibraryFolderListItem
import com.nexus.player.feature.library.component.LibrarySortBottomSheet
import com.nexus.player.feature.library.component.LibraryVideoGridCard
import com.nexus.player.feature.library.component.LibraryVideoListItem
import com.nexus.player.feature.library.preferences.FolderSortOption
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortOption
import com.nexus.player.feature.library.preferences.LibraryTab

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
fun LibraryRoute(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LibraryScreen(
        uiState = uiState,
        thumbnailLoader = viewModel.thumbnailLoader,
        onVideoClick = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onVideoLongClick = viewModel::onVideoLongClick,
        onFolderClick = onFolderClick,
        onTabSelected = viewModel::setSelectedTab,
        onLayoutModeToggle = {
            val nextMode = if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                LibraryLayoutMode.LIST
            } else {
                LibraryLayoutMode.GRID
            }
            viewModel.setLayoutMode(nextMode)
        },
        onSortClick = {
            if (uiState.selectedTab == LibraryTab.VIDEOS) {
                viewModel.showSortSheet(true)
            } else {
                viewModel.showFolderSortSheet(true)
            }
        },
        onRefreshClick = { viewModel.triggerRescan() },
        onSearchClick = onNavigateToSearch,
        onSortOptionSelected = { option ->
            viewModel.setSortOption(option)
            viewModel.showSortSheet(false)
        },
        onFolderSortOptionSelected = { option ->
            viewModel.setFolderSortOption(option)
            viewModel.showFolderSortSheet(false)
        },
        onDismissSortSheet = { viewModel.showSortSheet(false) },
        onDismissFolderSortSheet = { viewModel.showFolderSortSheet(false) },
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
fun LibraryScreen(
    uiState: LibraryUiState,
    thumbnailLoader: ThumbnailLoader,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onLayoutModeToggle: () -> Unit,
    onSortClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSortOptionSelected: (LibrarySortOption) -> Unit,
    onFolderSortOptionSelected: (FolderSortOption) -> Unit,
    onDismissSortSheet: () -> Unit,
    onDismissFolderSortSheet: () -> Unit,
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
                title = when (uiState.selectedTab) {
                    LibraryTab.VIDEOS -> if (uiState.videos.isNotEmpty()) {
                        "Library (${uiState.videos.size})"
                    } else {
                        "Library"
                    }
                    LibraryTab.FOLDERS -> if (uiState.folders.isNotEmpty()) {
                        "Folders (${uiState.folders.size})"
                    } else {
                        "Folders"
                    }
                },
                actions = {
                    // Search Action Entry Point
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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

                    // Sort & Filter Action
                    IconButton(onClick = onSortClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rescan / Refresh Action
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !uiState.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh library",
                            tint = if (uiState.isScanning) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val bottomNavPadding = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Top Library Tab Bar: Videos | Folders
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            val count = if (tab == LibraryTab.VIDEOS) uiState.videos.size else uiState.folders.size
                            Text(
                                text = if (count > 0) "${tab.displayName} ($count)" else tab.displayName,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }

            // Subtle Scanning Indicator
            AnimatedVisibility(
                visible = uiState.isScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
                ) {
                    val progress = (uiState.scanState as? ScanState.Scanning)?.progress
                    val total = progress?.total
                    val progressText = if (progress != null && total != null && total > 0) {
                        "Scanning media... (${progress.current}/$total)"
                    } else {
                        "Scanning media library..."
                    }

                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    VerticalSpacer(spacing.extraSmall)

                    if (progress != null && total != null && total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.current.toFloat() / total },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }

            // Error Banner (if scan failed)
            if (uiState.scanState is ScanState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Scan error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        HorizontalSpacer(spacing.small)
                        val scanError = uiState.scanState as ScanState.Error
                        Text(
                            text = UserFeedbackFormatter.formatScanError(scanError.cause, scanError.message),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        HorizontalSpacer(spacing.small)
                        OutlinedButton(onClick = onRefreshClick) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Storage Access Revoked Warning Banner
            if (!uiState.isStorageAccessGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        HorizontalSpacer(spacing.small)
                        Text(
                            text = "Storage access is currently unavailable.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        HorizontalSpacer(spacing.small)
                        OutlinedButton(onClick = onRefreshClick) {
                            Text("Check Access")
                        }
                    }
                }
            }

            // Main Media Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Initial Loading state
                    uiState.isLoading -> {
                        NexusLoadingIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Empty state for active tab
                    uiState.isEmpty -> {
                        EmptyLibraryView(
                            selectedTab = uiState.selectedTab,
                            onRescanClick = onRefreshClick,
                            isScanning = uiState.isScanning,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Videos Tab Content
                    uiState.selectedTab == LibraryTab.VIDEOS -> {
                        if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(
                                    start = spacing.medium,
                                    end = spacing.medium,
                                    top = spacing.medium,
                                    bottom = bottomNavPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.videos,
                                    key = { it.id }
                                ) { video ->
                                    LibraryVideoGridCard(
                                        video = video,
                                        thumbnailLoader = thumbnailLoader,
                                        onClick = { onVideoClick(video.id) },
                                        onLongClick = { onVideoLongClick(video) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = spacing.medium,
                                    end = spacing.medium,
                                    top = spacing.medium,
                                    bottom = bottomNavPadding
                                ),
                                verticalArrangement = Arrangement.spacedBy(spacing.small),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.videos,
                                    key = { it.id }
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

                    // Folders Tab Content
                    uiState.selectedTab == LibraryTab.FOLDERS -> {
                        if (uiState.layoutMode == LibraryLayoutMode.GRID) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(
                                    start = spacing.medium,
                                    end = spacing.medium,
                                    top = spacing.medium,
                                    bottom = bottomNavPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.folderPath }
                                ) { folder ->
                                    LibraryFolderCard(
                                        folder = folder,
                                        thumbnailLoader = thumbnailLoader,
                                        onClick = { onFolderClick(folder.folderPath, folder.folderName) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = spacing.medium,
                                    end = spacing.medium,
                                    top = spacing.medium,
                                    bottom = bottomNavPadding
                                ),
                                verticalArrangement = Arrangement.spacedBy(spacing.small),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.folders,
                                    key = { it.folderPath }
                                ) { folder ->
                                    LibraryFolderListItem(
                                        folder = folder,
                                        thumbnailLoader = thumbnailLoader,
                                        onClick = { onFolderClick(folder.folderPath, folder.folderName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sort Bottom Sheet for Videos
        if (uiState.isSortSheetVisible) {
            LibrarySortBottomSheet(
                currentSortOption = uiState.sortOption,
                onSortOptionSelected = onSortOptionSelected,
                onDismissRequest = onDismissSortSheet
            )
        }

        // Sort Bottom Sheet for Folders
        if (uiState.isFolderSortSheetVisible) {
            FolderSortBottomSheet(
                currentSortOption = uiState.folderSortOption,
                onSortOptionSelected = onFolderSortOptionSelected,
                onDismissRequest = onDismissFolderSortSheet
            )
        }

        // Unified Video Context Menu & Dialogs
        var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }

        VideoActionHost(
            video = uiState.selectedVideoForMenu,
            isSheetVisible = uiState.selectedVideoForMenu != null,
            folders = uiState.folders,
            onDismissSheet = onDismissContextMenu,
            onPlay = onVideoClick,
            onAddToPlaylist = { video -> videoForAddToPlaylist = video },
            onToggleFavorite = onToggleFavorite,
            onShare = onShare,
            onOpenContainingFolder = onFolderClick,
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

/**
 * Clean streaming-style empty state view for videos or folders.
 */
@Composable
private fun EmptyLibraryView(
    selectedTab: LibraryTab,
    onRescanClick: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    if (isScanning) {
        NexusLoadingIndicator(
            label = if (selectedTab == LibraryTab.VIDEOS) "Searching for videos..." else "Searching for folders...",
            modifier = modifier
        )
    } else {
        val (icon, title, subtitle) = if (selectedTab == LibraryTab.VIDEOS) {
            Triple(
                Icons.Default.VideoLibrary,
                "No Videos Found",
                "We couldn't find any videos on your device. Ensure storage access is granted or add video folders to build your library."
            )
        } else {
            Triple(
                Icons.Default.Folder,
                "No Folders Found",
                "No video folders discovered yet. Ensure storage access is granted or rescan your library."
            )
        }

        NexusEmptyState(
            icon = icon,
            title = title,
            description = subtitle,
            actionText = "Rescan Library",
            actionIcon = Icons.Default.Refresh,
            onActionClick = onRescanClick,
            modifier = modifier
        )
    }
}
