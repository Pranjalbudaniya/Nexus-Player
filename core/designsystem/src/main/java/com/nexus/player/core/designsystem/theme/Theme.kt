package com.nexus.player.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Supported Theme Modes for Nexus Player.
 */
enum class NexusThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

/**
 * Theme configuration model designed for clean integration with
 * Appearance Settings and persistence without individual screens needing
 * to know how colors, tokens, or schemes are constructed.
 */
@Immutable
data class ThemeConfig(
    val themeMode: NexusThemeMode = NexusThemeMode.SYSTEM,
    val isAmoled: Boolean = false,
    val dynamicColor: Boolean = true,
    val accentColor: NexusAccentColor = NexusAccentColor.DEFAULT
)

/**
 * Nexus Theme Container.
 *
 * Configures Material 3 dynamic theming, AMOLED high-contrast roles, typography,
 * shapes, 4dp grid spacing, accessibility dimensions, metadata typography, and motion.
 *
 * ZERO hardcoded hex color values.
 */
@Composable
fun NexusTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val colorScheme = NexusColorSchemes.forConfig(
        context = context,
        config = themeConfig,
        systemInDark = systemInDark
    )

    val spacing = NexusSpacing()
    val dimensions = NexusDimensions()
    val customShapes = NexusCustomShapes()
    val metadataTypography = NexusMetadataTypography()
    val motion = NexusMotion()

    CompositionLocalProvider(
        LocalNexusSpacing provides spacing,
        LocalNexusDimensions provides dimensions,
        LocalNexusCustomShapes provides customShapes,
        LocalNexusMetadataTypography provides metadataTypography,
        LocalNexusMotion provides motion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexusTypography,
            shapes = NexusShapes,
            content = content
        )
    }
}

/**
 * Convenient overload accepting raw theme parameters.
 */
@Composable
fun NexusTheme(
    themeMode: NexusThemeMode = NexusThemeMode.SYSTEM,
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true,
    accentColor: NexusAccentColor = NexusAccentColor.DEFAULT,
    content: @Composable () -> Unit
) {
    NexusTheme(
        themeConfig = ThemeConfig(
            themeMode = themeMode,
            isAmoled = isAmoled,
            dynamicColor = dynamicColor,
            accentColor = accentColor
        ),
        content = content
    )
}

/**
 * Centralized accessor object for custom Nexus theme tokens.
 */
object NexusTheme {
    val spacing: NexusSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalNexusSpacing.current

    val dimensions: NexusDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalNexusDimensions.current

    val customShapes: NexusCustomShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNexusCustomShapes.current

    val metadataTypography: NexusMetadataTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNexusMetadataTypography.current

    val motion: NexusMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalNexusMotion.current
}
