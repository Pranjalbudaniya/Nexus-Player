package com.nexus.player.core.playback.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioEffectsControllerTest {

    private lateinit var controller: AudioEffectsControllerImpl

    @Before
    fun setUp() {
        controller = AudioEffectsControllerImpl()
    }

    @Test
    fun equalizerPresets_allPresetsDefinedWithFiveBands() {
        val presets = EqualizerPreset.PRESETS
        assertEquals(10, presets.size)
        for (preset in presets) {
            assertEquals("Preset ${preset.name} must have 5 bands", 5, preset.bandGains.size)
        }
    }

    @Test
    fun equalizerPresets_fromNameReturnsCorrectPreset() {
        assertEquals(EqualizerPreset.ROCK, EqualizerPreset.fromName("Rock"))
        assertEquals(EqualizerPreset.ROCK, EqualizerPreset.fromName("rock"))
        assertEquals(EqualizerPreset.JAZZ, EqualizerPreset.fromName("Jazz"))
        assertEquals(EqualizerPreset.FLAT, EqualizerPreset.fromName("NonExistent"))
    }

    @Test
    fun audioBoost_clampsBetween100And200() {
        assertEquals(100, controller.boostPercent.value)

        controller.setAudioBoost(150)
        assertEquals(150, controller.boostPercent.value)

        // Below 100 clamps to 100
        controller.setAudioBoost(50)
        assertEquals(100, controller.boostPercent.value)

        // Above 200 clamps to 200
        controller.setAudioBoost(350)
        assertEquals(200, controller.boostPercent.value)
    }

    @Test
    fun equalizerEnabled_togglesState() {
        assertFalse(controller.isEqualizerEnabled.value)

        controller.setEqualizerEnabled(true)
        assertTrue(controller.isEqualizerEnabled.value)

        controller.setEqualizerEnabled(false)
        assertFalse(controller.isEqualizerEnabled.value)
    }

    @Test
    fun equalizerPreset_appliesPresetBands() {
        controller.setEqualizerPreset("Rock")
        assertEquals("Rock", controller.currentPreset.value)
        assertEquals(EqualizerPreset.ROCK.bandGains[0], controller.bandLevels.value[0])
        assertEquals(EqualizerPreset.ROCK.bandGains[1], controller.bandLevels.value[1])
        assertEquals(EqualizerPreset.ROCK.bandGains[4], controller.bandLevels.value[4])
    }

    @Test
    fun equalizerBandAdjustment_updatesPresetToCustom() {
        controller.setEqualizerPreset("Flat")
        assertEquals("Flat", controller.currentPreset.value)

        controller.setBandLevel(0, 500)
        assertEquals("Custom", controller.currentPreset.value)
        assertEquals(500, controller.bandLevels.value[0])
    }

    @Test
    fun unattachedSession_operationsAreSafeAndDoNotThrow() {
        // Calling methods without attaching an audio session must never crash
        controller.setAudioBoost(175)
        controller.setEqualizerEnabled(true)
        controller.setEqualizerPreset("Vocal")
        controller.setBandLevel(2, 300)
        controller.detachAudioSession()
        controller.release()
    }

    @Test
    fun equalizerBandLevels_setBandLevelsUpdatesMapAndSetsCustomPreset() {
        val customMap = mapOf(0 to 300, 1 to -100, 2 to 200, 3 to -300, 4 to 500)
        controller.setBandLevels(customMap)

        assertEquals("Custom", controller.currentPreset.value)
        assertEquals(300, controller.bandLevels.value[0])
        assertEquals(-100, controller.bandLevels.value[1])
        assertEquals(200, controller.bandLevels.value[2])
        assertEquals(-300, controller.bandLevels.value[3])
        assertEquals(500, controller.bandLevels.value[4])
    }

    @Test
    fun customPreset_doesNotResetCustomBandsToFlat() {
        controller.setBandLevels(mapOf(0 to 450, 1 to -250))
        assertEquals(450, controller.bandLevels.value[0])

        // Calling setEqualizerPreset("Custom") must retain the custom values
        controller.setEqualizerPreset("Custom")
        assertEquals(450, controller.bandLevels.value[0])
        assertEquals(-250, controller.bandLevels.value[1])
    }
}
