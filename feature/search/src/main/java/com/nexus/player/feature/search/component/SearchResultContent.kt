package com.nexus.player.feature.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.feature.library.component.LibraryVideoGridCard
import com.nexus.player.feature.library.component.LibraryVideoListItem
import com.nexus.player.feature.search.SearchLayoutMode

/**
 * Results content displaying matching videos in either a responsive Grid or List.
 */
@Composable
fun SearchResultContent(
    results: List<MediaMetadata>,
    resultCount: Int,
    layoutMode: SearchLayoutMode,
    thumbnailLoader: ThumbnailLoader,
    onVideoClick: (String) -> Unit,
    onVideoLongClick: (MediaMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    if (layoutMode == SearchLayoutMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (resultCount == 1) "1 video found" else "$resultCount videos found",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = spacing.extraSmall)
                )
            }

            items(items = results, key = { it.id }) { video ->
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
                Text(
                    text = if (resultCount == 1) "1 video found" else "$resultCount videos found",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = spacing.extraSmall)
                )
            }

            items(items = results, key = { it.id }) { video ->
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
