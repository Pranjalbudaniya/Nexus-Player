package com.nexus.player.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
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
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
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

@Composable
fun LibraryRoute(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        Text(
                            text = (uiState.scanState as ScanState.Error).message,
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
                                contentPadding = PaddingValues(spacing.medium),
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
                                contentPadding = PaddingValues(spacing.medium),
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
                                contentPadding = PaddingValues(spacing.medium),
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
                                contentPadding = PaddingValues(spacing.medium),
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

        // Context Menu Sheet on Long-Press
        val selectedVideo = uiState.selectedVideoForMenu
        if (selectedVideo != null) {
            LibraryContextMenuSheet(
                video = selectedVideo,
                onPlay = onVideoClick,
                onDismissRequest = onDismissContextMenu
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
    val spacing = NexusTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (selectedTab == LibraryTab.VIDEOS) Icons.Default.VideoLibrary else Icons.Default.Folder
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )

        VerticalSpacer(spacing.medium)

        val title = when {
            isScanning && selectedTab == LibraryTab.VIDEOS -> "Searching for videos..."
            isScanning && selectedTab == LibraryTab.FOLDERS -> "Searching for folders..."
            selectedTab == LibraryTab.VIDEOS -> "No Videos Found"
            else -> "No Folders Found"
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        VerticalSpacer(spacing.small)

        val subtitle = when {
            isScanning && selectedTab == LibraryTab.VIDEOS ->
                "We are indexing your device's media storage. Videos will appear here automatically."
            isScanning && selectedTab == LibraryTab.FOLDERS ->
                "We are indexing video directories on your device. Folders will appear here automatically."
            selectedTab == LibraryTab.VIDEOS ->
                "We couldn't find any videos on your device. Ensure storage access is granted or add video folders to build your library."
            else ->
                "No video folders discovered yet. Ensure storage access is granted or rescan your library."
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (!isScanning) {
            VerticalSpacer(spacing.large)

            Button(onClick = onRescanClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                HorizontalSpacer(spacing.small)
                Text("Rescan Library")
            }
        }
    }
}
