package com.nexus.player.core.playback.model

/**
 * Domain representation of video aspect-ratio scaling modes.
 *
 * Supported modes:
 * - Fit: Maintains original aspect ratio, letterboxing/pillarboxing to fit entire frame.
 * - Fill: Maintains original aspect ratio, zooming to fill the entire frame and cropping excess.
 * - Crop: Zooms and crops to standard fill framing.
 * - Stretch: Stretches video to fill the screen dimensions, ignoring aspect ratio.
 * - Original: Preserves native pixel aspect ratio without distortion.
 */
enum class VideoScaleMode(
    val id: Int,
    val label: String,
    val resizeMode: Int
) {
    Fit(id = 0, label = "Fit", resizeMode = 0),         // RESIZE_MODE_FIT
    Fill(id = 1, label = "Fill", resizeMode = 4),       // RESIZE_MODE_ZOOM
    Crop(id = 2, label = "Crop", resizeMode = 4),       // RESIZE_MODE_ZOOM
    Stretch(id = 3, label = "Stretch", resizeMode = 3), // RESIZE_MODE_FILL
    Original(id = 4, label = "Original", resizeMode = 0); // RESIZE_MODE_FIT

    fun next(): VideoScaleMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: Int): VideoScaleMode = entries.firstOrNull { it.id == id } ?: Fit
        fun fromResizeMode(mode: Int): VideoScaleMode = entries.firstOrNull { it.resizeMode == mode } ?: Fit
    }
}
