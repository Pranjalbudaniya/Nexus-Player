package com.nexus.player.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexus.player.core.navigation.TopLevelDestination

/**
 * Material 3 Bottom Navigation Bar for Nexus Player.
 *
 * Implements semantic Material 3 container colors, selected/unselected icon hierarchy,
 * accessible labels, and zero hardcoded hex colors.
 */
@Composable
fun NexusNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        content = content
    )
}

/**
 * Top-level destination bottom navigation bar implementation.
 */
@Composable
fun NexusBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination?,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NexusNavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            val isSelected = destination == currentDestination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = "${destination.title} tab"
                }
            )
        }
    }
}
