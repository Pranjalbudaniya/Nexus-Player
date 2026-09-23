package com.nexus.player.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexus.player.feature.home.component.HomeSection
import com.nexus.player.feature.home.component.HomeThumbnail
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.component.NexusErrorState
import com.nexus.player.core.ui.component.NexusFolderCard
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusRecentVideoCard
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.NexusVideoCard
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedVideoForMenu by viewModel.selectedVideoForMenu.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    HomeScreen(
        uiState = uiState,
        thumbnailLoader = viewModel.thumbnailLoader,
        selectedVideoForMenu = selectedVideoForMenu,
        folders = folders,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToSearch = onNavigateToSearch,
        onVideoClick = { videoId ->
            viewModel.playVideo(videoId)
            onVideoClick(videoId)
        },
        onVideoLongClick = viewModel::onVideoLongClick,
        onDismissContextMenu = viewModel::dismissContextMenu,
        onFolderClick = onFolderClick,
        onToggleFavorite = viewModel::toggleFavorite,
        onShare = { video -> viewModel.shareVideo(context, video) },
        onTriggerRescan = viewModel::triggerRescan,
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
fun HomeScreen(
    uiState: HomeUiState,
    thumbnailLoader: ThumbnailLoader? = null,
    selectedVideoForMenu: MediaMetadata? = null,
    folders: List<VideoFolder> = emptyList(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onVideoLongClick: (String) -> Unit = {},
    onDismissContextMenu: () -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    onToggleFavorite: (MediaMetadata) -> Unit = {},
    onShare: (MediaMetadata) -> Unit = {},
    onTriggerRescan: () -> Unit = {},
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit = { _, _ -> },
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit = { _, _ -> },
    onDeleteConfirm: (video: MediaMetadata) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    var videoForAddToPlaylist by remember { mutableStateOf<Pair<String, String>?>(null) }

    NexusScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NexusTopAppBar(
                title = "Nexus Player",
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.semantics { contentDescription = "Search" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Settings" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Subtle scanning banner when media library is actively being indexed
            val isScanning = when (uiState) {
                is HomeUiState.Success -> uiState.isScanning
                is HomeUiState.Empty -> uiState.isScanning
                else -> false
            }

            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.medium, vertical = spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalSpacer(spacing.smallMedium)
                        Text(
                            text = "Scanning media library...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (uiState) {
                is HomeUiState.Loading -> {
                    NexusLoadingIndicator()
                }

                is HomeUiState.Empty -> {
                    if (uiState.isScanning) {
                        NexusLoadingIndicator(
                            label = "Scanning your device for videos...",
                            description = "Discovered videos will appear here shortly"
                        )
                    } else if (uiState.noAccessibleMedia) {
                        NexusEmptyState(
                            icon = Icons.Default.FolderOpen,
                            title = "No accessible media found",
                            description = "Storage access is required to display your video library",
                            actionText = "Open Settings",
                            actionIcon = Icons.Default.Settings,
                            onActionClick = onNavigateToSettings
                        )
                    } else {
                        NexusEmptyState(
                            icon = Icons.Default.VideoLibrary,
                            title = "No Media Found",
                            description = "Add video files to your device storage or rescan to populate your library.",
                            actionText = "Rescan Media",
                            actionIcon = Icons.Default.Refresh,
                            onActionClick = onTriggerRescan
                        )
                    }
                }

                is HomeUiState.Error -> {
                    NexusErrorState(
                        message = UserFeedbackFormatter.formatScanError(null, uiState.message),
                        actionText = "Retry Scan",
                        onActionClick = onTriggerRescan
                    )
                }

                is HomeUiState.Success -> {
                    val bottomNavPadding = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = spacing.extraSmall,
                            bottom = bottomNavPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        // =========================================================================
                        // Section 1: Continue Watching (First content section below header)
                        // =========================================================================
                        item(key = "section_continue_watching") {
                            HomeSection(
                                title = "Continue Watching",
                                subtitle = "Resume where you left off",
                                items = uiState.continueWatching,
                                key = { it.id },
                                emptyMessage = "No in-progress videos"
                            ) { item ->
                                NexusVideoCard(
                                    title = item.title,
                                    duration = item.duration,
                                    quality = item.quality,
                                    progress = item.progress,
                                    thumbnail = if (thumbnailLoader != null && item.mediaUri.isNotBlank()) {
                                        {
                                            HomeThumbnail(
                                                mediaUri = item.mediaUri,
                                                thumbnailLoader = thumbnailLoader,
                                                contentDescription = "Thumbnail for ${item.title}"
                                            )
                                        }
                                    } else null,
                                    onClick = { onVideoClick(item.id) },
                                    onLongClick = { onVideoLongClick(item.id) }
                                )
                            }
                        }

                        // =========================================================================
                        // Section 2: Recently Added (Three-line presentation)
                        // =========================================================================
                        item(key = "section_recently_added") {
                            HomeSection(
                                title = "Recently Added",
                                subtitle = "Latest video files on device",
                                items = uiState.recentlyAdded,
                                key = { it.id },
                                emptyMessage = "No recently added videos"
                            ) { item ->
                                NexusRecentVideoCard(
                                    title = item.title,
                                    technicalSpecs = item.technicalSpecs,
                                    addedTimeAndFolder = item.addedTimeAndFolder,
                                    duration = item.duration,
                                    quality = item.quality,
                                    thumbnail = if (thumbnailLoader != null && item.mediaUri.isNotBlank()) {
                                        {
                                            HomeThumbnail(
                                                mediaUri = item.mediaUri,
                                                thumbnailLoader = thumbnailLoader,
                                                contentDescription = "Thumbnail for ${item.title}"
                                            )
                                        }
                                    } else null,
                                    onClick = { onVideoClick(item.id) },
                                    onLongClick = { onVideoLongClick(item.id) }
                                )
                            }
                        }

                        // =========================================================================
                        // Section 3: Favorites
                        // =========================================================================
                        item(key = "section_favorites") {
                            HomeSection(
                                title = "Favorites",
                                subtitle = "Your pinned and top-rated videos",
                                items = uiState.favorites,
                                key = { it.id },
                                emptyMessage = "No favorite videos yet"
                            ) { item ->
                                NexusVideoCard(
                                    title = item.title,
                                    duration = item.duration,
                                    quality = item.quality,
                                    thumbnail = if (thumbnailLoader != null && item.mediaUri.isNotBlank()) {
                                        {
                                            HomeThumbnail(
                                                mediaUri = item.mediaUri,
                                                thumbnailLoader = thumbnailLoader,
                                                contentDescription = "Thumbnail for ${item.title}"
                                            )
                                        }
                                    } else null,
                                    onClick = { onVideoClick(item.id) },
                                    onLongClick = { onVideoLongClick(item.id) }
                                )
                            }
                        }

                        // =========================================================================
                        // Section 4: Folders
                        // =========================================================================
                        item(key = "section_folders") {
                            HomeSection(
                                title = "Folders",
                                subtitle = "Organized storage directories",
                                items = uiState.folders,
                                key = { it.id },
                                emptyMessage = "No video folders found"
                            ) { item ->
                                NexusFolderCard(
                                    folderName = item.name,
                                    videoCountText = item.videoCountText,
                                    thumbnail = if (thumbnailLoader != null && !item.previewMediaUri.isNullOrBlank()) {
                                        {
                                            HomeThumbnail(
                                                mediaUri = item.previewMediaUri,
                                                thumbnailLoader = thumbnailLoader,
                                                contentDescription = "Folder ${item.name} preview thumbnail"
                                            )
                                        }
                                    } else null,
                                    onClick = { onFolderClick(item.id, item.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    VideoActionHost(
        video = selectedVideoForMenu,
        isSheetVisible = selectedVideoForMenu != null,
        folders = folders,
        onDismissSheet = onDismissContextMenu,
        onPlay = onVideoClick,
        onAddToPlaylist = { video -> videoForAddToPlaylist = video.id to video.title },
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
            videoId = videoForAddToPlaylist!!.first,
            videoTitle = videoForAddToPlaylist!!.second,
            onDismissRequest = { videoForAddToPlaylist = null }
        )
    }
}
