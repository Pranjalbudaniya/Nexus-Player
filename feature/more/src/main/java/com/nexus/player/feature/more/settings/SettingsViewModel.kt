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
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val storageAccessRepository: StorageAccessRepository? = null,
    private val mediaScanOrchestrator: MediaScanOrchestrator? = null
) : ViewModel() {

    private val _expandedSections = MutableStateFlow<Set<SettingSection>>(setOf(SettingSection.PLAYBACK))
    private val _isClearAnalyticsDialogOpen = MutableStateFlow(false)
    private val _isClearHistoryDialogOpen = MutableStateFlow(false)
    private val _activeAboutDialog = MutableStateFlow<AboutDialogType?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val storageFlow = storageAccessRepository?.storageAccessState ?: flowOf(StorageAccessState())
    private val scanStateFlow = mediaScanOrchestrator?.scanState ?: flowOf(ScanState.Idle)
    private val isScanningFlow = mediaScanOrchestrator?.isScanning ?: flowOf(false)
    private val foldersFlow = videoRepository.getFolders()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        _expandedSections,
        _isClearAnalyticsDialogOpen,
        _isClearHistoryDialogOpen,
        _activeAboutDialog,
        _userMessage,
        storageFlow,
        scanStateFlow,
        isScanningFlow,
        foldersFlow
    ) { args: Array<Any?> ->
        val settings = args[0] as com.nexus.player.core.common.settings.model.NexusSettings
        @Suppress("UNCHECKED_CAST")
        val expanded = args[1] as Set<SettingSection>
        val isClearAnalyticsOpen = args[2] as Boolean
        val isClearHistoryOpen = args[3] as Boolean
        val activeAbout = args[4] as AboutDialogType?
        val message = args[5] as String?
        val storageAccess = args[6] as StorageAccessState
        val scanState = args[7] as ScanState
        val isScanning = args[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val folders = args[9] as List<com.nexus.player.core.database.model.VideoFolder>

        val folderCount = if (storageAccess.accessMode == StorageAccessMode.SELECTED_FOLDERS) {
            storageAccess.selectedFolderUris.size
        } else {
            folders.size
        }
        val videoCount = folders.sumOf { it.videoCount }

        SettingsUiState(
            settings = settings,
            expandedSections = expanded,
            isClearAnalyticsDialogOpen = isClearAnalyticsOpen,
            isClearHistoryDialogOpen = isClearHistoryOpen,
            activeAboutDialog = activeAbout,
            userMessage = message,
            storageAccessMode = storageAccess.accessMode,
            indexedFolderCount = folderCount,
            indexedVideoCount = videoCount,
            scanState = scanState,
            isScanning = isScanning
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

    fun setPressAndHoldSpeedEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setPressAndHoldSpeedEnabled(enabled)
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

    fun setSubtitleTextSize(size: SubtitleTextSize) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleTextSize(size)
        }
    }

    fun setSubtitleTextColor(color: SubtitleTextColor) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleTextColor(color)
        }
    }

    fun setSubtitleBackgroundStyle(style: SubtitleBackgroundStyle) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleBackgroundStyle(style)
        }
    }

    fun setSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleBackgroundOpacity(opacity)
        }
    }

    fun setSubtitlePosition(position: SubtitlePosition) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitlePosition(position)
        }
    }

    fun setSubtitleDelayMs(delayMs: Long) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setSubtitleDelayMs(delayMs)
        }
    }

    fun setDefaultSubtitleTrackBehavior(behavior: DefaultSubtitleTrackBehavior) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setDefaultSubtitleTrackBehavior(behavior)
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

    fun setScanOnAppLaunch(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setScanOnAppLaunch(enabled)
        }
    }

    fun setIncludeHiddenFiles(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setIncludeHiddenFiles(enabled)
        }
    }

    fun triggerFullScan() {
        mediaScanOrchestrator?.triggerManualScan()
    }

    fun triggerIncrementalScan() {
        mediaScanOrchestrator?.triggerIncrementalScan()
    }

    fun cancelScan() {
        mediaScanOrchestrator?.cancelScan()
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

    fun setAudioBoostPercent(percent: Int) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAudioBoostPercent(percent)
        }
    }

    fun setAudioBoostEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAudioBoostEnabled(enabled)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setEqualizerEnabled(enabled)
        }
    }

    fun setEqualizerPreset(preset: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setEqualizerPreset(preset)
        }
    }

    fun setCustomBandLevel(bandIndex: Int, levelmB: Int) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setCustomBandLevel(bandIndex, levelmB)
        }
    }

    fun resetCustomBandLevels() {
        viewModelScope.launch(ioDispatcher) {
            val flatBands = (0 until 5).associateWith { 0 }
            settingsRepository.setCustomBandLevels(flatBands)
            settingsRepository.setEqualizerPreset("Flat")
        }
    }

    fun setAudioDelayMs(delayMs: Long) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setAudioDelayMs(delayMs)
        }
    }

    fun setRememberPerVideoAudioSettings(remember: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setRememberPerVideoAudioSettings(remember)
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

    fun setClearHistoryDialogOpen(open: Boolean) {
        _isClearHistoryDialogOpen.value = open
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch(ioDispatcher) {
            videoRepository.clearAllHistory()
            _isClearHistoryDialogOpen.value = false
            _userMessage.value = "Playback history has been cleared."
        }
    }

    fun setActiveAboutDialog(dialog: AboutDialogType?) {
        _activeAboutDialog.value = dialog
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
