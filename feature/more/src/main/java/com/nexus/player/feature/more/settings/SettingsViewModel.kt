package com.nexus.player.feature.more.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.database.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _expandedSections = MutableStateFlow<Set<SettingSection>>(setOf(SettingSection.PLAYBACK))
    private val _isClearAnalyticsDialogOpen = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        _expandedSections,
        _isClearAnalyticsDialogOpen,
        _userMessage
    ) { settings, expanded, isClearOpen, message ->
        SettingsUiState(
            settings = settings,
            expandedSections = expanded,
            isClearAnalyticsDialogOpen = isClearOpen,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun toggleSectionExpanded(section: SettingSection) {
        val current = _expandedSections.value
        _expandedSections.value = if (current.contains(section)) {
            current - section
        } else {
            current + section
        }
    }

    // Playback mutators
    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDefaultPlaybackSpeed(speed)
        }
    }

    fun setAutoNextEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAutoNextEnabled(enabled)
        }
    }

    fun setRepeatMode(mode: RepeatModeSetting) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setRepeatMode(mode)
        }
    }

    fun setResumeBehavior(behavior: ResumeBehavior) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setResumeBehavior(behavior)
        }
    }

    // Player mutators
    fun setSeekDurationSeconds(seconds: Int) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSeekDurationSeconds(seconds)
        }
    }

    fun setDoubleTapSeekEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDoubleTapSeekEnabled(enabled)
        }
    }

    fun setPressAndHoldSpeed(speed: Float) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setPressAndHoldSpeed(speed)
        }
    }

    fun setDefaultDisplayMode(mode: VideoDisplayMode) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDefaultDisplayMode(mode)
        }
    }

    // Subtitles mutators
    fun setSubtitlesEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitlesEnabled(enabled)
        }
    }

    fun setPreferredSubtitleLanguage(lang: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setPreferredSubtitleLanguage(lang)
        }
    }

    fun setSubtitleFontScale(scale: Float) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleFontScale(scale)
        }
    }

    // Library mutators
    fun setDefaultLayoutMode(layout: LibraryLayout) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDefaultLayoutMode(layout)
        }
    }

    fun setDefaultSortOption(sort: LibrarySort) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDefaultSortOption(sort)
        }
    }

    fun setScanBehavior(behavior: ScanBehavior) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setScanBehavior(behavior)
        }
    }

    // Appearance mutators
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAmoledMode(enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setAccentColor(accent: AccentColor) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAccentColor(accent)
        }
    }

    // Audio mutators
    fun setPreferredAudioLanguage(lang: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setPreferredAudioLanguage(lang)
        }
    }

    fun setAudioBoostEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAudioBoostEnabled(enabled)
        }
    }

    fun setAudioDelayMs(delayMs: Long) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAudioDelayMs(delayMs)
        }
    }

    // Advanced mutators
    fun setHardwareAcceleration(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setHardwareAcceleration(enabled)
        }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDebugLogging(enabled)
        }
    }

    // Privacy & Data management
    fun setClearAnalyticsDialogOpen(open: Boolean) {
        _isClearAnalyticsDialogOpen.value = open
    }

    fun clearAllAnalytics() {
        viewModelScope.launch(ioDispatcher) {
            videoRepository.clearAllAnalyticsAndHistory()
            _isClearAnalyticsDialogOpen.value = false
            _userMessage.value = "History and analytics have been reset."
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
