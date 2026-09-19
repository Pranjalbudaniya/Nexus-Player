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
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.library.component.LibraryContextMenuSheet
import com.nexus.player.feature.library.component.LibraryFolderCard
import com.nexus.player.feature.library.component.LibraryFolderListItem
import com.nexus.player.feature.library.component.LibrarySortBottomSheet
import com.nexus.player.feature.library.component.LibraryVideoGridCard
import com.nexus.player.feature.library.component.LibraryVideoListItem
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibrarySortOption

@Composable
fun FolderRoute(
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (folderPath: String, folderName: String) -> Unit,
    onNavigateToPlayer: (videoId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier,
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
                    EmptyFolderView(modifier = Modifier.align(Alignment.Center))
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

        // Context Menu Sheet on Long-Press
        var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }
        val selectedVideo = uiState.selectedVideoForMenu
        if (selectedVideo != null) {
            LibraryContextMenuSheet(
                video = selectedVideo,
                onPlay = onVideoClick,
                onDismissRequest = onDismissContextMenu,
                onAddToPlaylist = { video ->
                    videoForAddToPlaylist = video
                }
            )
        }

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
private fun EmptyFolderView(modifier: Modifier = Modifier) {
    val spacing = NexusTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )

        VerticalSpacer(spacing.medium)

        Text(
            text = "Folder is Empty",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        VerticalSpacer(spacing.small)

        Text(
            text = "There are no video files or subfolders inside this location.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
