package com.nexus.player.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexus.player.core.navigation.TopLevelDestination

/**
 * Material 3 Bottom Navigation Bar for Nexus Player.
 *
 * Implements a solid floating island layout with zero transparency and smooth sliding indicator.
 */
@Composable
fun NexusNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Top-level destination bottom navigation bar implementation.
 * Rendered as an island floating above the bottom edge with a smooth sliding indicator pill.
 */
@Composable
fun NexusBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination?,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = destinations.indexOf(currentDestination).coerceAtLeast(0)

    Surface(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = CircleShape)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val count = destinations.size.coerceAtLeast(1)
            val tabWidth = maxWidth / count

            // Animated sliding indicator pill that moves smoothly from tab to tab
            val animatedIndexFraction by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "nav_sliding_indicator"
            )

            // Sliding indicator background pill
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animatedIndexFraction)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )

            // Navigation tab items
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEachIndexed { index, destination ->
                    val isSelected = destination == currentDestination
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onNavigateToDestination(destination) }
                            )
                            .semantics {
                                contentDescription = "${destination.title} tab"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

