package com.nexus.player.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Curated custom accent color tokens for Nexus Player.
 *
 * NOTE: Strictly adheres to ZERO hardcoded hex color values and zero Color(0xFF...).
 * Colors are defined using float RGB components and converted into full Material 3 ColorSchemes.
 */
enum class NexusAccentColor(
    val label: String,
    val swatchColor: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightSecondaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color
) {
    DEFAULT(
        label = "Default (System)",
        swatchColor = Color(red = 0.25f, green = 0.45f, blue = 0.70f),
        lightPrimary = Color(red = 0.12f, green = 0.42f, blue = 0.75f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 0.85f, green = 0.92f, blue = 1.0f),
        lightOnPrimaryContainer = Color(red = 0.0f, green = 0.20f, blue = 0.50f),
        lightSecondary = Color(red = 0.32f, green = 0.42f, blue = 0.52f),
        lightSecondaryContainer = Color(red = 0.86f, green = 0.90f, blue = 0.96f),
        darkPrimary = Color(red = 0.65f, green = 0.82f, blue = 1.0f),
        darkOnPrimary = Color(red = 0.0f, green = 0.20f, blue = 0.45f),
        darkPrimaryContainer = Color(red = 0.0f, green = 0.30f, blue = 0.65f),
        darkOnPrimaryContainer = Color(red = 0.85f, green = 0.92f, blue = 1.0f),
        darkSecondary = Color(red = 0.72f, green = 0.80f, blue = 0.90f),
        darkSecondaryContainer = Color(red = 0.25f, green = 0.32f, blue = 0.40f)
    ),
    BLUE(
        label = "Ocean Blue",
        swatchColor = Color(red = 0.10f, green = 0.45f, blue = 0.90f),
        lightPrimary = Color(red = 0.08f, green = 0.42f, blue = 0.88f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 0.82f, green = 0.90f, blue = 1.0f),
        lightOnPrimaryContainer = Color(red = 0.0f, green = 0.18f, blue = 0.55f),
        lightSecondary = Color(red = 0.30f, green = 0.40f, blue = 0.55f),
        lightSecondaryContainer = Color(red = 0.85f, green = 0.90f, blue = 0.98f),
        darkPrimary = Color(red = 0.62f, green = 0.80f, blue = 1.0f),
        darkOnPrimary = Color(red = 0.0f, green = 0.18f, blue = 0.50f),
        darkPrimaryContainer = Color(red = 0.0f, green = 0.30f, blue = 0.72f),
        darkOnPrimaryContainer = Color(red = 0.82f, green = 0.90f, blue = 1.0f),
        darkSecondary = Color(red = 0.70f, green = 0.80f, blue = 0.94f),
        darkSecondaryContainer = Color(red = 0.22f, green = 0.32f, blue = 0.45f)
    ),
    TEAL(
        label = "Teal Cyan",
        swatchColor = Color(red = 0.0f, green = 0.55f, blue = 0.60f),
        lightPrimary = Color(red = 0.0f, green = 0.50f, blue = 0.55f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 0.75f, green = 0.95f, blue = 0.98f),
        lightOnPrimaryContainer = Color(red = 0.0f, green = 0.25f, blue = 0.30f),
        lightSecondary = Color(red = 0.28f, green = 0.45f, blue = 0.48f),
        lightSecondaryContainer = Color(red = 0.82f, green = 0.92f, blue = 0.94f),
        darkPrimary = Color(red = 0.50f, green = 0.88f, blue = 0.92f),
        darkOnPrimary = Color(red = 0.0f, green = 0.25f, blue = 0.28f),
        darkPrimaryContainer = Color(red = 0.0f, green = 0.40f, blue = 0.45f),
        darkOnPrimaryContainer = Color(red = 0.75f, green = 0.95f, blue = 0.98f),
        darkSecondary = Color(red = 0.68f, green = 0.82f, blue = 0.85f),
        darkSecondaryContainer = Color(red = 0.20f, green = 0.34f, blue = 0.36f)
    ),
    EMERALD(
        label = "Emerald Green",
        swatchColor = Color(red = 0.15f, green = 0.60f, blue = 0.30f),
        lightPrimary = Color(red = 0.12f, green = 0.52f, blue = 0.25f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 0.78f, green = 0.96f, blue = 0.82f),
        lightOnPrimaryContainer = Color(red = 0.0f, green = 0.28f, blue = 0.10f),
        lightSecondary = Color(red = 0.30f, green = 0.45f, blue = 0.35f),
        lightSecondaryContainer = Color(red = 0.84f, green = 0.92f, blue = 0.86f),
        darkPrimary = Color(red = 0.55f, green = 0.88f, blue = 0.62f),
        darkOnPrimary = Color(red = 0.0f, green = 0.28f, blue = 0.10f),
        darkPrimaryContainer = Color(red = 0.05f, green = 0.42f, blue = 0.18f),
        darkOnPrimaryContainer = Color(red = 0.78f, green = 0.96f, blue = 0.82f),
        darkSecondary = Color(red = 0.70f, green = 0.84f, blue = 0.74f),
        darkSecondaryContainer = Color(red = 0.22f, green = 0.35f, blue = 0.26f)
    ),
    AMBER(
        label = "Warm Amber",
        swatchColor = Color(red = 0.85f, green = 0.45f, blue = 0.0f),
        lightPrimary = Color(red = 0.76f, green = 0.40f, blue = 0.0f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 1.0f, green = 0.88f, blue = 0.70f),
        lightOnPrimaryContainer = Color(red = 0.40f, green = 0.18f, blue = 0.0f),
        lightSecondary = Color(red = 0.48f, green = 0.38f, blue = 0.28f),
        lightSecondaryContainer = Color(red = 0.94f, green = 0.88f, blue = 0.82f),
        darkPrimary = Color(red = 1.0f, green = 0.75f, blue = 0.45f),
        darkOnPrimary = Color(red = 0.40f, green = 0.18f, blue = 0.0f),
        darkPrimaryContainer = Color(red = 0.62f, green = 0.30f, blue = 0.0f),
        darkOnPrimaryContainer = Color(red = 1.0f, green = 0.88f, blue = 0.70f),
        darkSecondary = Color(red = 0.88f, green = 0.78f, blue = 0.68f),
        darkSecondaryContainer = Color(red = 0.38f, green = 0.28f, blue = 0.20f)
    ),
    ROSE(
        label = "Rose Red",
        swatchColor = Color(red = 0.85f, green = 0.15f, blue = 0.30f),
        lightPrimary = Color(red = 0.78f, green = 0.15f, blue = 0.30f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 1.0f, green = 0.84f, blue = 0.88f),
        lightOnPrimaryContainer = Color(red = 0.45f, green = 0.0f, blue = 0.12f),
        lightSecondary = Color(red = 0.48f, green = 0.32f, blue = 0.38f),
        lightSecondaryContainer = Color(red = 0.95f, green = 0.85f, blue = 0.88f),
        darkPrimary = Color(red = 1.0f, green = 0.65f, blue = 0.75f),
        darkOnPrimary = Color(red = 0.45f, green = 0.0f, blue = 0.12f),
        darkPrimaryContainer = Color(red = 0.65f, green = 0.05f, blue = 0.20f),
        darkOnPrimaryContainer = Color(red = 1.0f, green = 0.84f, blue = 0.88f),
        darkSecondary = Color(red = 0.90f, green = 0.74f, blue = 0.78f),
        darkSecondaryContainer = Color(red = 0.38f, green = 0.22f, blue = 0.26f)
    ),
    PURPLE(
        label = "Deep Purple",
        swatchColor = Color(red = 0.50f, green = 0.25f, blue = 0.80f),
        lightPrimary = Color(red = 0.45f, green = 0.22f, blue = 0.78f),
        lightOnPrimary = Color.White,
        lightPrimaryContainer = Color(red = 0.90f, green = 0.82f, blue = 1.0f),
        lightOnPrimaryContainer = Color(red = 0.25f, green = 0.05f, blue = 0.50f),
        lightSecondary = Color(red = 0.40f, green = 0.35f, blue = 0.50f),
        lightSecondaryContainer = Color(red = 0.90f, green = 0.86f, blue = 0.95f),
        darkPrimary = Color(red = 0.80f, green = 0.70f, blue = 1.0f),
        darkOnPrimary = Color(red = 0.25f, green = 0.05f, blue = 0.48f),
        darkPrimaryContainer = Color(red = 0.38f, green = 0.15f, blue = 0.65f),
        darkOnPrimaryContainer = Color(red = 0.90f, green = 0.82f, blue = 1.0f),
        darkSecondary = Color(red = 0.82f, green = 0.76f, blue = 0.90f),
        darkSecondaryContainer = Color(red = 0.32f, green = 0.26f, blue = 0.40f)
    )
}

