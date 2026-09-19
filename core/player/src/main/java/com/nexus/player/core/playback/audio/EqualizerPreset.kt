package com.nexus.player.core.playback.audio

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

        val PRESETS: List<EqualizerPreset> = listOf(
            FLAT,
            ACOUSTIC,
            CLASSICAL,
            DANCE,
            ELECTRONIC,
            HIP_HOP,
            JAZZ,
            ROCK,
            VOCAL
        )

        fun fromName(name: String): EqualizerPreset {
            return PRESETS.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FLAT
        }
    }
}
