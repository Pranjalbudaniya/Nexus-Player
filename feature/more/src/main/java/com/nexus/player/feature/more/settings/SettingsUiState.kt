package com.nexus.player.feature.more.settings

import com.nexus.player.core.common.settings.model.NexusSettings

/**
 * Identifiers for all 10 collapsible settings sections.
 */
enum class SettingSection(val title: String, val subtitle: String) {
    PLAYBACK("Playback", "Speed, auto-next, repeat, and resume behavior"),
    PLAYER("Player", "Gestures, seek duration, and viewport display mode"),
    SUBTITLES("Subtitles", "Preferred language and font scaling"),
    LIBRARY("Library", "Layout mode, default sorting, and scan behavior"),
    APPEARANCE("Appearance", "Theme mode and dynamic Material colors"),
    AUDIO("Audio", "Preferred audio track, boost, and sync delay"),
    STORAGE("Storage", "Thumbnail cache and deletion behaviors"),
    PRIVACY("Privacy", "Zero-telemetry status and local history management"),
    ADVANCED("Advanced", "Hardware acceleration and developer logs"),
    ABOUT("About", "Version details, build information, and license")
}

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val settings: NexusSettings = NexusSettings(),
    val expandedSections: Set<SettingSection> = setOf(SettingSection.PLAYBACK),
    val isClearAnalyticsDialogOpen: Boolean = false,
    val userMessage: String? = null
)
