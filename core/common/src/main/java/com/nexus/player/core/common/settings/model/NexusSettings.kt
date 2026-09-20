package com.nexus.player.core.common.settings.model

/**
 * Behavior when opening a partially watched video.
 */
enum class ResumeBehavior(val label: String) {
    ALWAYS("Always resume"),
    ASK("Ask before resuming"),
    NEVER("Start from beginning")
}

/**
 * Default display aspect ratio mode for the video player.
 */
enum class VideoDisplayMode(val label: String) {
    FIT("Fit to screen"),
    STRETCH("Stretch to fill"),
    CROP("Crop to fill (Zoom)"),
    ORIGINAL_100("Original (100%)")
}

/**
 * Layout arrangement for media listings.
 */
enum class LibraryLayout(val label: String) {
    GRID("Grid view"),
    LIST("List view")
}

/**
 * Default sort order for video listings.
 */
enum class LibrarySort(val label: String) {
    DATE_ADDED_DESC("Date added (Newest first)"),
    DATE_ADDED_ASC("Date added (Oldest first)"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    DURATION_DESC("Duration (Longest first)"),
    SIZE_DESC("File size (Largest first)")
}

/**
 * Media indexing and scanning behavior.
 */
enum class ScanBehavior(val label: String) {
    AUTOMATIC("Auto-scan on launch"),
    MANUAL("Manual refresh only")
}

/**
 * Application visual theme mode.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    DARK("Always dark"),
    LIGHT("Always light")
}

/**
 * Playback repeat modes for settings persistence.
 */
enum class RepeatModeSetting(val label: String) {
    OFF("Off"),
    ALL("Repeat all"),
    ONE("Repeat current video")
}

/**
 * Playback section configuration.
 */
data class PlaybackSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val isAutoNextEnabled: Boolean = true,
    val repeatMode: RepeatModeSetting = RepeatModeSetting.OFF,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ALWAYS
)

/**
 * Player gesture and viewport configuration.
 */
data class PlayerSettings(
    val seekDurationSeconds: Int = 10,
    val isDoubleTapSeekEnabled: Boolean = true,
    val pressAndHoldSpeed: Float = 2.0f,
    val defaultDisplayMode: VideoDisplayMode = VideoDisplayMode.FIT
)

/**
 * Subtitles configuration.
 */
data class SubtitleSettings(
    val areSubtitlesEnabled: Boolean = true,
    val preferredSubtitleLanguage: String = "Auto",
    val fontSizeScale: Float = 1.0f
)

/**
 * Media library display configuration.
 */
data class LibrarySettings(
    val defaultLayoutMode: LibraryLayout = LibraryLayout.GRID,
    val defaultSortOption: LibrarySort = LibrarySort.DATE_ADDED_DESC,
    val scanBehavior: ScanBehavior = ScanBehavior.AUTOMATIC
)

/**
 * Appearance and theme configuration.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true
)

/**
 * Audio output and passthrough configuration.
 */
data class AudioSettings(
    val preferredAudioLanguage: String = "Auto",
    val isAudioBoostEnabled: Boolean = false,
    val audioDelayMs: Long = 0L
)

/**
 * Local storage and cache configuration.
 */
data class StorageSettings(
    val cacheThumbnailMaxEntries: Int = 200,
    val preserveStagedDeletions: Boolean = true
)

/**
 * Privacy and local analytics status.
 */
data class PrivacySettings(
    val isLocalOnlyMode: Boolean = true
)

/**
 * Advanced rendering and debugging configuration.
 */
data class AdvancedSettings(
    val hardwareAcceleration: Boolean = true,
    val debugLogging: Boolean = false
)

/**
 * Application metadata and build details.
 */
data class AboutSettings(
    val appName: String = "Nexus Player",
    val versionName: String = "1.0.0",
    val buildType: String = "Debug",
    val license: String = "Open Source (Apache 2.0)",
    val privacyStatus: String = "100% Offline • Zero Telemetry"
)

/**
 * Consolidated immutable settings representation.
 */
data class NexusSettings(
    val playback: PlaybackSettings = PlaybackSettings(),
    val player: PlayerSettings = PlayerSettings(),
    val subtitles: SubtitleSettings = SubtitleSettings(),
    val library: LibrarySettings = LibrarySettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val audio: AudioSettings = AudioSettings(),
    val storage: StorageSettings = StorageSettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val advanced: AdvancedSettings = AdvancedSettings(),
    val about: AboutSettings = AboutSettings()
)
