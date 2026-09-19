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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusFolderCard
import com.nexus.player.core.ui.component.NexusLoadingIndicator
import com.nexus.player.core.ui.component.NexusRecentVideoCard
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.NexusVideoCard
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.home.component.HomeSection
import com.nexus.player.feature.home.component.HomeThumbnail

@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        thumbnailLoader = viewModel.thumbnailLoader,
        onNavigateToSettings = onNavigateToSettings,
        onVideoClick = { videoId ->
            viewModel.playVideo(videoId)
            onVideoClick(videoId)
        },
        onFolderClick = onFolderClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    thumbnailLoader: ThumbnailLoader? = null,
    onNavigateToSettings: () -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onFolderClick: (folderPath: String, folderName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier,
        topBar = {
            NexusTopAppBar(
                title = "Nexus Player",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NexusLoadingIndicator()
                    }
                }

                is HomeUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                VerticalSpacer(spacing.smallMedium)
                                Text(
                                    text = "Scanning your device for videos...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VerticalSpacer(spacing.extraSmall)
                                Text(
                                    text = "Discovered videos will appear here shortly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (uiState.noAccessibleMedia) {
                                Text(
                                    text = "No accessible media found",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VerticalSpacer(spacing.extraSmall)
                                Text(
                                    text = "Storage access is required to display your video library",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No media found on device",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VerticalSpacer(spacing.extraSmall)
                                Text(
                                    text = "Add video files to your device storage to view them here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is HomeUiState.Error -> {
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

                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = spacing.extraSmall),
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
                                    onClick = { onVideoClick(item.id) }
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
                                    onClick = { onVideoClick(item.id) }
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
                                    onClick = { onVideoClick(item.id) }
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
}
