package com.nexus.player.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

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
}
