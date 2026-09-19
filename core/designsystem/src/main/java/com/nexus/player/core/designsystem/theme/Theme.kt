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
 * Theme configuration model designed for clean integration with future
 * Appearance Settings and persistence without individual screens needing
 * to know how colors, tokens, or schemes are constructed.
 */
@Immutable
data class ThemeConfig(
    val themeMode: NexusThemeMode = NexusThemeMode.SYSTEM,
    val dynamicColor: Boolean = true
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

    val isDark = when (themeConfig.themeMode) {
        NexusThemeMode.SYSTEM -> systemInDark
        NexusThemeMode.LIGHT -> false
        NexusThemeMode.DARK -> true
        NexusThemeMode.AMOLED -> true
    }

    val isAmoled = themeConfig.themeMode == NexusThemeMode.AMOLED

    val colorScheme = if (themeConfig.dynamicColor) {
        NexusColorSchemes.dynamic(
            context = context,
            isDark = isDark,
            isAmoled = isAmoled
        )
    } else {
        if (isDark) {
            if (isAmoled) NexusColorSchemes.amoled(NexusColorSchemes.dark())
            else NexusColorSchemes.dark()
        } else {
            NexusColorSchemes.light()
        }
    }

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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    NexusTheme(
        themeConfig = ThemeConfig(themeMode = themeMode, dynamicColor = dynamicColor),
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
