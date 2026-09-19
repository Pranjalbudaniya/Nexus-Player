package com.nexus.player.core.designsystem.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Nexus Player Motion & Animation Foundation.
 *
 * Implements a restrained, modern, and fluid animation language.
 */
@Immutable
data class NexusMotion(
    // Durations in milliseconds
    val durationShort: Int = 150,     // Micro-interactions, icon state switches, toggle flips
    val durationMedium: Int = 250,    // Card expansions, tab shifts, subtle content fades
    val durationLong: Int = 350,      // Dialog/sheet entrances, screen crossfades

    // Standard Material 3 Easing Curves
    val standardEasing: Easing = FastOutSlowInEasing,
    val decelerateEasing: Easing = LinearOutSlowInEasing,
    val accelerateEasing: Easing = FastOutLinearInEasing
) {
    /**
     * Standard tween animation specs for smooth, consistent motion curves.
     */
    fun <T> shortTween(): TweenSpec<T> = tween(
        durationMillis = durationShort,
        easing = standardEasing
    )

    fun <T> mediumTween(): TweenSpec<T> = tween(
        durationMillis = durationMedium,
        easing = standardEasing
    )

    fun <T> longTween(): TweenSpec<T> = tween(
        durationMillis = durationLong,
        easing = standardEasing
    )
}

val LocalNexusMotion = staticCompositionLocalOf { NexusMotion() }
