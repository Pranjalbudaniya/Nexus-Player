package com.nexus.player.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.core.ui.component.contextmenu.VideoActionHost
import com.nexus.player.feature.more.component.ClearHistoryDialog
import com.nexus.player.feature.more.component.HistoryItemRow
import com.nexus.player.feature.playlists.add.AddToPlaylistBottomSheet

@Composable
fun MoreRoute(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onFolderClick: (folderPath: String, folderName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedVideoForMenu by viewModel.selectedVideoForMenu.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    var videoForAddToPlaylist by remember { mutableStateOf<MediaMetadata?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    MoreScreen(
        uiState = uiState,
        selectedVideoForMenu = selectedVideoForMenu,
        folders = folders,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateToPlayer = { videoId ->
            viewModel.playVideo(videoId)
            onNavigateToPlayer(videoId)
        },
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToAnalytics = onNavigateToAnalytics,
        onNavigateToSettings = onNavigateToSettings,
        onClearItem = { videoId -> viewModel.clearHistoryItem(videoId) },
        onClearAllClick = { viewModel.setClearAllDialogOpen(true) },
        onConfirmClearAll = { viewModel.clearAllHistory() },
        onDismissClearAll = { viewModel.setClearAllDialogOpen(false) },
        onVideoLongClick = { videoId -> viewModel.onVideoLongClick(videoId) },
        onDismissContextMenu = { viewModel.onDismissContextMenu() },
        onAddToPlaylist = { video -> videoForAddToPlaylist = video },
        onToggleFavorite = { video -> viewModel.onToggleFavorite(video) },
        onShare = { /* shared through core/ui helper if needed */ },
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
fun MoreScreen(
    uiState: MoreUiState,
    selectedVideoForMenu: MediaMetadata?,
    folders: List<VideoFolder>,
    thumbnailLoader: ThumbnailLoader?,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
                title = "More",
                actions = {
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
        when (uiState) {
            is MoreUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    NexusLoadingIndicator()
                }
            }

            is MoreUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
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
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    // 1. Playback Overview Card
                    item(key = "stats_card") {
                        WatchStatsCard(
                            stats = uiState.stats,
                            onAnalyticsClick = onNavigateToAnalytics
                        )
                    }

                    // 2. Playback History Section Header
                    item(key = "history_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Playback History",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isHistoryEmpty) "No recently watched videos" else "Resume or manage watched videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!uiState.isHistoryEmpty) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onClearAllClick,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = "Clear All History",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    TextButton(onClick = onNavigateToHistory) {
                                        Text("View All")
                                    }
                                }
                            }
                        }
                    }

                    // 3. History Items (Recent 10)
                    if (uiState.isHistoryEmpty) {
                        item(key = "empty_history") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = NexusTheme.customShapes.card,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.large),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    VerticalSpacer(spacing.small)
                                    Text(
                                        text = "No watch history",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    VerticalSpacer(spacing.extraSmall)
                                    Text(
                                        text = "Videos you watch will appear here with your saved position",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.history.take(10),
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

                    // 4. Quick Navigation Hub Section Header
                    item(key = "nav_header") {
                        Text(
                            text = "Quick Navigation",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = spacing.small)
                        )
                    }

                    // 5. Navigation Items
                    item(key = "nav_history") {
                        MoreNavRow(
                            icon = Icons.Default.History,
                            title = "Watch History",
                            subtitle = "Full chronological playback log",
                            onClick = onNavigateToHistory
                        )
                    }

                    item(key = "nav_analytics") {
                        MoreNavRow(
                            icon = Icons.Default.Analytics,
                            title = "Playback Analytics",
                            subtitle = "Watch time, completion rates, and format trends",
                            onClick = onNavigateToAnalytics
                        )
                    }

                    item(key = "nav_settings") {
                        MoreNavRow(
                            icon = Icons.Default.Settings,
                            title = "Settings",
                            subtitle = "Playback, subtitles, decoders, and audio",
                            onClick = onNavigateToSettings
                        )
                    }

                    item(key = "nav_about") {
                        MoreNavRow(
                            icon = Icons.Default.Info,
                            title = "Nexus Player",
                            subtitle = "Local offline media engine • Version 1.0.0",
                            onClick = {}
                        )
                    }
                }

                // Clear All History Confirmation Dialog
                if (uiState.isClearAllDialogOpen) {
                    ClearHistoryDialog(
                        onConfirm = onConfirmClearAll,
                        onDismiss = onDismissClearAll
                    )
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

@Composable
private fun WatchStatsCard(
    stats: WatchStats,
    onAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "View detailed playback analytics",
                onClick = onAnalyticsClick
            ),
        shape = shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Default.History,
                    value = stats.totalWatched.toString(),
                    label = "Watched",
                    iconTint = MaterialTheme.colorScheme.primary
                )

                StatDivider()

                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = stats.completedCount.toString(),
                    label = "Completed",
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                StatDivider()

                StatItem(
                    icon = Icons.Default.Favorite,
                    value = stats.favoritesCount.toString(),
                    label = "Favorites",
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        HorizontalSpacer(spacing.small)
                        Text(
                            text = "View Detailed Watch Analytics",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    val spacing = NexusTheme.spacing
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        VerticalSpacer(spacing.extraSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun MoreNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .clickable(role = Role.Button, onClick = onClick),
        shape = shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = spacing.smallMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shapes.thumbnail)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalSpacer(spacing.medium)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