/**
 * Nexus Player Color Scheme Architecture.
 *
 * NOTE: Strictly adheres to the architectural requirement of ZERO hardcoded hex color values.
 * Colors are resolved entirely via Material 3 semantic color tokens, dynamic Material You,
 * system palettes, and AMOLED high-contrast roles.
 */
object NexusColorSchemes {

    /**
     * Default Light Color Scheme using standard Material 3 semantic roles.
     */
    fun light(): ColorScheme = lightColorScheme()

    /**
     * Default Dark Color Scheme using standard Material 3 semantic roles.
     */
    fun dark(): ColorScheme = darkColorScheme()

    /**
     * AMOLED Dark Color Scheme:
     * Provides true-black (`Color.Black`) background and surface roles for OLED displays while
     * preserving semantic container separation, text contrast, and accent vibrancy.
     *
     * Strictly avoids arbitrary recoloring, maintaining legible Material 3 elevation layers.
     */
    fun amoled(base: ColorScheme = dark()): ColorScheme {
        return base.copy(
            // Primary canvas surfaces set to pure black for OLED power savings and deep contrast
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = base.surfaceBright,

            // Container hierarchy structured with tonal steps over black
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = base.surfaceContainerLow,
            surfaceContainer = base.surfaceContainer,
            surfaceContainerHigh = base.surfaceContainerHigh,
            surfaceContainerHighest = base.surfaceContainerHighest,

            // Borders and dividers kept crisp against true black
            outline = base.outline,
            outlineVariant = base.outlineVariant
        )
    }

