package com.nexus.player.feature.player.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.ExternalSubtitle
import com.nexus.player.core.playback.model.SubtitleAppearance
import com.nexus.player.core.playback.model.SubtitleBackgroundStyle
import com.nexus.player.core.playback.model.SubtitlePosition
import com.nexus.player.core.playback.model.SubtitleTextColor
import com.nexus.player.core.playback.model.SubtitleTextSize
import com.nexus.player.core.playback.model.VideoScaleMode
import com.nexus.player.core.playback.queue.RepeatMode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for managing persistent player preferences.
 */
interface PlayerPreferencesRepository {
    val playbackSpeed: Flow<Float>
    val resizeMode: Flow<Int>
    val decoderMode: Flow<DecoderMode>
    val preferredAudioLanguage: Flow<String?>
    val preferredSubtitleLanguage: Flow<String?>
    val areSubtitlesEnabled: Flow<Boolean>
    val audioDelayMs: Flow<Long>
    val subtitleDelayMs: Flow<Long>
    val subtitleAppearance: Flow<SubtitleAppearance>
    val seekDurationSeconds: Flow<Int>
    val isAutoNextEnabled: Flow<Boolean>
    val audioBoostPercent: Flow<Int>
    val isEqualizerEnabled: Flow<Boolean>
    val equalizerPreset: Flow<String>
    val repeatMode: Flow<RepeatMode>
    val isShuffleEnabled: Flow<Boolean>

    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setResizeMode(mode: Int)
    suspend fun setDecoderMode(mode: DecoderMode)
    suspend fun setPreferredAudioLanguage(language: String?)
    suspend fun setPreferredSubtitleLanguage(language: String?)
    suspend fun setSubtitlesEnabled(enabled: Boolean)
    suspend fun setAudioDelayMs(delayMs: Long)
    suspend fun setSubtitleDelayMs(delayMs: Long)
    suspend fun setSubtitleAppearance(appearance: SubtitleAppearance)
    suspend fun setSeekDurationSeconds(duration: Int)
    suspend fun setAutoNextEnabled(enabled: Boolean)
    suspend fun setAudioBoost(percent: Int)
    suspend fun setEqualizerEnabled(enabled: Boolean)
    suspend fun setEqualizerPreset(preset: String)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun setShuffleEnabled(enabled: Boolean)

    fun getExternalSubtitles(videoId: String): Flow<List<ExternalSubtitle>>
    suspend fun addExternalSubtitle(videoId: String, subtitle: ExternalSubtitle)

    fun getVideoScaleMode(videoId: String): Flow<VideoScaleMode>
    suspend fun setVideoScaleMode(videoId: String, mode: VideoScaleMode)
}

@Singleton
class PlayerPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PlayerPreferencesRepository {

    private object PreferencesKeys {
        val PLAYBACK_SPEED = floatPreferencesKey("key_player_playback_speed")
        val RESIZE_MODE = intPreferencesKey("key_player_resize_mode")
        val DECODER_MODE = stringPreferencesKey("key_player_decoder_mode")
        val PREFERRED_AUDIO_LANG = stringPreferencesKey("key_player_preferred_audio_lang")
        val PREFERRED_SUBTITLE_LANG = stringPreferencesKey("key_player_preferred_subtitle_lang")
        val SUBTITLES_ENABLED = booleanPreferencesKey("key_player_subtitles_enabled")
        val AUDIO_DELAY_MS = longPreferencesKey("key_player_audio_delay_ms")
        val SUBTITLE_DELAY_MS = longPreferencesKey("key_player_subtitle_delay_ms")
        val SUBTITLE_TEXT_SIZE = stringPreferencesKey("key_player_sub_text_size")
        val SUBTITLE_TEXT_COLOR = stringPreferencesKey("key_player_sub_text_color")
        val SUBTITLE_BG_STYLE = stringPreferencesKey("key_player_sub_bg_style")
        val SUBTITLE_POSITION = stringPreferencesKey("key_player_sub_position")
        val SEEK_DURATION_SECONDS = intPreferencesKey("key_player_seek_duration_seconds")
        val AUTO_NEXT_ENABLED = booleanPreferencesKey("key_player_auto_next_enabled")
        val AUDIO_BOOST = intPreferencesKey("key_player_audio_boost")
        val EQUALIZER_ENABLED = booleanPreferencesKey("key_player_equalizer_enabled")
        val EQUALIZER_PRESET = stringPreferencesKey("key_player_equalizer_preset")
        val REPEAT_MODE = stringPreferencesKey("key_player_repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("key_player_shuffle_enabled")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val playbackSpeed: Flow<Float> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.PLAYBACK_SPEED] ?: 1.0f
        }

