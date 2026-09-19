package com.nexus.player.core.playback.audio

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of [AudioEffectsController] wrapping Android's
 * [LoudnessEnhancer] for audio boost and [Equalizer] for frequency balancing.
 *
 * Implements strict fault tolerance: effect failures or device limitations never
 * crash or disrupt video playback.
 */
class AudioEffectsControllerImpl : AudioEffectsController {

    companion object {
        private const val TAG = "AudioEffectsController"
        private const val MAX_BOOST_GAIN_MB = 1000 // +10 dB at 200% boost
        private val DEFAULT_FREQUENCIES = listOf(60, 230, 910, 3600, 14000)
        private val DEFAULT_BAND_RANGE = -1500..1500
    }

    private var currentAudioSessionId: Int = 0
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    private var _isBoostSupported = true
    override val isBoostSupported: Boolean get() = _isBoostSupported

    private val _boostPercent = MutableStateFlow(100)
    override val boostPercent: StateFlow<Int> = _boostPercent.asStateFlow()

    private var _isEqualizerSupported = true
    override val isEqualizerSupported: Boolean get() = _isEqualizerSupported

    private val _isEqualizerEnabled = MutableStateFlow(false)
    override val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(EqualizerPreset.FLAT.name)
    override val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Int, Int>>(
        (0 until 5).associateWith { 0 }
    )
    override val bandLevels: StateFlow<Map<Int, Int>> = _bandLevels.asStateFlow()

    private var _bandFrequencies: List<Int> = DEFAULT_FREQUENCIES
    override val bandFrequencies: List<Int> get() = _bandFrequencies

    private var _bandLevelRange: IntRange = DEFAULT_BAND_RANGE
    override val bandLevelRange: IntRange get() = _bandLevelRange

    override fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentAudioSessionId) return
        detachAudioSession()
        currentAudioSessionId = audioSessionId

        // Initialize LoudnessEnhancer
        try {
            val enhancer = LoudnessEnhancer(audioSessionId)
            loudnessEnhancer = enhancer
            _isBoostSupported = true
            applyBoostInternal(_boostPercent.value)
        } catch (e: Throwable) {
            Log.w(TAG, "LoudnessEnhancer init failed for session $audioSessionId", e)
            _isBoostSupported = false
            loudnessEnhancer = null
        }

        // Initialize Equalizer
        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            _isEqualizerSupported = true

            // Read hardware band range
            try {
                val range = eq.bandLevelRange
                if (range != null && range.size >= 2) {
                    _bandLevelRange = range[0].toInt()..range[1].toInt()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed reading equalizer bandLevelRange", e)
            }

            // Read hardware frequencies
            try {
                val numBands = eq.numberOfBands.toInt()
                if (numBands > 0) {
                    val freqs = mutableListOf<Int>()
                    for (i in 0 until numBands) {
                        // getCenterFreq returns mHz -> convert to Hz
                        freqs.add(eq.getCenterFreq(i.toShort()) / 1000)
                    }
                    _bandFrequencies = freqs
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed reading equalizer frequencies", e)
            }

            // Apply enabled state & active preset
            eq.enabled = _isEqualizerEnabled.value
            applyPresetInternal(_currentPreset.value)
        } catch (e: Throwable) {
            Log.w(TAG, "Equalizer init failed for session $audioSessionId", e)
            _isEqualizerSupported = false
            equalizer = null
        }
    }

    override fun detachAudioSession() {
        try {
            loudnessEnhancer?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing LoudnessEnhancer", e)
        } finally {
            loudnessEnhancer = null
        }

        try {
            equalizer?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing Equalizer", e)
        } finally {
            equalizer = null
        }
        currentAudioSessionId = 0
    }

    override fun setAudioBoost(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        _boostPercent.value = clamped
        applyBoostInternal(clamped)
    }

    private fun applyBoostInternal(percent: Int) {
        val enhancer = loudnessEnhancer ?: return
        try {
            if (percent > 100) {
                // Map 100..200% linearly to 0..MAX_BOOST_GAIN_MB
                val gainmB = (((percent - 100) / 100f) * MAX_BOOST_GAIN_MB).toInt()
                enhancer.setTargetGain(gainmB)
                enhancer.enabled = true
            } else {
                enhancer.setTargetGain(0)
                enhancer.enabled = false
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to apply boost gain $percent%", e)
        }
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set equalizer enabled to $enabled", e)
        }
    }

    override fun setEqualizerPreset(presetName: String) {
        _currentPreset.value = presetName
        applyPresetInternal(presetName)
    }

    private fun applyPresetInternal(presetName: String) {
        val preset = EqualizerPreset.fromName(presetName)
        val eq = equalizer
        val levelsMap = mutableMapOf<Int, Int>()

        for (i in preset.bandGains.indices) {
            val gain = preset.bandGains[i].coerceIn(_bandLevelRange.first, _bandLevelRange.last)
            levelsMap[i] = gain
            if (eq != null && i < eq.numberOfBands) {
                try {
                    eq.setBandLevel(i.toShort(), gain.toShort())
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to set band $i level to $gain mB", e)
                }
            }
        }
        _bandLevels.value = levelsMap
    }

    override fun setBandLevel(bandIndex: Int, levelmB: Int) {
        val clamped = levelmB.coerceIn(_bandLevelRange.first, _bandLevelRange.last)
        val updated = _bandLevels.value.toMutableMap()
        updated[bandIndex] = clamped
        _bandLevels.value = updated
        _currentPreset.value = "Custom"

        try {
            val eq = equalizer
            if (eq != null && bandIndex < eq.numberOfBands) {
                eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set band $bandIndex to $clamped mB", e)
        }
    }

    override fun release() {
        detachAudioSession()
    }
}
