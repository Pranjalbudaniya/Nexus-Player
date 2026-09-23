package com.nexus.player.core.common.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.settings.model.AboutSettings
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.AdvancedSettings
import com.nexus.player.core.common.settings.model.AppearanceSettings
import com.nexus.player.core.common.settings.model.AudioSettings
import com.nexus.player.core.common.settings.model.EqualizerPreset
import com.nexus.player.core.common.settings.model.parseBandLevels
import com.nexus.player.core.common.settings.model.serializeBandLevels
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySettings
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.settings.model.PlaybackSettings
import com.nexus.player.core.common.settings.model.PlayerSettings
import com.nexus.player.core.common.settings.model.PrivacySettings
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.StorageSettings
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleSettings
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private object Keys {
        // Playback
        val PLAYBACK_SPEED = floatPreferencesKey("key_player_playback_speed")
        val AUTO_NEXT_ENABLED = booleanPreferencesKey("key_player_auto_next_enabled")
        val REPEAT_MODE = stringPreferencesKey("key_player_repeat_mode")
        val RESUME_BEHAVIOR = stringPreferencesKey("key_playback_resume_behavior")

        // Player
        val SEEK_DURATION_SECONDS = intPreferencesKey("key_player_seek_duration_seconds")
        val DOUBLE_TAP_SEEK_ENABLED = booleanPreferencesKey("key_player_double_tap_seek")
        val PRESS_HOLD_SPEED_ENABLED = booleanPreferencesKey("key_player_press_hold_speed_enabled")
        val PRESS_HOLD_SPEED = floatPreferencesKey("key_player_press_hold_speed")
        val DEFAULT_DISPLAY_MODE = stringPreferencesKey("key_player_default_display_mode")

        // Subtitles
        val SUBTITLES_ENABLED = booleanPreferencesKey("key_player_subtitles_enabled")
        val PREFERRED_SUBTITLE_LANG = stringPreferencesKey("key_player_preferred_subtitle_lang")
        val SUBTITLE_FONT_SCALE = floatPreferencesKey("key_subtitle_font_scale")
        val SUBTITLE_TEXT_SIZE = stringPreferencesKey("key_player_sub_text_size")
        val SUBTITLE_TEXT_COLOR = stringPreferencesKey("key_player_sub_text_color")
        val SUBTITLE_BG_STYLE = stringPreferencesKey("key_player_sub_bg_style")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("key_player_sub_bg_opacity")
        val SUBTITLE_POSITION = stringPreferencesKey("key_player_sub_position")
        val SUBTITLE_DELAY_MS = longPreferencesKey("key_player_subtitle_delay_ms")
        val SUBTITLE_TRACK_BEHAVIOR = stringPreferencesKey("key_player_sub_track_behavior")

        // Library
        val LIBRARY_LAYOUT_MODE = stringPreferencesKey("key_library_layout_mode")
        val LIBRARY_SORT_FIELD = stringPreferencesKey("key_library_sort_field")
        val LIBRARY_SORT_DIRECTION = stringPreferencesKey("key_library_sort_direction")
        val SCAN_BEHAVIOR = stringPreferencesKey("key_library_scan_behavior")
        val SCAN_ON_APP_LAUNCH = booleanPreferencesKey("key_library_scan_on_launch")
        val INCLUDE_HIDDEN_FILES = booleanPreferencesKey("key_library_include_hidden")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("key_library_excluded_folders")

        // Appearance
        val THEME_MODE = stringPreferencesKey("key_appearance_theme_mode")
        val AMOLED_MODE = booleanPreferencesKey("key_appearance_amoled_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("key_appearance_dynamic_color")
        val ACCENT_COLOR = stringPreferencesKey("key_appearance_accent_color")

        // Audio
        val PREFERRED_AUDIO_LANG = stringPreferencesKey("key_player_preferred_audio_lang")
        val AUDIO_BOOST_PERCENT = intPreferencesKey("key_player_audio_boost")
        val AUDIO_BOOST_ENABLED = booleanPreferencesKey("key_audio_boost_enabled")
        val EQUALIZER_ENABLED = booleanPreferencesKey("key_player_equalizer_enabled")
        val EQUALIZER_PRESET = stringPreferencesKey("key_player_equalizer_preset")
        val CUSTOM_BAND_LEVELS = stringPreferencesKey("key_player_custom_band_levels")
        val AUDIO_DELAY_MS = longPreferencesKey("key_player_audio_delay_ms")
        val REMEMBER_PER_VIDEO_AUDIO = booleanPreferencesKey("key_player_remember_per_video_audio")

        // Storage
        val THUMBNAIL_CACHE_SIZE = intPreferencesKey("key_storage_thumb_cache_size")
        val PRESERVE_STAGED = booleanPreferencesKey("key_storage_preserve_staged")

        // Advanced
        val HW_ACCEL = booleanPreferencesKey("key_advanced_hw_accel")
        val DEBUG_LOGGING = booleanPreferencesKey("key_advanced_debug_logging")
    }

    override val settings: StateFlow<NexusSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toNexusSettings() }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = NexusSettings()
        )

    private fun Preferences.toNexusSettings(): NexusSettings {
        // 1. Playback
        val playback = PlaybackSettings(
            defaultPlaybackSpeed = this[Keys.PLAYBACK_SPEED] ?: 1.0f,
            isAutoNextEnabled = this[Keys.AUTO_NEXT_ENABLED] ?: true,
            repeatMode = parseEnum(this[Keys.REPEAT_MODE], RepeatModeSetting.OFF),
            resumeBehavior = parseEnum(this[Keys.RESUME_BEHAVIOR], ResumeBehavior.ALWAYS)
        )

        // 2. Player
        val player = PlayerSettings(
            seekDurationSeconds = this[Keys.SEEK_DURATION_SECONDS] ?: 10,
            isDoubleTapSeekEnabled = this[Keys.DOUBLE_TAP_SEEK_ENABLED] ?: true,
            isPressAndHoldSpeedEnabled = this[Keys.PRESS_HOLD_SPEED_ENABLED] ?: true,
            pressAndHoldSpeed = this[Keys.PRESS_HOLD_SPEED] ?: 2.0f,
            defaultDisplayMode = parseEnum(this[Keys.DEFAULT_DISPLAY_MODE], VideoDisplayMode.FIT)
        )

        // 3. Subtitles
        val subtitles = SubtitleSettings(
            areSubtitlesEnabled = this[Keys.SUBTITLES_ENABLED] ?: true,
            preferredSubtitleLanguage = this[Keys.PREFERRED_SUBTITLE_LANG] ?: "Auto",
            textSize = parseEnum(this[Keys.SUBTITLE_TEXT_SIZE], SubtitleTextSize.Normal),
            textColor = parseEnum(this[Keys.SUBTITLE_TEXT_COLOR], SubtitleTextColor.White),
            backgroundStyle = parseEnum(this[Keys.SUBTITLE_BG_STYLE], SubtitleBackgroundStyle.Box),
            backgroundOpacity = (this[Keys.SUBTITLE_BG_OPACITY] ?: 0.75f).coerceIn(0f, 1f),
            position = parseEnum(this[Keys.SUBTITLE_POSITION], SubtitlePosition.Bottom),
            subtitleDelayMs = this[Keys.SUBTITLE_DELAY_MS] ?: 0L,
            defaultTrackBehavior = parseEnum(this[Keys.SUBTITLE_TRACK_BEHAVIOR], DefaultSubtitleTrackBehavior.AUTO),
            fontSizeScale = this[Keys.SUBTITLE_FONT_SCALE] ?: 1.0f
        )

        // 4. Library
        val library = LibrarySettings(
            defaultLayoutMode = parseEnum(this[Keys.LIBRARY_LAYOUT_MODE], LibraryLayout.GRID),
            defaultSortOption = parseEnum(this[Keys.LIBRARY_SORT_FIELD], LibrarySort.DATE_ADDED_DESC),
            scanBehavior = parseEnum(this[Keys.SCAN_BEHAVIOR], ScanBehavior.AUTOMATIC),
            scanOnAppLaunch = this[Keys.SCAN_ON_APP_LAUNCH] ?: true,
            includeHiddenFiles = this[Keys.INCLUDE_HIDDEN_FILES] ?: false,
            excludedFolders = this[Keys.EXCLUDED_FOLDERS] ?: emptySet()
        )

        // 5. Appearance
        val appearance = AppearanceSettings(
            themeMode = parseEnum(this[Keys.THEME_MODE], ThemeMode.SYSTEM),
            useAmoledMode = this[Keys.AMOLED_MODE] ?: false,
            useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
            accentColor = parseEnum(this[Keys.ACCENT_COLOR], AccentColor.DEFAULT)
        )

        // 6. Audio
        val boostPercent = this[Keys.AUDIO_BOOST_PERCENT] ?: 100
        val isBoostEnabled = this[Keys.AUDIO_BOOST_ENABLED] ?: (boostPercent > 100)
        val audio = AudioSettings(
            preferredAudioLanguage = this[Keys.PREFERRED_AUDIO_LANG] ?: "Auto",
            audioBoostPercent = boostPercent,
            isAudioBoostEnabled = isBoostEnabled,
            isEqualizerEnabled = this[Keys.EQUALIZER_ENABLED] ?: false,
            equalizerPreset = this[Keys.EQUALIZER_PRESET] ?: "Flat",
            customBandLevels = parseBandLevels(this[Keys.CUSTOM_BAND_LEVELS]),
            audioDelayMs = this[Keys.AUDIO_DELAY_MS] ?: 0L,
            rememberPerVideoAudioSettings = this[Keys.REMEMBER_PER_VIDEO_AUDIO] ?: false
        )

        // 7. Storage
        val storage = StorageSettings(
            cacheThumbnailMaxEntries = this[Keys.THUMBNAIL_CACHE_SIZE] ?: 200,
            preserveStagedDeletions = this[Keys.PRESERVE_STAGED] ?: true
        )

        // 8. Privacy
        val privacy = PrivacySettings(
            isLocalOnlyMode = true
        )

        // 9. Advanced
        val advanced = AdvancedSettings(
            hardwareAcceleration = this[Keys.HW_ACCEL] ?: true,
            debugLogging = this[Keys.DEBUG_LOGGING] ?: false
        )

        // 10. About
        val about = AboutSettings()

        return NexusSettings(
            playback = playback,
            player = player,
            subtitles = subtitles,
            library = library,
            appearance = appearance,
            audio = audio,
            storage = storage,
            privacy = privacy,
            advanced = advanced,
            about = about
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(name: String?, default: T): T {
        if (name == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, name)
        } catch (_: IllegalArgumentException) {
            default
        }
    }

    // --- Mutators ---

    override suspend fun setDefaultPlaybackSpeed(speed: Float) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
        }
    }

    override suspend fun setAutoNextEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.AUTO_NEXT_ENABLED] = enabled }
        }
    }

    override suspend fun setRepeatMode(mode: RepeatModeSetting) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
        }
    }

    override suspend fun setResumeBehavior(behavior: ResumeBehavior) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.RESUME_BEHAVIOR] = behavior.name }
        }
    }

    override suspend fun setSeekDurationSeconds(seconds: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SEEK_DURATION_SECONDS] = seconds }
        }
    }

    override suspend fun setDoubleTapSeekEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.DOUBLE_TAP_SEEK_ENABLED] = enabled }
        }
    }

    override suspend fun setPressAndHoldSpeedEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PRESS_HOLD_SPEED_ENABLED] = enabled }
        }
    }

    override suspend fun setPressAndHoldSpeed(speed: Float) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PRESS_HOLD_SPEED] = speed }
        }
    }

    override suspend fun setDefaultDisplayMode(mode: VideoDisplayMode) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.DEFAULT_DISPLAY_MODE] = mode.name }
        }
    }

    override suspend fun setSubtitlesEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLES_ENABLED] = enabled }
        }
    }

    override suspend fun setPreferredSubtitleLanguage(language: String) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PREFERRED_SUBTITLE_LANG] = language }
        }
    }

    override suspend fun setSubtitleFontScale(scale: Float) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_FONT_SCALE] = scale }
        }
    }

    override suspend fun setSubtitleTextSize(size: SubtitleTextSize) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_TEXT_SIZE] = size.name }
        }
    }

    override suspend fun setSubtitleTextColor(color: SubtitleTextColor) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_TEXT_COLOR] = color.name }
        }
    }

    override suspend fun setSubtitleBackgroundStyle(style: SubtitleBackgroundStyle) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_BG_STYLE] = style.name }
        }
    }

    override suspend fun setSubtitleBackgroundOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_BG_OPACITY] = clamped }
        }
    }

    override suspend fun setSubtitlePosition(position: SubtitlePosition) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_POSITION] = position.name }
        }
    }

    override suspend fun setSubtitleDelayMs(delayMs: Long) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_DELAY_MS] = delayMs }
        }
    }

    override suspend fun setDefaultSubtitleTrackBehavior(behavior: DefaultSubtitleTrackBehavior) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SUBTITLE_TRACK_BEHAVIOR] = behavior.name }
        }
    }

    override suspend fun setDefaultLayoutMode(layout: LibraryLayout) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.LIBRARY_LAYOUT_MODE] = layout.name }
        }
    }

    override suspend fun setDefaultSortOption(sort: LibrarySort) {
        withContext(ioDispatcher) {
            dataStore.edit {
                it[Keys.LIBRARY_SORT_FIELD] = sort.name
                it[Keys.LIBRARY_SORT_DIRECTION] = when (sort) {
                    LibrarySort.DATE_ADDED_DESC,
                    LibrarySort.TITLE_DESC,
                    LibrarySort.DURATION_DESC,
                    LibrarySort.SIZE_DESC,
                    LibrarySort.DATE_MODIFIED_DESC -> "DESCENDING"
                    LibrarySort.DATE_ADDED_ASC,
                    LibrarySort.TITLE_ASC,
                    LibrarySort.DURATION_ASC,
                    LibrarySort.SIZE_ASC,
                    LibrarySort.DATE_MODIFIED_ASC -> "ASCENDING"
                }
            }
        }
    }

    override suspend fun setScanBehavior(behavior: ScanBehavior) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SCAN_BEHAVIOR] = behavior.name }
        }
    }

    override suspend fun setScanOnAppLaunch(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.SCAN_ON_APP_LAUNCH] = enabled }
        }
    }

    override suspend fun setIncludeHiddenFiles(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.INCLUDE_HIDDEN_FILES] = enabled }
        }
    }

    override suspend fun setExcludedFolders(folders: Set<String>) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.EXCLUDED_FOLDERS] = folders }
        }
    }

    override suspend fun addExcludedFolder(folderPath: String) {
        withContext(ioDispatcher) {
            dataStore.edit {
                val current = it[Keys.EXCLUDED_FOLDERS] ?: emptySet()
                it[Keys.EXCLUDED_FOLDERS] = current + folderPath
            }
        }
    }

    override suspend fun removeExcludedFolder(folderPath: String) {
        withContext(ioDispatcher) {
            dataStore.edit {
                val current = it[Keys.EXCLUDED_FOLDERS] ?: emptySet()
                it[Keys.EXCLUDED_FOLDERS] = current - folderPath
            }
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.THEME_MODE] = mode.name }
        }
    }

    override suspend fun setAmoledMode(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.AMOLED_MODE] = enabled }
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
        }
    }

    override suspend fun setAccentColor(accent: AccentColor) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.ACCENT_COLOR] = accent.name }
        }
    }

    override suspend fun setPreferredAudioLanguage(language: String) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PREFERRED_AUDIO_LANG] = language }
        }
    }

    override suspend fun setAudioBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(EqualizerPreset.MIN_BOOST_PERCENT, EqualizerPreset.MAX_BOOST_PERCENT)
        withContext(ioDispatcher) {
            dataStore.edit {
                it[Keys.AUDIO_BOOST_PERCENT] = clamped
                it[Keys.AUDIO_BOOST_ENABLED] = clamped > 100
            }
        }
    }

    override suspend fun setAudioBoostEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit {
                it[Keys.AUDIO_BOOST_ENABLED] = enabled
                if (!enabled) {
                    it[Keys.AUDIO_BOOST_PERCENT] = 100
                } else if ((it[Keys.AUDIO_BOOST_PERCENT] ?: 100) <= 100) {
                    it[Keys.AUDIO_BOOST_PERCENT] = 125
                }
            }
        }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.EQUALIZER_ENABLED] = enabled }
        }
    }

    override suspend fun setEqualizerPreset(preset: String) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.EQUALIZER_PRESET] = preset }
        }
    }

    override suspend fun setCustomBandLevels(levels: Map<Int, Int>) {
        withContext(ioDispatcher) {
            dataStore.edit {
                it[Keys.CUSTOM_BAND_LEVELS] = serializeBandLevels(levels)
            }
        }
    }

    override suspend fun setCustomBandLevel(bandIndex: Int, levelmB: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { prefs ->
                val currentLevels = parseBandLevels(prefs[Keys.CUSTOM_BAND_LEVELS]).toMutableMap()
                currentLevels[bandIndex] = levelmB.coerceIn(EqualizerPreset.MIN_BAND_LEVEL_MB, EqualizerPreset.MAX_BAND_LEVEL_MB)
                prefs[Keys.CUSTOM_BAND_LEVELS] = serializeBandLevels(currentLevels)
                prefs[Keys.EQUALIZER_PRESET] = "Custom"
            }
        }
    }

    override suspend fun setAudioDelayMs(delayMs: Long) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.AUDIO_DELAY_MS] = delayMs }
        }
    }

    override suspend fun setRememberPerVideoAudioSettings(remember: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.REMEMBER_PER_VIDEO_AUDIO] = remember }
        }
    }

    override suspend fun setCacheThumbnailMaxEntries(maxEntries: Int) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.THUMBNAIL_CACHE_SIZE] = maxEntries }
        }
    }

    override suspend fun setPreserveStagedDeletions(preserve: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.PRESERVE_STAGED] = preserve }
        }
    }

    override suspend fun setHardwareAcceleration(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.HW_ACCEL] = enabled }
        }
    }

    override suspend fun setDebugLogging(enabled: Boolean) {
        withContext(ioDispatcher) {
            dataStore.edit { it[Keys.DEBUG_LOGGING] = enabled }
        }
    }
}

