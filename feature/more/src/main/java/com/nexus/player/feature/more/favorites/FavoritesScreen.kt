package com.nexus.player.feature.more.favorites

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusBadge
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import com.nexus.player.feature.more.MoreViewModel
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet

import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import com.nexus.player.core.ui.component.NexusEmptyState
import com.nexus.player.core.ui.feedback.UserFeedbackFormatter
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FavoritesRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val selectedVideoForMenu by viewModel.selectedVideoForMenu.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }

    FavoritesScreen(
        favoriteVideos = favoriteVideos,
        selectedVideoForMenu = selectedVideoForMenu,
        folders = folders,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateBack = onNavigateBack,
        onNavigateToPlayer = { videoId ->
            viewModel.playFavoriteVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onVideoLongClick = { videoId -> viewModel.onVideoLongClick(videoId) },
        onDismissContextMenu = { viewModel.onDismissContextMenu() },
        onAddToPlaylist = { video -> videoForAddToPlaylist = video },
        onToggleFavorite = { video ->
            viewModel.onToggleFavorite(video) { newFavorite ->
                if (!newFavorite) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Removed \"${video.title}\" from Favorites",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onToggleFavorite(video)
                        }
                    }
                }
            }
        },
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
fun FavoritesScreen(
    favoriteVideos: List<Video>,
    selectedVideoForMenu: MediaMetadata?,
    folders: List<VideoFolder>,
    thumbnailLoader: ThumbnailLoader?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
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
                title = "Favorites",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (favoriteVideos.isEmpty()) {
            NexusEmptyState(
                icon = Icons.Default.FavoriteBorder,
                title = "No Favorites Yet",
                description = "Star your favorite videos from your library for quick access anytime.",
                actionText = "Browse Library",
                onActionClick = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = spacing.medium,
                    end = spacing.medium,
                    top = spacing.small,
                    bottom = spacing.large
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.smallMedium)
            ) {
                items(
                    items = favoriteVideos,
                    key = { it.id }
                ) { video ->
                    FavoriteVideoRow(
                        video = video,
                        thumbnailLoader = thumbnailLoader,
                        onClick = { onNavigateToPlayer(video.id) },
                        onLongClick = { onVideoLongClick(video.id) }
                    )
                }
            }
        }

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteVideoRow(
    video: Video,
    thumbnailLoader: ThumbnailLoader?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button
            ),
        shape = shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.smallMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(shapes.thumbnail)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                var bitmap by remember(video.mediaUri) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(video.mediaUri, thumbnailLoader) {
                    if (thumbnailLoader != null && video.mediaUri.isNotBlank()) {
                        bitmap = thumbnailLoader.loadThumbnail(video.mediaUri, 256, 144)
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    NexusBadge(text = formatDuration(video.durationMs))
                }
            }

            HorizontalSpacer(spacing.medium)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title.ifBlank { video.fileName },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                VerticalSpacer(spacing.extraSmall)

                Text(
                    text = video.folderName.ifBlank { "Videos" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                VerticalSpacer(spacing.extraSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    if (video.resolutionLabel.isNotBlank()) {
                        Text(
                            text = video.resolutionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
