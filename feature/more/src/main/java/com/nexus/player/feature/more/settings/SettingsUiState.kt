package com.nexus.player.feature.more.settings

import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.scanner.model.ScanState

/**
 * Identifiers for all 10 collapsible settings sections.
 */
enum class SettingSection(val title: String, val subtitle: String) {
    PLAYBACK("Playback", "Speed, auto-next, repeat, and resume behavior"),
    PLAYER("Player", "Gestures, seek duration, and viewport display mode"),
    SUBTITLES("Subtitles", "Appearance, text styling, and track behavior"),
    LIBRARY("Library", "Layout mode and default sorting"),
    APPEARANCE("Appearance", "Theme mode and dynamic Material colors"),
    AUDIO("Audio", "Preferred audio track, boost, and sync delay"),
    STORAGE("Storage", "Scan locations, indexing behavior, cache, and exclusions"),
    PRIVACY("Privacy", "Zero-telemetry status and local history management"),
    ADVANCED("Advanced", "Hardware acceleration and developer logs"),
    ABOUT("About", "Version details, open-source licenses, and acknowledgements")
}

/**
 * Dialog variants for the About section.
 */
enum class AboutDialogType {
    PROJECT_INFO,
    LICENSES,
    ACKNOWLEDGEMENTS
}

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val settings: NexusSettings = NexusSettings(),
    val expandedSections: Set<SettingSection> = setOf(SettingSection.PLAYBACK),
    val isClearAnalyticsDialogOpen: Boolean = false,
    val isClearHistoryDialogOpen: Boolean = false,
    val activeAboutDialog: AboutDialogType? = null,
    val userMessage: String? = null,
    val storageAccessMode: StorageAccessMode = StorageAccessMode.ALL_MEDIA,
    val indexedFolderCount: Int = 0,
    val indexedVideoCount: Int = 0,
    val scanState: ScanState = ScanState.Idle,
    val isScanning: Boolean = false
)
