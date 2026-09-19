package com.nexus.player.core.playback.model

/**
 * Decoder selection mode for video playback.
 *
 * ## Platform Limitations
 *
 * Media3/ExoPlayer delegates codec selection to Android's [android.media.MediaCodec].
 * The platform chooses hardware or software decoders automatically based on format
 * support. There is **no built-in toggle** to force pure software decoding for
 * arbitrary formats.
 *
 * To implement a true [Software] mode, a native decoder backend such as
 * `media3-decoder-ffmpeg` must be integrated. The architecture isolates
 * [RenderersFactory][androidx.media3.exoplayer.RenderersFactory] configuration
 * so this can be added cleanly without restructuring.
 *
 * @see Hardware for the default, recommended mode.
 */
enum class DecoderMode {
    /**
     * Default platform hardware-accelerated decoding.
     * Uses [android.media.MediaCodec] with hardware codec preference.
     * This is the recommended and currently only functional mode.
     */
    Hardware,

    /**
     * Software decoding fallback.
     *
     * **Not yet functional.** Requires integrating a native decoder library
     * (e.g., `media3-decoder-ffmpeg`). Selecting this mode currently falls
     * back to [Hardware] with a logged warning.
     */
    Software,

    /**
     * Automatic: prefer hardware codecs, fallback to software if supported.
     *
     * Behaves identically to [Hardware] until a software decoder backend
     * is integrated.
     */
    Auto
}
