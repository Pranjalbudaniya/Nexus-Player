package com.nexus.player.core.common.settings.model

/**
 * Representation of an equalizer preset.
 *
 * @param name The human-readable name of the preset.
 * @param bandGains Millibel gains (1 dB = 100 mB) for 5 standard frequency bands
 *                  (approx: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz).
 */
data class EqualizerPreset(
    val name: String,
    val bandGains: List<Int>
) {
    companion object {
        val FLAT = EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0))
        val ACOUSTIC = EqualizerPreset("Acoustic", listOf(400, 200, 0, 200, 300))
        val CLASSICAL = EqualizerPreset("Classical", listOf(500, 300, -200, 400, 400))
        val DANCE = EqualizerPreset("Dance", listOf(600, 200, 0, 200, 400))
        val ELECTRONIC = EqualizerPreset("Electronic", listOf(500, 300, 0, 200, 500))
        val HIP_HOP = EqualizerPreset("Hip-Hop", listOf(700, 300, 0, 200, 300))
        val JAZZ = EqualizerPreset("Jazz", listOf(400, 200, -100, 200, 500))
        val ROCK = EqualizerPreset("Rock", listOf(600, 300, -100, 300, 600))
        val VOCAL = EqualizerPreset("Vocal", listOf(-200, 400, 600, 300, -100))
        val CUSTOM = EqualizerPreset("Custom", listOf(0, 0, 0, 0, 0))

        val PRESETS: List<EqualizerPreset> = listOf(
            FLAT,
            ACOUSTIC,
            CLASSICAL,
            DANCE,
            ELECTRONIC,
            HIP_HOP,
            JAZZ,
            ROCK,
            VOCAL,
            CUSTOM
        )

        val STANDARD_FREQUENCIES: List<Int> = listOf(60, 230, 910, 3600, 14000)

        const val MIN_BAND_LEVEL_MB = -1500
        const val MAX_BAND_LEVEL_MB = 1500
        val DEFAULT_BAND_RANGE = MIN_BAND_LEVEL_MB..MAX_BAND_LEVEL_MB

        const val MIN_BOOST_PERCENT = 100
        const val MAX_BOOST_PERCENT = 200

        fun fromName(name: String): EqualizerPreset {
            return PRESETS.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FLAT
        }
    }
}

/**
 * Formats a frequency in Hz into human-readable label (e.g., 60Hz, 3.6kHz).
 */
fun formatAudioFrequency(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val kHz = freqHz / 1000f
        if (kHz % 1f == 0f) "${kHz.toInt()}kHz" else "${kHz}kHz"
    } else {
        "${freqHz}Hz"
    }
}

/**
 * Serializes a 5-band gain map to a compact CSV string (e.g. "0,200,-100,300,0").
 */
fun serializeBandLevels(levels: Map<Int, Int>): String {
    return (0 until 5).joinToString(",") { index ->
        val level = levels[index] ?: 0
        level.coerceIn(EqualizerPreset.MIN_BAND_LEVEL_MB, EqualizerPreset.MAX_BAND_LEVEL_MB).toString()
    }
}

/**
 * Parses a 5-band CSV string into a Map of band index to gain in mB.
 */
fun parseBandLevels(raw: String?): Map<Int, Int> {
    if (raw.isNullOrBlank()) {
        return (0 until 5).associateWith { 0 }
    }
    return try {
        val parts = raw.split(",").map { it.trim().toInt() }
        (0 until 5).associateWith { index ->
            if (index < parts.size) {
                parts[index].coerceIn(EqualizerPreset.MIN_BAND_LEVEL_MB, EqualizerPreset.MAX_BAND_LEVEL_MB)
            } else {
                0
            }
        }
    } catch (_: Exception) {
        (0 until 5).associateWith { 0 }
    }
}
