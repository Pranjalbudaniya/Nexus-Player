package com.nexus.player.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Custom shape tokens tailored for modern media streaming & player applications.
 */
@Immutable
data class NexusCustomShapes(
    val badge: Shape = RoundedCornerShape(4.dp),         // 4dp - Compact metadata tags
    val thumbnail: Shape = RoundedCornerShape(8.dp),     // 8dp - Video poster / list thumbnail
    val card: Shape = RoundedCornerShape(12.dp),         // 12dp - Standard media cards & containers
    val dialog: Shape = RoundedCornerShape(20.dp),       // 20dp - Modal dialogs & floating sheets
    val bottomSheet: Shape = RoundedCornerShape(         // 24dp top corners for bottom sheets
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),
    val pill: Shape = RoundedCornerShape(50)             // 50% - Circular playback badges & action pills
)

val LocalNexusCustomShapes = staticCompositionLocalOf { NexusCustomShapes() }

/**
 * Standard Material 3 Shapes tokens for Nexus Player.
 */
val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
