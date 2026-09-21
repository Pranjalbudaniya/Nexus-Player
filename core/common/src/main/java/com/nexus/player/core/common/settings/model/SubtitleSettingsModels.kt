package com.nexus.player.core.common.settings.model

/**
 * Text size options for subtitles.
 */
enum class SubtitleTextSize(val sizeSp: Float, val label: String) {
    Small(14f, "Small"),
    Normal(18f, "Medium"),
    Large(22f, "Large"),
    ExtraLarge(28f, "Extra Large");

    companion object {
        val Medium: SubtitleTextSize = Normal

        fun fromName(name: String?): SubtitleTextSize =
            when (name?.lowercase()?.trim()) {
                "small" -> Small
                "normal", "medium" -> Normal
                "large" -> Large
                "extralarge", "extra large", "extra_large" -> ExtraLarge
                else -> Normal
            }
    }
}

/**
 * Text color options for subtitles using standard Android ARGB integer color values.
 */
enum class SubtitleTextColor(val argbColor: Int, val label: String) {
    White(-0x1, "White"),               // 0xFFFFFFFF
    Yellow(-0x14c5, "Yellow"),          // 0xFFFFEB3B
    Cyan(-0xff1a01, "Cyan"),            // 0xFF00E5FF
    LightGreen(-0x8900fd, "Green");     // 0xFF76FF03

    companion object {
        fun fromName(name: String?): SubtitleTextColor =
            when (name?.lowercase()?.trim()) {
                "white" -> White
                "yellow" -> Yellow
                "cyan" -> Cyan
                "lightgreen", "green" -> LightGreen
                else -> White
            }
    }
}

/**
 * Background and edge decoration styles for subtitles.
 */
enum class SubtitleBackgroundStyle(val label: String) {
    None("None"),
    DropShadow("Shadow"),
    Outline("Outline"),
    Box("Background Box");

    companion object {
        fun fromName(name: String?): SubtitleBackgroundStyle =
            when (name?.lowercase()?.trim()) {
                "none" -> None
                "dropshadow", "shadow", "drop shadow" -> DropShadow
                "outline" -> Outline
                "box", "background box" -> Box
                else -> Box
            }
    }
}

/**
 * Vertical position/padding options for subtitles.
 */
enum class SubtitlePosition(val bottomPaddingFraction: Float, val label: String) {
    Bottom(0.08f, "Bottom"),
    Raised(0.18f, "Raised"),
    Top(0.85f, "Top");

    companion object {
        fun fromName(name: String?): SubtitlePosition =
            when (name?.lowercase()?.trim()) {
                "bottom" -> Bottom
                "raised" -> Raised
                "top" -> Top
                else -> Bottom
            }
    }
}

/**
 * Automated subtitle track selection behavior for new video playback sessions.
 */
enum class DefaultSubtitleTrackBehavior(val label: String) {
    AUTO("Preferred language with fallback"),
    FORCED_ONLY("Forced subtitles only"),
    FIRST_AVAILABLE("First available track"),
    OFF("Subtitles off by default");

    companion object {
        fun fromName(name: String?): DefaultSubtitleTrackBehavior =
            when (name?.lowercase()?.trim()) {
                "auto" -> AUTO
                "forced_only", "forced only" -> FORCED_ONLY
                "first_available", "first available" -> FIRST_AVAILABLE
                "off" -> OFF
                else -> AUTO
            }
    }
}