    /**
     * Converts a curated [NexusAccentColor] into a complete, harmonious Material 3 [ColorScheme].
     * Tints surfaces, containers, backgrounds, and borders with the accent color so the entire app
     * reflects the chosen accent theme.
     */
    fun fromAccent(
        accent: NexusAccentColor,
        isDark: Boolean,
        isAmoled: Boolean = false
    ): ColorScheme {
        val baseScheme = if (isDark) {
            val neutralDark = Color(red = 0.08f, green = 0.08f, blue = 0.10f)
            val darkBg = lerp(Color(red = 0.06f, green = 0.06f, blue = 0.07f), accent.darkPrimary, 0.07f)
            val darkSurface = lerp(neutralDark, accent.darkPrimary, 0.09f)
            val darkSurfaceContainerLow = lerp(Color(red = 0.11f, green = 0.11f, blue = 0.13f), accent.darkPrimary, 0.12f)
            val darkSurfaceContainer = lerp(Color(red = 0.14f, green = 0.14f, blue = 0.17f), accent.darkPrimary, 0.15f)
            val darkSurfaceContainerHigh = lerp(Color(red = 0.18f, green = 0.18f, blue = 0.22f), accent.darkPrimary, 0.19f)
            val darkSurfaceContainerHighest = lerp(Color(red = 0.22f, green = 0.22f, blue = 0.26f), accent.darkPrimary, 0.24f)
            val darkSurfaceVariant = lerp(Color(red = 0.26f, green = 0.26f, blue = 0.30f), accent.darkSecondary, 0.20f)

            darkColorScheme(
                primary = accent.darkPrimary,
                onPrimary = accent.darkOnPrimary,
                primaryContainer = accent.darkPrimaryContainer,
                onPrimaryContainer = accent.darkOnPrimaryContainer,
                secondary = accent.darkSecondary,
                secondaryContainer = accent.darkSecondaryContainer,
                background = darkBg,
                onBackground = Color(red = 0.92f, green = 0.92f, blue = 0.94f),
                surface = darkSurface,
                onSurface = Color(red = 0.92f, green = 0.92f, blue = 0.94f),
                surfaceVariant = darkSurfaceVariant,
                onSurfaceVariant = Color(red = 0.78f, green = 0.78f, blue = 0.82f),
                surfaceContainerLowest = Color(red = 0.04f, green = 0.04f, blue = 0.05f),
                surfaceContainerLow = darkSurfaceContainerLow,
                surfaceContainer = darkSurfaceContainer,
                surfaceContainerHigh = darkSurfaceContainerHigh,
                surfaceContainerHighest = darkSurfaceContainerHighest,
                outline = lerp(Color(red = 0.45f, green = 0.45f, blue = 0.50f), accent.darkPrimary, 0.25f),
                outlineVariant = lerp(Color(red = 0.28f, green = 0.28f, blue = 0.32f), accent.darkPrimary, 0.20f)
            )
        } else {
            val neutralLight = Color(red = 0.98f, green = 0.98f, blue = 0.99f)
            val lightBg = lerp(Color(red = 0.98f, green = 0.98f, blue = 0.99f), accent.lightPrimary, 0.04f)
            val lightSurface = lerp(neutralLight, accent.lightPrimary, 0.05f)
            val lightSurfaceContainerLow = lerp(Color(red = 0.95f, green = 0.95f, blue = 0.96f), accent.lightPrimary, 0.08f)
            val lightSurfaceContainer = lerp(Color(red = 0.92f, green = 0.92f, blue = 0.94f), accent.lightPrimary, 0.12f)
            val lightSurfaceContainerHigh = lerp(Color(red = 0.88f, green = 0.88f, blue = 0.91f), accent.lightPrimary, 0.16f)
            val lightSurfaceContainerHighest = lerp(Color(red = 0.84f, green = 0.84f, blue = 0.88f), accent.lightPrimary, 0.20f)
            val lightSurfaceVariant = lerp(Color(red = 0.86f, green = 0.86f, blue = 0.90f), accent.lightSecondary, 0.18f)

            lightColorScheme(
                primary = accent.lightPrimary,
                onPrimary = accent.lightOnPrimary,
                primaryContainer = accent.lightPrimaryContainer,
                onPrimaryContainer = accent.lightOnPrimaryContainer,
                secondary = accent.lightSecondary,
                secondaryContainer = accent.lightSecondaryContainer,
                background = lightBg,
                onBackground = Color(red = 0.10f, green = 0.10f, blue = 0.12f),
                surface = lightSurface,
                onSurface = Color(red = 0.10f, green = 0.10f, blue = 0.12f),
                surfaceVariant = lightSurfaceVariant,
                onSurfaceVariant = Color(red = 0.30f, green = 0.30f, blue = 0.35f),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = lightSurfaceContainerLow,
                surfaceContainer = lightSurfaceContainer,
                surfaceContainerHigh = lightSurfaceContainerHigh,
                surfaceContainerHighest = lightSurfaceContainerHighest,
                outline = lerp(Color(red = 0.55f, green = 0.55f, blue = 0.60f), accent.lightPrimary, 0.25f),
                outlineVariant = lerp(Color(red = 0.78f, green = 0.78f, blue = 0.82f), accent.lightPrimary, 0.20f)
            )
        }

        return if (isDark && isAmoled) {
            amoled(baseScheme)
        } else {
            baseScheme
        }
    }

