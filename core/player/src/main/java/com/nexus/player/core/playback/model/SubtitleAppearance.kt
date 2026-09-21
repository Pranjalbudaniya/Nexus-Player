package com.nexus.player.core.playback.model

import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize

typealias SubtitleTextSize = SubtitleTextSize
typealias SubtitleTextColor = SubtitleTextColor
typealias SubtitleBackgroundStyle = SubtitleBackgroundStyle
typealias SubtitlePosition = SubtitlePosition
typealias DefaultSubtitleTrackBehavior = DefaultSubtitleTrackBehavior

/**
 * Subtitle visual configuration settings.
 */
data class SubtitleAppearance(
    val textSize: SubtitleTextSize = SubtitleTextSize.Normal,
    val textColor: SubtitleTextColor = SubtitleTextColor.White,
    val backgroundStyle: SubtitleBackgroundStyle = SubtitleBackgroundStyle.Box,
    val backgroundOpacity: Float = 0.75f,
    val position: SubtitlePosition = SubtitlePosition.Bottom
) {
    companion object {
        val DEFAULT = SubtitleAppearance()
    }
}
