package com.nexus.player.core.common.settings

import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized, reactive repository interface for managing persistent application settings.
 * Exposes an immutable [StateFlow] of [NexusSettings] with strongly typed values.
 */
interface SettingsRepository {

    /**
     * Observable stream of current application settings.
     */
    val settings: StateFlow<NexusSettings>

    // --- Playback Section ---
    suspend fun setDefaultPlaybackSpeed(speed: Float) {}
    suspend fun setAutoNextEnabled(enabled: Boolean) {}
    suspend fun setRepeatMode(mode: RepeatModeSetting) {}
    suspend fun setResumeBehavior(behavior: ResumeBehavior) {}

    // --- Player Section ---
    suspend fun setSeekDurationSeconds(seconds: Int) {}
    suspend fun setDoubleTapSeekEnabled(enabled: Boolean) {}
    suspend fun setPressAndHoldSpeedEnabled(enabled: Boolean) {}
    suspend fun setPressAndHoldSpeed(speed: Float) {}
    suspend fun setDefaultDisplayMode(mode: VideoDisplayMode) {}
    // --- Subtitles Section ---
    suspend fun setSubtitlesEnabled(enabled: Boolean) {}
    suspend fun setPreferredSubtitleLanguage(language: String) {}
    suspend fun setSubtitleFontScale(scale: Float) {}
    suspend fun setSubtitleTextSize(size: SubtitleTextSize) {}
    suspend fun setSubtitleTextColor(color: SubtitleTextColor) {}
    suspend fun setSubtitleBackgroundStyle(style: SubtitleBackgroundStyle) {}
    suspend fun setSubtitleBackgroundOpacity(opacity: Float) {}
    suspend fun setSubtitlePosition(position: SubtitlePosition) {}
    suspend fun setSubtitleDelayMs(delayMs: Long) {}
    suspend fun setDefaultSubtitleTrackBehavior(behavior: DefaultSubtitleTrackBehavior) {}

    // --- Library Section ---
    suspend fun setDefaultLayoutMode(layout: LibraryLayout) {}
    suspend fun setDefaultSortOption(sort: LibrarySort) {}
    suspend fun setScanBehavior(behavior: ScanBehavior) {}
    suspend fun setScanOnAppLaunch(enabled: Boolean) {}
    suspend fun setIncludeHiddenFiles(enabled: Boolean) {}
    suspend fun setExcludedFolders(folders: Set<String>) {}
    suspend fun addExcludedFolder(folderPath: String) {}
    suspend fun removeExcludedFolder(folderPath: String) {}

    // --- Appearance Section ---
    suspend fun setThemeMode(mode: ThemeMode) {}
    suspend fun setAmoledMode(enabled: Boolean) {}
    suspend fun setDynamicColor(enabled: Boolean) {}
    suspend fun setAccentColor(accent: AccentColor) {}

    // --- Audio Section ---
    suspend fun setPreferredAudioLanguage(language: String) {}
    suspend fun setAudioBoostPercent(percent: Int) {}
    suspend fun setAudioBoostEnabled(enabled: Boolean) {}
    suspend fun setEqualizerEnabled(enabled: Boolean) {}
    suspend fun setEqualizerPreset(preset: String) {}
    suspend fun setCustomBandLevels(levels: Map<Int, Int>) {}
    suspend fun setCustomBandLevel(bandIndex: Int, levelmB: Int) {}
    suspend fun setAudioDelayMs(delayMs: Long) {}
    suspend fun setRememberPerVideoAudioSettings(remember: Boolean) {}

    // --- Storage Section ---
    suspend fun setCacheThumbnailMaxEntries(maxEntries: Int) {}
    suspend fun setPreserveStagedDeletions(preserve: Boolean) {}

    // --- Advanced Section ---
    suspend fun setHardwareAcceleration(enabled: Boolean) {}
    suspend fun setDebugLogging(enabled: Boolean) {}
}