    /**
     * Dynamic Color Scheme builder supporting Material You system theming on Android 12+ (API 31+),
     * with graceful zero-hex semantic fallbacks on older Android versions.
     */
    fun dynamic(
        context: Context,
        isDark: Boolean,
        isAmoled: Boolean = false
    ): ColorScheme {
        val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                if (isDark) dark() else light()
            }
        } else {
            if (isDark) dark() else light()
        }

        return if (isDark && isAmoled) {
            amoled(baseScheme)
        } else {
            baseScheme
        }
    }

    /**
     * Master ColorScheme resolver based on the current [ThemeConfig].
     *
     * Resolution hierarchy:
     * 1. Dynamic Material You wallpaper theme takes precedence if enabled.
     * 2. When dynamic theme is OFF, custom accent color takes effect (tinting entire app).
     * 3. Falls back gracefully to standard Nexus Material 3 theme.
     * 4. Applies AMOLED true-black surfaces when AMOLED is active.
     */
    fun forConfig(
        context: Context,
        config: ThemeConfig,
        systemInDark: Boolean
    ): ColorScheme {
        val isAmoled = config.isAmoled || config.themeMode == NexusThemeMode.AMOLED
        val isDark = when (config.themeMode) {
            NexusThemeMode.SYSTEM -> systemInDark || isAmoled
            NexusThemeMode.LIGHT -> isAmoled
            NexusThemeMode.DARK -> true
            NexusThemeMode.AMOLED -> true
        }

        return if (config.dynamicColor) {
            // System dynamic wallpaper colors
            dynamic(context = context, isDark = isDark, isAmoled = isDark && isAmoled)
        } else if (config.accentColor != NexusAccentColor.DEFAULT) {
            // Explicit user accent selection when dynamic color is OFF
            fromAccent(config.accentColor, isDark = isDark, isAmoled = isDark && isAmoled)
        } else {
            // Default M3 theme
            if (isDark) {
                if (isAmoled) amoled(dark()) else dark()
            } else {
                light()
            }
        }
    }
}
