package com.nexus.player.core.playback.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Controller abstraction managing audio effects (Audio Boost via LoudnessEnhancer
 * and Equalizer) attached to the active playback audio session.
 *
 * All operations are guaranteed to be safe and resilient against platform discrepancies,
 * missing hardware effects, or session creation failures.
 */
interface AudioEffectsController {

    /**
     * Whether audio boost (LoudnessEnhancer) is supported on this device.
     */
    val isBoostSupported: Boolean

    /**
     * Current audio boost percentage. Range: 100% to 200%. Default: 100% (no boost).
     */
    val boostPercent: StateFlow<Int>

    /**
     * Whether hardware Equalizer effect is supported on this device.
     */
    val isEqualizerSupported: Boolean

    /**
     * Whether the equalizer is currently enabled.
     */
    val isEqualizerEnabled: StateFlow<Boolean>

    /**
     * Currently active equalizer preset name (e.g. "Flat", "Rock", "Custom").
     */
    val currentPreset: StateFlow<String>

    /**
     * Map of band index to gain level in millibels (mB).
     */
    val bandLevels: StateFlow<Map<Int, Int>>

    /**
     * Center frequencies of available bands in Hz (e.g. [60, 230, 910, 3600, 14000]).
     */
    val bandFrequencies: List<Int>

    /**
     * Valid range for band levels in millibels (typically -1500..1500 mB / -15dB..+15dB).
     */
    val bandLevelRange: IntRange

    /**
     * Attaches the effects to the specified audio session ID from ExoPlayer.
     */
    fun attachAudioSession(audioSessionId: Int)

    /**
     * Detaches effects from the current audio session.
     */
    fun detachAudioSession()

    /**
     * Sets the audio boost percentage (clamped to 100..200).
     */
    fun setAudioBoost(percent: Int)

    /**
     * Enables or disables the equalizer effect.
     */
    fun setEqualizerEnabled(enabled: Boolean)

    /**
     * Applies a named preset (e.g. "Rock", "Classical", "Flat").
     */
    fun setEqualizerPreset(presetName: String)

    /**
     * Adjusts the gain of a specific band in millibels (mB).
     * Sets the preset name to "Custom".
     */
    fun setBandLevel(bandIndex: Int, levelmB: Int)

    /**
     * Sets multiple band gains at once in millibels (mB).
     * Sets the preset name to "Custom".
     */
    fun setBandLevels(levels: Map<Int, Int>)

    /**
     * Releases all underlying audio effect resources.
     */
    fun release()
}
