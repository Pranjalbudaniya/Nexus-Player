package com.nexus.player.core.common.settings.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleSettingsModelsTest {

    @Test
    fun subtitleTextSize_fromName_resolvesCorrectly() {
        assertEquals(SubtitleTextSize.Small, SubtitleTextSize.fromName("small"))
        assertEquals(SubtitleTextSize.Normal, SubtitleTextSize.fromName("normal"))
        assertEquals(SubtitleTextSize.Normal, SubtitleTextSize.fromName("medium"))
        assertEquals(SubtitleTextSize.Normal, SubtitleTextSize.fromName("Medium"))
        assertEquals(SubtitleTextSize.Large, SubtitleTextSize.fromName("large"))
        assertEquals(SubtitleTextSize.ExtraLarge, SubtitleTextSize.fromName("extralarge"))
        assertEquals(SubtitleTextSize.ExtraLarge, SubtitleTextSize.fromName("extra large"))
        assertEquals(SubtitleTextSize.Normal, SubtitleTextSize.fromName("unknown"))
        assertEquals(SubtitleTextSize.Normal, SubtitleTextSize.fromName(null))
        assertEquals("Medium", SubtitleTextSize.Normal.label)
    }

    @Test
    fun subtitleTextColor_fromName_resolvesCorrectly() {
        assertEquals(SubtitleTextColor.White, SubtitleTextColor.fromName("white"))
        assertEquals(SubtitleTextColor.Yellow, SubtitleTextColor.fromName("yellow"))
        assertEquals(SubtitleTextColor.Cyan, SubtitleTextColor.fromName("cyan"))
        assertEquals(SubtitleTextColor.LightGreen, SubtitleTextColor.fromName("green"))
        assertEquals(SubtitleTextColor.LightGreen, SubtitleTextColor.fromName("lightgreen"))
        assertEquals(SubtitleTextColor.White, SubtitleTextColor.fromName("invalid"))
        assertEquals(SubtitleTextColor.White, SubtitleTextColor.fromName(null))
    }

    @Test
    fun subtitleBackgroundStyle_fromName_resolvesCorrectly() {
        assertEquals(SubtitleBackgroundStyle.None, SubtitleBackgroundStyle.fromName("none"))
        assertEquals(SubtitleBackgroundStyle.DropShadow, SubtitleBackgroundStyle.fromName("dropshadow"))
        assertEquals(SubtitleBackgroundStyle.DropShadow, SubtitleBackgroundStyle.fromName("shadow"))
        assertEquals(SubtitleBackgroundStyle.Outline, SubtitleBackgroundStyle.fromName("outline"))
        assertEquals(SubtitleBackgroundStyle.Box, SubtitleBackgroundStyle.fromName("box"))
        assertEquals(SubtitleBackgroundStyle.Box, SubtitleBackgroundStyle.fromName("unknown"))
        assertEquals(SubtitleBackgroundStyle.Box, SubtitleBackgroundStyle.fromName(null))
    }

    @Test
    fun subtitlePosition_fromName_resolvesCorrectly() {
        assertEquals(SubtitlePosition.Bottom, SubtitlePosition.fromName("bottom"))
        assertEquals(SubtitlePosition.Raised, SubtitlePosition.fromName("raised"))
        assertEquals(SubtitlePosition.Top, SubtitlePosition.fromName("top"))
        assertEquals(SubtitlePosition.Bottom, SubtitlePosition.fromName("invalid"))
        assertEquals(SubtitlePosition.Bottom, SubtitlePosition.fromName(null))
    }

    @Test
    fun defaultSubtitleTrackBehavior_fromName_resolvesCorrectly() {
        assertEquals(DefaultSubtitleTrackBehavior.AUTO, DefaultSubtitleTrackBehavior.fromName("auto"))
        assertEquals(DefaultSubtitleTrackBehavior.FORCED_ONLY, DefaultSubtitleTrackBehavior.fromName("forced_only"))
        assertEquals(DefaultSubtitleTrackBehavior.FORCED_ONLY, DefaultSubtitleTrackBehavior.fromName("forced only"))
        assertEquals(DefaultSubtitleTrackBehavior.FIRST_AVAILABLE, DefaultSubtitleTrackBehavior.fromName("first_available"))
        assertEquals(DefaultSubtitleTrackBehavior.FIRST_AVAILABLE, DefaultSubtitleTrackBehavior.fromName("first available"))
        assertEquals(DefaultSubtitleTrackBehavior.OFF, DefaultSubtitleTrackBehavior.fromName("off"))
        assertEquals(DefaultSubtitleTrackBehavior.AUTO, DefaultSubtitleTrackBehavior.fromName("other"))
        assertEquals(DefaultSubtitleTrackBehavior.AUTO, DefaultSubtitleTrackBehavior.fromName(null))
    }

    @Test
    fun subtitleSettings_defaults_areValid() {
        val settings = SubtitleSettings()
        assertTrue(settings.areSubtitlesEnabled)
        assertEquals("Auto", settings.preferredSubtitleLanguage)
        assertEquals(SubtitleTextSize.Normal, settings.textSize)
        assertEquals(SubtitleTextColor.White, settings.textColor)
        assertEquals(SubtitleBackgroundStyle.Box, settings.backgroundStyle)
        assertEquals(0.75f, settings.backgroundOpacity, 0.001f)
        assertEquals(SubtitlePosition.Bottom, settings.position)
        assertEquals(0L, settings.subtitleDelayMs)
        assertEquals(DefaultSubtitleTrackBehavior.AUTO, settings.defaultTrackBehavior)
    }
}
