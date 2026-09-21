package com.nexus.player.core.common.settings.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSettingsModelsTest {

    @Test
    fun equalizerPreset_allStandardPresetsHaveFiveBands() {
        val presets = EqualizerPreset.PRESETS
        assertEquals(10, presets.size)
        presets.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 5 bands", 5, preset.bandGains.size)
        }
    }

    @Test
    fun equalizerPreset_fromName_resolvesCorrectly() {
        assertEquals(EqualizerPreset.ROCK, EqualizerPreset.fromName("Rock"))
        assertEquals(EqualizerPreset.ROCK, EqualizerPreset.fromName("rock"))
        assertEquals(EqualizerPreset.CLASSICAL, EqualizerPreset.fromName("Classical"))
        assertEquals(EqualizerPreset.DANCE, EqualizerPreset.fromName("Dance"))
        assertEquals(EqualizerPreset.ELECTRONIC, EqualizerPreset.fromName("Electronic"))
        assertEquals(EqualizerPreset.HIP_HOP, EqualizerPreset.fromName("Hip-Hop"))
        assertEquals(EqualizerPreset.VOCAL, EqualizerPreset.fromName("Vocal"))
        assertEquals(EqualizerPreset.CUSTOM, EqualizerPreset.fromName("Custom"))
        assertEquals(EqualizerPreset.FLAT, EqualizerPreset.fromName("UnknownPreset"))
    }

    @Test
    fun formatAudioFrequency_formatsHzAndKHz() {
        assertEquals("60Hz", formatAudioFrequency(60))
        assertEquals("230Hz", formatAudioFrequency(230))
        assertEquals("910Hz", formatAudioFrequency(910))
        assertEquals("3.6kHz", formatAudioFrequency(3600))
        assertEquals("14kHz", formatAudioFrequency(14000))
        assertEquals("1kHz", formatAudioFrequency(1000))
    }

    @Test
    fun serializeAndParseBandLevels_roundTripSuccessfully() {
        val levels = mapOf(
            0 to 400,
            1 to -200,
            2 to 0,
            3 to 300,
            4 to -500
        )
        val serialized = serializeBandLevels(levels)
        assertEquals("400,-200,0,300,-500", serialized)

        val parsed = parseBandLevels(serialized)
        assertEquals(5, parsed.size)
        assertEquals(400, parsed[0])
        assertEquals(-200, parsed[1])
        assertEquals(0, parsed[2])
        assertEquals(300, parsed[3])
        assertEquals(-500, parsed[4])
    }

    @Test
    fun serializeBandLevels_clampsValuesToValidRange() {
        val extremeLevels = mapOf(
            0 to -3000,
            1 to 5000,
            2 to 0,
            3 to 1500,
            4 to -1500
        )
        val serialized = serializeBandLevels(extremeLevels)
        assertEquals("-1500,1500,0,1500,-1500", serialized)
    }

    @Test
    fun parseBandLevels_nullOrCorruptInput_returnsZeroBands() {
        val defaultNull = parseBandLevels(null)
        val defaultEmpty = parseBandLevels("")
        val defaultCorrupt = parseBandLevels("not_a_number,abc")

        listOf(defaultNull, defaultEmpty, defaultCorrupt).forEach { parsed ->
            assertEquals(5, parsed.size)
            (0 until 5).forEach { idx ->
                assertEquals(0, parsed[idx])
            }
        }
    }
}
