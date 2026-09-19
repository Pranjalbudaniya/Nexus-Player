package com.nexus.player.feature.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.library.component.LibraryVideoGridCard
import com.nexus.player.feature.library.component.LibraryVideoListItem
import com.nexus.player.feature.search.SearchLayoutMode

/**
 * Initial search view presenting quick suggestion chips and recently indexed videos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchInitialContent(
    recentVideos: List<MediaMetadata>,
    layoutMode: SearchLayoutMode,
    thumbnailLoader: ThumbnailLoader,
    onSuggestionClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val suggestions = listOf("4K", "1080p", "Short", "Downloads", "Camera", "HEVC")

    if (recentVideos.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            VerticalSpacer(spacing.medium)
            Text(
                text = "Search your media library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(spacing.extraSmall)
            Text(
                text = "Find videos by title, folder name, resolution (4K, 1080p), date, or format.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        if (layoutMode == SearchLayoutMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                modifier = modifier.fillMaxSize()
            ) {
                // Header Span: Suggestions & Section Title
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quick Filters",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        VerticalSpacer(spacing.small)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestions.forEach { suggestion ->
                                AssistChip(
                                    onClick = { onSuggestionClick(suggestion) },
                                    label = { Text(suggestion) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        VerticalSpacer(spacing.medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            HorizontalSpacer(spacing.small)
                            Text(
                                text = "Recently Added",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                items(items = recentVideos, key = { it.id }) { video ->
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
                modifier = modifier.fillMaxSize()
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quick Filters",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        VerticalSpacer(spacing.small)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestions.forEach { suggestion ->
                                AssistChip(
                                    onClick = { onSuggestionClick(suggestion) },
                                    label = { Text(suggestion) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                        VerticalSpacer(spacing.medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            HorizontalSpacer(spacing.small)
                            Text(
                                text = "Recently Added",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        VerticalSpacer(spacing.small)
                    }
                }

                items(items = recentVideos, key = { it.id }) { video ->
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
