package com.nexus.player.feature.search.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexus.player.core.ui.component.NexusEmptyState

/**
 * Empty results view displayed when no indexed videos match the active search query.
 */
@Composable
fun SearchEmptyContent(
    query: String,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexusEmptyState(
        icon = Icons.Default.SearchOff,
        title = "No results for \"$query\"",
        description = "Try checking your spelling, using fewer keywords, or searching by folder (e.g. Movies) or resolution (e.g. 4K, 1080p).",
        actionText = "Clear Search",
        actionIcon = Icons.Default.Clear,
        onActionClick = onClearQuery,
        modifier = modifier
    )
}
