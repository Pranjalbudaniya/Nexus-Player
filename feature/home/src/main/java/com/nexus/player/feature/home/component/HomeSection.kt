package com.nexus.player.feature.home.component

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexus.player.core.ui.component.NexusHorizontalSection

/**
 * Reusable Home Section component encapsulating header, horizontal lazy row,
 * empty state, and loading placeholders.
 */
@Composable
fun <T> HomeSection(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    emptyMessage: String = "No items available",
    itemContent: @Composable (T) -> Unit
) {
    NexusHorizontalSection(
        title = title,
        subtitle = subtitle,
        action = action,
        isLoading = isLoading,
        isEmpty = items.isEmpty(),
        emptyMessage = emptyMessage,
        modifier = modifier
    ) {
        items(
            items = items,
            key = key
        ) { item ->
            itemContent(item)
        }
    }
}
