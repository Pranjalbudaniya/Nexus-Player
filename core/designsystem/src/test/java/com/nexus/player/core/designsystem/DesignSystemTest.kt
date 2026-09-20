package com.nexus.player.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusAccentColor
import com.nexus.player.core.designsystem.theme.NexusColorSchemes
import com.nexus.player.core.designsystem.theme.NexusDimensions
import com.nexus.player.core.designsystem.theme.NexusMotion
import com.nexus.player.core.designsystem.theme.NexusSpacing
import com.nexus.player.core.designsystem.theme.NexusThemeMode
import com.nexus.player.core.designsystem.theme.ThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun spacing_strictlyFollows4dpGrid() {
        val spacing = NexusSpacing()
        val tokens = listOf(
            spacing.none,
            spacing.extraSmall,
            spacing.small,
            spacing.smallMedium,
            spacing.medium,
            spacing.mediumLarge,
            spacing.large,
            spacing.extraLarge,
            spacing.huge,
            spacing.massive,
            spacing.expansive
        )

        for (token in tokens) {
            val value = token.value.toInt()
            assertEquals("Spacing token $value must be a multiple of 4", 0, value % 4)
        }
    }

    @Test
    fun dimensions_minimumTouchTargetCompliesWithAccessibility() {
        val dimensions = NexusDimensions()
        assertTrue(
            "Minimum touch target must be at least 48dp for accessibility compliance",
            dimensions.minTouchTarget >= 48.dp
        )
    }

    @Test
    fun amoledScheme_setsPureBlackCanvasWhilePreservingHierarchy() {
        val amoledScheme = NexusColorSchemes.amoled()
        assertEquals(Color.Black, amoledScheme.background)
        assertEquals(Color.Black, amoledScheme.surface)
        assertEquals(Color.Black, amoledScheme.surfaceContainerLowest)
    }

    @Test
    fun motion_durationsAreProperlyOrderedAndPositive() {
        val motion = NexusMotion()
        assertTrue(motion.durationShort > 0)
        assertTrue(motion.durationMedium > motion.durationShort)
        assertTrue(motion.durationLong > motion.durationMedium)
    }

    @Test
    fun themeConfig_defaultIsSystemWithDynamicColorAndDefaultAccent() {
        val config = ThemeConfig()
        assertEquals(NexusThemeMode.SYSTEM, config.themeMode)
        assertFalse(config.isAmoled)
        assertTrue(config.dynamicColor)
        assertEquals(NexusAccentColor.DEFAULT, config.accentColor)
    }

    @Test
    fun customAccents_generateValidLightAndDarkColorSchemes() {
        for (accent in NexusAccentColor.values()) {
            val lightScheme = NexusColorSchemes.fromAccent(accent, isDark = false, isAmoled = false)
            assertNotNull(lightScheme)
            assertEquals(accent.lightPrimary, lightScheme.primary)
            assertEquals(accent.lightOnPrimary, lightScheme.onPrimary)

            val darkScheme = NexusColorSchemes.fromAccent(accent, isDark = true, isAmoled = false)
            assertNotNull(darkScheme)
            assertEquals(accent.darkPrimary, darkScheme.primary)
            assertEquals(accent.darkOnPrimary, darkScheme.onPrimary)

            val amoledScheme = NexusColorSchemes.fromAccent(accent, isDark = true, isAmoled = true)
            assertNotNull(amoledScheme)
            assertEquals(Color.Black, amoledScheme.background)
            assertEquals(Color.Black, amoledScheme.surface)
            assertEquals(accent.darkPrimary, amoledScheme.primary)
        }
    }
}