    override val seekDurationSeconds: Flow<Int> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.SEEK_DURATION_SECONDS] ?: 10
        }

    override val isAutoNextEnabled: Flow<Boolean> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.AUTO_NEXT_ENABLED] ?: false
        }

    override val audioBoostPercent: Flow<Int> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.AUDIO_BOOST] ?: 100
        }

    override val isEqualizerEnabled: Flow<Boolean> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.EQUALIZER_ENABLED] ?: false
        }

    override val equalizerPreset: Flow<String> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.EQUALIZER_PRESET] ?: "Flat"
        }

    override val repeatMode: Flow<RepeatMode> = safePreferences
        .map { prefs ->
            val raw = prefs[PreferencesKeys.REPEAT_MODE] ?: RepeatMode.OFF.name
            try {
                RepeatMode.valueOf(raw)
            } catch (_: Exception) {
                RepeatMode.OFF
            }
        }

    override val isShuffleEnabled: Flow<Boolean> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.SHUFFLE_ENABLED] ?: false
        }

    override val resizeMode: Flow<Int> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.RESIZE_MODE] ?: 0 // RESIZE_MODE_FIT
        }

    override val decoderMode: Flow<DecoderMode> = safePreferences
        .map { prefs ->
            val modeStr = prefs[PreferencesKeys.DECODER_MODE]
            try {
                if (modeStr != null) DecoderMode.valueOf(modeStr) else DecoderMode.Hardware
            } catch (_: IllegalArgumentException) {
                DecoderMode.Hardware
            }
        }

    override val preferredAudioLanguage: Flow<String?> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.PREFERRED_AUDIO_LANG]
        }

    override val preferredSubtitleLanguage: Flow<String?> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.PREFERRED_SUBTITLE_LANG]
        }

    override val areSubtitlesEnabled: Flow<Boolean> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.SUBTITLES_ENABLED] ?: true
        }

    override val audioDelayMs: Flow<Long> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.AUDIO_DELAY_MS] ?: 0L
        }

    override val subtitleDelayMs: Flow<Long> = safePreferences
        .map { prefs ->
            prefs[PreferencesKeys.SUBTITLE_DELAY_MS] ?: 0L
        }

    override val subtitleAppearance: Flow<SubtitleAppearance> = safePreferences
        .map { prefs ->
            SubtitleAppearance(
                textSize = SubtitleTextSize.fromName(prefs[PreferencesKeys.SUBTITLE_TEXT_SIZE]),
                textColor = SubtitleTextColor.fromName(prefs[PreferencesKeys.SUBTITLE_TEXT_COLOR]),
                backgroundStyle = SubtitleBackgroundStyle.fromName(prefs[PreferencesKeys.SUBTITLE_BG_STYLE]),
                position = SubtitlePosition.fromName(prefs[PreferencesKeys.SUBTITLE_POSITION])
            )
        }

    override suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PLAYBACK_SPEED] = speed
        }
    }

    override suspend fun setResizeMode(mode: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.RESIZE_MODE] = mode
        }
    }

    override suspend fun setDecoderMode(mode: DecoderMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DECODER_MODE] = mode.name
        }
    }

    override suspend fun setPreferredAudioLanguage(language: String?) {
        dataStore.edit { prefs ->
            if (language != null) {
                prefs[PreferencesKeys.PREFERRED_AUDIO_LANG] = language
            } else {
                prefs.remove(PreferencesKeys.PREFERRED_AUDIO_LANG)
            }
        }
    }

    override suspend fun setPreferredSubtitleLanguage(language: String?) {
        dataStore.edit { prefs ->
            if (language != null) {
                prefs[PreferencesKeys.PREFERRED_SUBTITLE_LANG] = language
            } else {
                prefs.remove(PreferencesKeys.PREFERRED_SUBTITLE_LANG)
            }
        }
    }

    override suspend fun setSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUBTITLES_ENABLED] = enabled
        }
    }

    override suspend fun setAudioDelayMs(delayMs: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUDIO_DELAY_MS] = delayMs
        }
    }

    override suspend fun setSubtitleDelayMs(delayMs: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUBTITLE_DELAY_MS] = delayMs
        }
    }

    override suspend fun setSubtitleAppearance(appearance: SubtitleAppearance) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUBTITLE_TEXT_SIZE] = appearance.textSize.name
            prefs[PreferencesKeys.SUBTITLE_TEXT_COLOR] = appearance.textColor.name
            prefs[PreferencesKeys.SUBTITLE_BG_STYLE] = appearance.backgroundStyle.name
            prefs[PreferencesKeys.SUBTITLE_POSITION] = appearance.position.name
        }
    }

    override suspend fun setSeekDurationSeconds(duration: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEEK_DURATION_SECONDS] = duration
        }
    }

    override suspend fun setAutoNextEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_NEXT_ENABLED] = enabled
        }
    }

    override suspend fun setAudioBoost(percent: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUDIO_BOOST] = percent.coerceIn(100, 200)
        }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQUALIZER_ENABLED] = enabled
        }
    }

    override suspend fun setEqualizerPreset(preset: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQUALIZER_PRESET] = preset
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.REPEAT_MODE] = mode.name
        }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHUFFLE_ENABLED] = enabled
        }
    }

    override fun getExternalSubtitles(videoId: String): Flow<List<ExternalSubtitle>> = safePreferences
        .map { prefs ->
            val raw = prefs[stringPreferencesKey("key_ext_subs_$videoId")]
            deserializeExternalSubtitles(raw)
        }

    override suspend fun addExternalSubtitle(videoId: String, subtitle: ExternalSubtitle) {
        dataStore.edit { prefs ->
            val key = stringPreferencesKey("key_ext_subs_$videoId")
            val current = deserializeExternalSubtitles(prefs[key]).toMutableList()
            if (current.none { it.uri == subtitle.uri }) {
                current.add(subtitle)
            }
            prefs[key] = serializeExternalSubtitles(current)
        }
    }

    override fun getVideoScaleMode(videoId: String): Flow<VideoScaleMode> = safePreferences
        .map { prefs ->
            val modeStr = prefs[stringPreferencesKey("key_scale_mode_$videoId")]
            if (modeStr != null) {
                try {
                    VideoScaleMode.valueOf(modeStr)
                } catch (_: Exception) {
                    VideoScaleMode.Fit
                }
            } else {
                VideoScaleMode.Fit
            }
        }

    override suspend fun setVideoScaleMode(videoId: String, mode: VideoScaleMode) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("key_scale_mode_$videoId")] = mode.name
        }
    }

    private fun serializeExternalSubtitles(list: List<ExternalSubtitle>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("uri", item.uri)
            obj.put("label", item.label)
            obj.put("language", item.language ?: "")
            obj.put("mimeType", item.mimeType)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeExternalSubtitles(raw: String?): List<ExternalSubtitle> {
        if (raw.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ExternalSubtitle>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ExternalSubtitle(
                        id = obj.getString("id"),
                        uri = obj.getString("uri"),
                        label = obj.getString("label"),
                        language = obj.optString("language").takeIf { it.isNotBlank() },
                        mimeType = obj.getString("mimeType")
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore corrupted JSON
        }
        return list
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface PlayerPreferencesModule {
    @Binds
    @Singleton
    fun bindPlayerPreferencesRepository(
        impl: PlayerPreferencesRepositoryImpl
    ): PlayerPreferencesRepository
}
