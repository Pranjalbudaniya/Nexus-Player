package com.nexus.player.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nexus Player 4dp spacing grid system.
 *
 * Strictly adheres to multiples of 4dp to guarantee rhythmic, predictable layouts
 * across all screens and components.
 */
@Immutable
data class NexusSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,       // 4dp  - Micro gaps, inline badge spacing, icon borders
    val small: Dp = 8.dp,            // 8dp  - Chip padding, compact list margins, metadata item gaps
    val smallMedium: Dp = 12.dp,     // 12dp - Dense card content gaps, internal form padding
    val medium: Dp = 16.dp,          // 16dp - Standard screen padding, card margins, list item gutters
    val mediumLarge: Dp = 20.dp,     // 20dp - Intermediate section margins
    val large: Dp = 24.dp,           // 24dp - Major component breaks, dialog insets
    val extraLarge: Dp = 32.dp,      // 32dp - Screen section dividers, header top spacing
    val huge: Dp = 40.dp,            // 40dp - Prominent visual separators
    val massive: Dp = 48.dp,         // 48dp - Hero media offsets, player overlay margins
    val expansive: Dp = 64.dp        // 64dp - Spacious landing layouts, splash offsets
)

/**
 * Nexus Player layout dimensions and accessibility touch target tokens.
 */
@Immutable
data class NexusDimensions(
    // Accessibility: Minimum recommended touch target size for touch interaction
    val minTouchTarget: Dp = 48.dp,

    // Standard Icon Sizes (following 4dp increments)
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconExtraLarge: Dp = 48.dp,

    // Component Dimensions
    val badgeHeight: Dp = 20.dp,
    val buttonHeight: Dp = 48.dp,
    val compactButtonHeight: Dp = 36.dp,
    val cardMinHeight: Dp = 64.dp,
    val topAppBarHeight: Dp = 64.dp,

    // Standard Elevations
    val elevationNone: Dp = 0.dp,
    val elevationLow: Dp = 2.dp,
    val elevationMedium: Dp = 4.dp,
    val elevationHigh: Dp = 8.dp
)

val LocalNexusSpacing = staticCompositionLocalOf { NexusSpacing() }
val LocalNexusDimensions = staticCompositionLocalOf { NexusDimensions() }
