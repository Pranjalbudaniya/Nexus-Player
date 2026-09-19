package com.nexus.player.core.playback.model

/**
 * Text size options for subtitles.
 */
enum class SubtitleTextSize(val sizeSp: Float, val label: String) {
    Small(14f, "Small"),
    Normal(18f, "Normal"),
    Large(22f, "Large"),
    ExtraLarge(28f, "Extra Large");

    companion object {
        fun fromName(name: String?): SubtitleTextSize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Normal
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
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: White
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
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Box
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
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Bottom
    }
}

/**
 * Subtitle visual configuration settings.
 */
data class SubtitleAppearance(
    val textSize: SubtitleTextSize = SubtitleTextSize.Normal,
    val textColor: SubtitleTextColor = SubtitleTextColor.White,
    val backgroundStyle: SubtitleBackgroundStyle = SubtitleBackgroundStyle.Box,
    val position: SubtitlePosition = SubtitlePosition.Bottom
) {
    companion object {
        val DEFAULT = SubtitleAppearance()
    }
}
