package com.nexus.player.feature.more.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.DeleteSweep

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import com.nexus.player.feature.more.analytics.component.ClearAnalyticsDialog
import com.nexus.player.feature.more.settings.component.AccentColorPicker
import com.nexus.player.feature.more.settings.component.AcknowledgementsDialog
import com.nexus.player.feature.more.settings.component.AudioSettingsCard
import com.nexus.player.feature.more.settings.component.ClearHistoryDialog
import com.nexus.player.feature.more.settings.component.LicensesDialog
import com.nexus.player.feature.more.settings.component.ProjectInfoDialog
import com.nexus.player.feature.more.settings.component.SettingActionRow
import com.nexus.player.feature.more.settings.component.SettingInfoRow
import com.nexus.player.feature.more.settings.component.SettingSectionCard
import com.nexus.player.feature.more.settings.component.SettingSelectRow
import com.nexus.player.feature.more.settings.component.SettingSelectionDialog
import com.nexus.player.feature.more.settings.component.SettingSwitchRow
import com.nexus.player.feature.more.settings.component.StorageSettingsCard
import com.nexus.player.feature.more.settings.component.SubtitleSettingsCard

private enum class ActiveDialog {
    PLAYBACK_SPEED,
    REPEAT_MODE,
    RESUME_BEHAVIOR,
    SEEK_DURATION,
    PRESS_HOLD_SPEED,
    DISPLAY_MODE,
    SUBTITLE_LANG,
    SUBTITLE_TRACK_BEHAVIOR,
    LIBRARY_LAYOUT,
    LIBRARY_SORT,
    THEME_MODE,
    AUDIO_LANG,
    AUDIO_DELAY
}

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToStorageLocations: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToStorageLocations = onNavigateToStorageLocations,
        onToggleSection = viewModel::toggleSectionExpanded,
        onSetDefaultPlaybackSpeed = viewModel::setDefaultPlaybackSpeed,
        onSetAutoNextEnabled = viewModel::setAutoNextEnabled,
        onSetRepeatMode = viewModel::setRepeatMode,
        onSetResumeBehavior = viewModel::setResumeBehavior,
        onSetSeekDurationSeconds = viewModel::setSeekDurationSeconds,
        onSetDoubleTapSeekEnabled = viewModel::setDoubleTapSeekEnabled,
        onSetPressAndHoldSpeed = viewModel::setPressAndHoldSpeed,
        onSetDefaultDisplayMode = viewModel::setDefaultDisplayMode,
        onSetSubtitlesEnabled = viewModel::setSubtitlesEnabled,
        onSetPreferredSubtitleLanguage = viewModel::setPreferredSubtitleLanguage,
        onSetSubtitleTextSize = viewModel::setSubtitleTextSize,
        onSetSubtitleTextColor = viewModel::setSubtitleTextColor,
        onSetSubtitleBackgroundStyle = viewModel::setSubtitleBackgroundStyle,
        onSetSubtitleBackgroundOpacity = viewModel::setSubtitleBackgroundOpacity,
        onSetSubtitlePosition = viewModel::setSubtitlePosition,
        onSetSubtitleDelayMs = viewModel::setSubtitleDelayMs,
        onSetDefaultSubtitleTrackBehavior = viewModel::setDefaultSubtitleTrackBehavior,
        onSetDefaultLayoutMode = viewModel::setDefaultLayoutMode,
        onSetDefaultSortOption = viewModel::setDefaultSortOption,
        onSetScanOnAppLaunch = viewModel::setScanOnAppLaunch,
        onSetIncludeHiddenFiles = viewModel::setIncludeHiddenFiles,
        onTriggerIncrementalScan = viewModel::triggerIncrementalScan,
        onTriggerFullScan = viewModel::triggerFullScan,
        onCancelScan = viewModel::cancelScan,
        onSetThemeMode = viewModel::setThemeMode,
        onSetAmoledMode = viewModel::setAmoledMode,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSetAccentColor = viewModel::setAccentColor,
        onSetPreferredAudioLanguage = viewModel::setPreferredAudioLanguage,
        onSetAudioBoostPercent = viewModel::setAudioBoostPercent,
        onSetAudioBoostEnabled = viewModel::setAudioBoostEnabled,
        onSetEqualizerEnabled = viewModel::setEqualizerEnabled,
        onSetEqualizerPreset = viewModel::setEqualizerPreset,
        onSetCustomBandLevel = viewModel::setCustomBandLevel,
        onResetCustomBandLevels = viewModel::resetCustomBandLevels,
        onSetAudioDelayMs = viewModel::setAudioDelayMs,
        onSetRememberPerVideoAudioSettings = viewModel::setRememberPerVideoAudioSettings,
        onSetHardwareAcceleration = viewModel::setHardwareAcceleration,
        onSetDebugLogging = viewModel::setDebugLogging,
        onOpenClearHistoryDialog = { viewModel.setClearHistoryDialogOpen(true) },
        onConfirmClearHistory = viewModel::clearPlaybackHistory,
        onDismissClearHistory = { viewModel.setClearHistoryDialogOpen(false) },
        onOpenClearAnalyticsDialog = { viewModel.setClearAnalyticsDialogOpen(true) },
        onConfirmClearAnalytics = viewModel::clearAllAnalytics,
        onDismissClearAnalytics = { viewModel.setClearAnalyticsDialogOpen(false) },
        onOpenAboutDialog = viewModel::setActiveAboutDialog,
        onDismissAboutDialog = { viewModel.setActiveAboutDialog(null) },
        onClearUserMessage = viewModel::clearUserMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToStorageLocations: () -> Unit = {},
    onToggleSection: (SettingSection) -> Unit,
    onSetDefaultPlaybackSpeed: (Float) -> Unit,
    onSetAutoNextEnabled: (Boolean) -> Unit,
    onSetRepeatMode: (RepeatModeSetting) -> Unit,
    onSetResumeBehavior: (ResumeBehavior) -> Unit,
    onSetSeekDurationSeconds: (Int) -> Unit,
    onSetDoubleTapSeekEnabled: (Boolean) -> Unit,
    onSetPressAndHoldSpeed: (Float) -> Unit,
    onSetDefaultDisplayMode: (VideoDisplayMode) -> Unit,
    onSetSubtitlesEnabled: (Boolean) -> Unit,
    onSetPreferredSubtitleLanguage: (String) -> Unit,
    onSetSubtitleTextSize: (SubtitleTextSize) -> Unit = {},
    onSetSubtitleTextColor: (SubtitleTextColor) -> Unit = {},
    onSetSubtitleBackgroundStyle: (SubtitleBackgroundStyle) -> Unit = {},
    onSetSubtitleBackgroundOpacity: (Float) -> Unit = {},
    onSetSubtitlePosition: (SubtitlePosition) -> Unit = {},
    onSetSubtitleDelayMs: (Long) -> Unit = {},
    onSetDefaultSubtitleTrackBehavior: (DefaultSubtitleTrackBehavior) -> Unit = {},
    onSetDefaultLayoutMode: (LibraryLayout) -> Unit,
    onSetDefaultSortOption: (LibrarySort) -> Unit,
    onSetScanOnAppLaunch: (Boolean) -> Unit = {},
    onSetIncludeHiddenFiles: (Boolean) -> Unit = {},
    onTriggerIncrementalScan: () -> Unit = {},
    onTriggerFullScan: () -> Unit = {},
    onCancelScan: () -> Unit = {},
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAmoledMode: (Boolean) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAccentColor: (AccentColor) -> Unit,
    onSetPreferredAudioLanguage: (String) -> Unit,
    onSetAudioBoostPercent: (Int) -> Unit = {},
    onSetAudioBoostEnabled: (Boolean) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onSetEqualizerPreset: (String) -> Unit = {},
    onSetCustomBandLevel: (bandIndex: Int, levelmB: Int) -> Unit = { _, _ -> },
    onResetCustomBandLevels: () -> Unit = {},
    onSetAudioDelayMs: (Long) -> Unit,
    onSetRememberPerVideoAudioSettings: (Boolean) -> Unit = {},
    onSetHardwareAcceleration: (Boolean) -> Unit,
    onSetDebugLogging: (Boolean) -> Unit,
    onOpenClearHistoryDialog: () -> Unit,
    onConfirmClearHistory: () -> Unit,
    onDismissClearHistory: () -> Unit,
    onOpenClearAnalyticsDialog: () -> Unit,
    onConfirmClearAnalytics: () -> Unit,
    onDismissClearAnalytics: () -> Unit,
    onOpenAboutDialog: (AboutDialogType) -> Unit,
    onDismissAboutDialog: () -> Unit,
    onClearUserMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearUserMessage()
        }
    }

    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NexusTopAppBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = NexusTheme.spacing.medium,
                vertical = NexusTheme.spacing.medium
            ),
            verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.medium)
        ) {
            // 1. PLAYBACK
            item(key = "section_playback") {
                SettingSectionCard(
                    title = SettingSection.PLAYBACK.title,
                    subtitle = SettingSection.PLAYBACK.subtitle,
                    icon = Icons.Default.PlayArrow,
                    isExpanded = uiState.expandedSections.contains(SettingSection.PLAYBACK),
                    onToggleExpand = { onToggleSection(SettingSection.PLAYBACK) }
                ) {
                    SettingSelectRow(
                        title = "Default Playback Speed",
                        currentValue = "${uiState.settings.playback.defaultPlaybackSpeed}x",
                        onClick = { activeDialog = ActiveDialog.PLAYBACK_SPEED },
                        description = "Initial speed when starting video playback"
                    )
                    SettingSwitchRow(
                        title = "Auto-play Next Video",
                        checked = uiState.settings.playback.isAutoNextEnabled,
                        onCheckedChange = onSetAutoNextEnabled,
                        description = "Automatically advance to next queue item upon completion"
                    )
                    SettingSelectRow(
                        title = "Repeat Mode",
                        currentValue = uiState.settings.playback.repeatMode.label,
                        onClick = { activeDialog = ActiveDialog.REPEAT_MODE },
                        description = "Default loop behavior for playlists and folders"
                    )
                    SettingSelectRow(
                        title = "Resume Behavior",
                        currentValue = uiState.settings.playback.resumeBehavior.label,
                        onClick = { activeDialog = ActiveDialog.RESUME_BEHAVIOR },
                        description = "How to handle partially watched videos"
                    )
                }
            }

            // 2. PLAYER
            item(key = "section_player") {
                SettingSectionCard(
                    title = SettingSection.PLAYER.title,
                    subtitle = SettingSection.PLAYER.subtitle,
                    icon = Icons.Default.SmartDisplay,
                    isExpanded = uiState.expandedSections.contains(SettingSection.PLAYER),
                    onToggleExpand = { onToggleSection(SettingSection.PLAYER) }
                ) {
                    SettingSelectRow(
                        title = "Seek Duration",
                        currentValue = "${uiState.settings.player.seekDurationSeconds}s",
                        onClick = { activeDialog = ActiveDialog.SEEK_DURATION },
                        description = "Time skipped during forward or backward seeking"
                    )
                    SettingSwitchRow(
                        title = "Double-tap to Seek",
                        checked = uiState.settings.player.isDoubleTapSeekEnabled,
                        onCheckedChange = onSetDoubleTapSeekEnabled,
                        description = "Double-tap edges of screen to skip forward/backward"
                    )
                    SettingSelectRow(
                        title = "Press & Hold Speed",
                        currentValue = "${uiState.settings.player.pressAndHoldSpeed}x",
                        onClick = { activeDialog = ActiveDialog.PRESS_HOLD_SPEED },
                        description = "Temporary playback speed while long-pressing player screen"
                    )
                    SettingSelectRow(
                        title = "Default Display Mode",
                        currentValue = uiState.settings.player.defaultDisplayMode.label,
                        onClick = { activeDialog = ActiveDialog.DISPLAY_MODE },
                        description = "Aspect ratio scaling mode when opening videos"
                    )
                }
            }

            // 3. SUBTITLES
            item(key = "section_subtitles") {
                SubtitleSettingsCard(
                    settings = uiState.settings.subtitles,
                    isExpanded = uiState.expandedSections.contains(SettingSection.SUBTITLES),
                    onToggleExpand = { onToggleSection(SettingSection.SUBTITLES) },
                    onSetSubtitlesEnabled = onSetSubtitlesEnabled,
                    onOpenLanguageDialog = { activeDialog = ActiveDialog.SUBTITLE_LANG },
                    onOpenTrackBehaviorDialog = { activeDialog = ActiveDialog.SUBTITLE_TRACK_BEHAVIOR },
                    onSetSubtitleTextSize = onSetSubtitleTextSize,
                    onSetSubtitleTextColor = onSetSubtitleTextColor,
                    onSetSubtitleBackgroundStyle = onSetSubtitleBackgroundStyle,
                    onSetSubtitleBackgroundOpacity = onSetSubtitleBackgroundOpacity,
                    onSetSubtitlePosition = onSetSubtitlePosition,
                    onSetSubtitleDelayMs = onSetSubtitleDelayMs
                )
            }

            // 4. LIBRARY
            item(key = "section_library") {
                SettingSectionCard(
                    title = SettingSection.LIBRARY.title,
                    subtitle = SettingSection.LIBRARY.subtitle,
                    icon = Icons.Default.VideoLibrary,
                    isExpanded = uiState.expandedSections.contains(SettingSection.LIBRARY),
                    onToggleExpand = { onToggleSection(SettingSection.LIBRARY) }
                ) {
                    SettingSelectRow(
                        title = "Default Layout Mode",
                        currentValue = uiState.settings.library.defaultLayoutMode.label,
                        onClick = { activeDialog = ActiveDialog.LIBRARY_LAYOUT },
                        description = "Arrangement style for media folders and lists"
                    )
                    SettingSelectRow(
                        title = "Default Sorting",
                        currentValue = uiState.settings.library.defaultSortOption.label,
                        onClick = { activeDialog = ActiveDialog.LIBRARY_SORT },
                        description = "Initial ordering criteria for video items"
                    )
                }
            }

            // 5. APPEARANCE
            item(key = "section_appearance") {
                SettingSectionCard(
                    title = SettingSection.APPEARANCE.title,
                    subtitle = SettingSection.APPEARANCE.subtitle,
                    icon = Icons.Default.Palette,
                    isExpanded = uiState.expandedSections.contains(SettingSection.APPEARANCE),
                    onToggleExpand = { onToggleSection(SettingSection.APPEARANCE) }
                ) {
                    SettingSelectRow(
                        title = "Theme Mode",
                        currentValue = uiState.settings.appearance.themeMode.label,
                        onClick = { activeDialog = ActiveDialog.THEME_MODE },
                        description = "Switch between dark, light, or system themes"
                    )
                    SettingSwitchRow(
                        title = "AMOLED True Black",
                        checked = uiState.settings.appearance.useAmoledMode,
                        onCheckedChange = onSetAmoledMode,
                        enabled = uiState.settings.appearance.themeMode != ThemeMode.LIGHT,
                        description = "Pitch-black canvas for OLED power savings and deep contrast in dark mode"
                    )
                    SettingSwitchRow(
                        title = "Dynamic Material You Colors",
                        checked = uiState.settings.appearance.useDynamicColor,
                        onCheckedChange = onSetDynamicColor,
                        description = if (uiState.settings.appearance.accentColor != AccentColor.DEFAULT) {
                            "Overridden by custom accent color below"
                        } else {
                            "Derive accent palette from device wallpaper on supported Android 12+ devices"
                        }
                    )
                    AccentColorPicker(
                        selectedAccent = uiState.settings.appearance.accentColor,
                        onAccentSelected = onSetAccentColor
                    )
                }
            }

            // 6. AUDIO
            item(key = "section_audio") {
                AudioSettingsCard(
                    settings = uiState.settings.audio,
                    isExpanded = uiState.expandedSections.contains(SettingSection.AUDIO),
                    onToggleExpand = { onToggleSection(SettingSection.AUDIO) },
                    onSetAudioBoostPercent = onSetAudioBoostPercent,
                    onSetEqualizerEnabled = onSetEqualizerEnabled,
                    onSetEqualizerPreset = onSetEqualizerPreset,
                    onSetCustomBandLevel = onSetCustomBandLevel,
                    onResetCustomBandLevels = onResetCustomBandLevels,
                    onOpenLanguageDialog = { activeDialog = ActiveDialog.AUDIO_LANG },
                    onSetAudioDelayMs = onSetAudioDelayMs,
                    onSetRememberPerVideoAudioSettings = onSetRememberPerVideoAudioSettings
                )
            }

            // 7. STORAGE
            item(key = "section_storage") {
                StorageSettingsCard(
                    storageSettings = uiState.settings.storage,
                    librarySettings = uiState.settings.library,
                    accessMode = uiState.storageAccessMode,
                    indexedFolderCount = uiState.indexedFolderCount,
                    indexedVideoCount = uiState.indexedVideoCount,
                    scanState = uiState.scanState,
                    isScanning = uiState.isScanning,
                    isExpanded = uiState.expandedSections.contains(SettingSection.STORAGE),
                    onToggleExpand = { onToggleSection(SettingSection.STORAGE) },
                    onNavigateToStorageLocations = onNavigateToStorageLocations,
                    onSetScanOnAppLaunch = onSetScanOnAppLaunch,
                    onSetIncludeHiddenFiles = onSetIncludeHiddenFiles,
                    onTriggerIncrementalScan = onTriggerIncrementalScan,
                    onTriggerFullScan = onTriggerFullScan,
                    onCancelScan = onCancelScan
                )
            }

            // 8. PRIVACY
            item(key = "section_privacy") {
                SettingSectionCard(
                    title = SettingSection.PRIVACY.title,
                    subtitle = SettingSection.PRIVACY.subtitle,
                    icon = Icons.Default.Security,
                    isExpanded = uiState.expandedSections.contains(SettingSection.PRIVACY),
                    onToggleExpand = { onToggleSection(SettingSection.PRIVACY) }
                ) {
                    SettingInfoRow(
                        title = "Local-First Architecture",
                        value = "100% On-Device",
                        description = "All media indexes, playback states, and preferences reside strictly on device"
                    )
                    SettingInfoRow(
                        title = "Account-Free",
                        value = "No Sign-In",
                        description = "No registration, profile creation, or account credentials required"
                    )
                    SettingInfoRow(
                        title = "Cloud-Free",
                        value = "Zero Cloud Sync",
                        description = "No remote servers, cloud backups, or off-device network synchronization"
                    )
                    SettingInfoRow(
                        title = "Telemetry-Free",
                        value = "Zero Tracking",
                        description = "Zero diagnostic telemetry, usage tracking, or advertising SDKs"
                    )

                    VerticalSpacer(NexusTheme.spacing.extraSmall)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    VerticalSpacer(NexusTheme.spacing.extraSmall)

                    SettingActionRow(
                        title = "Clear Watch History",
                        description = "Reset playback progress, resume positions, and completion markers",
                        trailingIcon = Icons.Outlined.DeleteSweep,
                        isDestructive = true,
                        onClick = onOpenClearHistoryDialog
                    )
                    SettingActionRow(
                        title = "Clear All Analytics & History",
                        description = "Reset all watch time statistics, play counts, and history records",
                        trailingIcon = Icons.Outlined.DeleteSweep,
                        isDestructive = true,
                        onClick = onOpenClearAnalyticsDialog
                    )
                }
            }

            // 9. ADVANCED
            item(key = "section_advanced") {
                SettingSectionCard(
                    title = SettingSection.ADVANCED.title,
                    subtitle = SettingSection.ADVANCED.subtitle,
                    icon = Icons.Default.Tune,
                    isExpanded = uiState.expandedSections.contains(SettingSection.ADVANCED),
                    onToggleExpand = { onToggleSection(SettingSection.ADVANCED) }
                ) {
                    SettingSwitchRow(
                        title = "Hardware Acceleration",
                        checked = uiState.settings.advanced.hardwareAcceleration,
                        onCheckedChange = onSetHardwareAcceleration,
                        description = "Prioritize GPU-accelerated video decoding"
                    )
                    SettingSwitchRow(
                        title = "Debug Logging",
                        checked = uiState.settings.advanced.debugLogging,
                        onCheckedChange = onSetDebugLogging,
                        description = "Output verbose ExoPlayer logs to Logcat"
                    )
                }
            }

            // 10. ABOUT
            item(key = "section_about") {
                SettingSectionCard(
                    title = SettingSection.ABOUT.title,
                    subtitle = SettingSection.ABOUT.subtitle,
                    icon = Icons.Default.Info,
                    isExpanded = uiState.expandedSections.contains(SettingSection.ABOUT),
                    onToggleExpand = { onToggleSection(SettingSection.ABOUT) }
                ) {
                    SettingInfoRow(
                        title = "Application",
                        value = uiState.settings.about.appName,
                        description = "Modern, privacy-first offline video player"
                    )
                    SettingInfoRow(
                        title = "Version",
                        value = "${uiState.settings.about.versionName} (${uiState.settings.about.buildType})",
                        description = "Built for Android with Jetpack Compose & Media3"
                    )
                    SettingActionRow(
                        title = "Project Information",
                        description = "Architecture, capabilities, and offline design philosophy",
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        onClick = { onOpenAboutDialog(AboutDialogType.PROJECT_INFO) }
                    )
                    SettingActionRow(
                        title = "Open Source Licenses",
                        description = "Third-party libraries, notices, and attribution",
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        onClick = { onOpenAboutDialog(AboutDialogType.LICENSES) }
                    )
                    SettingActionRow(
                        title = "Acknowledgements",
                        description = "Credits and gratitude to foundational open-source projects",
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        onClick = { onOpenAboutDialog(AboutDialogType.ACKNOWLEDGEMENTS) }
                    )
                }
            }
        }
    }

    // Modal Selection Dialogs
    when (activeDialog) {
        ActiveDialog.PLAYBACK_SPEED -> {
            val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            SettingSelectionDialog(
                title = "Default Playback Speed",
                options = speedOptions,
                selectedOption = uiState.settings.playback.defaultPlaybackSpeed,
                onOptionSelected = onSetDefaultPlaybackSpeed,
                onDismissRequest = { activeDialog = null },
                getLabel = { "${it}x" }
            )
        }
        ActiveDialog.REPEAT_MODE -> {
            SettingSelectionDialog(
                title = "Default Repeat Mode",
                options = RepeatModeSetting.values().toList(),
                selectedOption = uiState.settings.playback.repeatMode,
                onOptionSelected = onSetRepeatMode,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.RESUME_BEHAVIOR -> {
            SettingSelectionDialog(
                title = "Resume Behavior",
                options = ResumeBehavior.values().toList(),
                selectedOption = uiState.settings.playback.resumeBehavior,
                onOptionSelected = onSetResumeBehavior,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.SEEK_DURATION -> {
            val seekOptions = listOf(5, 10, 15, 30, 60)
            SettingSelectionDialog(
                title = "Seek Duration",
                options = seekOptions,
                selectedOption = uiState.settings.player.seekDurationSeconds,
                onOptionSelected = onSetSeekDurationSeconds,
                onDismissRequest = { activeDialog = null },
                getLabel = { "${it} seconds" }
            )
        }
        ActiveDialog.PRESS_HOLD_SPEED -> {
            val speedOptions = listOf(1.5f, 2.0f, 2.5f, 3.0f)
            SettingSelectionDialog(
                title = "Press & Hold Speed",
                options = speedOptions,
                selectedOption = uiState.settings.player.pressAndHoldSpeed,
                onOptionSelected = onSetPressAndHoldSpeed,
                onDismissRequest = { activeDialog = null },
                getLabel = { "${it}x" }
            )
        }
        ActiveDialog.DISPLAY_MODE -> {
            SettingSelectionDialog(
                title = "Default Display Mode",
                options = VideoDisplayMode.values().toList(),
                selectedOption = uiState.settings.player.defaultDisplayMode,
                onOptionSelected = onSetDefaultDisplayMode,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.SUBTITLE_LANG -> {
            val languages = listOf("Auto", "English", "Spanish", "French", "Japanese", "German", "Hindi")
            SettingSelectionDialog(
                title = "Preferred Subtitle Language",
                options = languages,
                selectedOption = uiState.settings.subtitles.preferredSubtitleLanguage,
                onOptionSelected = onSetPreferredSubtitleLanguage,
                onDismissRequest = { activeDialog = null },
                getLabel = { it }
            )
        }
        ActiveDialog.SUBTITLE_TRACK_BEHAVIOR -> {
            SettingSelectionDialog(
                title = "Default Track Selection Behavior",
                options = DefaultSubtitleTrackBehavior.entries,
                selectedOption = uiState.settings.subtitles.defaultTrackBehavior,
                onOptionSelected = onSetDefaultSubtitleTrackBehavior,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.LIBRARY_LAYOUT -> {
            SettingSelectionDialog(
                title = "Default Layout Mode",
                options = LibraryLayout.values().toList(),
                selectedOption = uiState.settings.library.defaultLayoutMode,
                onOptionSelected = onSetDefaultLayoutMode,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.LIBRARY_SORT -> {
            SettingSelectionDialog(
                title = "Default Sort Option",
                options = LibrarySort.values().toList(),
                selectedOption = uiState.settings.library.defaultSortOption,
                onOptionSelected = onSetDefaultSortOption,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.THEME_MODE -> {
            SettingSelectionDialog(
                title = "Theme Mode",
                options = ThemeMode.values().toList(),
                selectedOption = uiState.settings.appearance.themeMode,
                onOptionSelected = onSetThemeMode,
                onDismissRequest = { activeDialog = null },
                getLabel = { it.label }
            )
        }
        ActiveDialog.AUDIO_LANG -> {
            val languages = listOf("Auto", "Original", "English", "Spanish", "French", "Japanese", "Hindi")
            SettingSelectionDialog(
                title = "Preferred Audio Language",
                options = languages,
                selectedOption = uiState.settings.audio.preferredAudioLanguage,
                onOptionSelected = onSetPreferredAudioLanguage,
                onDismissRequest = { activeDialog = null },
                getLabel = { it }
            )
        }
        ActiveDialog.AUDIO_DELAY -> {
            val delays = listOf(0L, 50L, 100L, 200L, -50L, -100L, -200L)
            SettingSelectionDialog(
                title = "Audio Delay",
                options = delays,
                selectedOption = uiState.settings.audio.audioDelayMs,
                onOptionSelected = onSetAudioDelayMs,
                onDismissRequest = { activeDialog = null },
                getLabel = { delay ->
                    if (delay > 0) "+$delay ms" else "$delay ms"
                }
            )
        }
        null -> { /* No dialog active */ }
    }

    // Confirmation dialog for destructive playback history reset
    if (uiState.isClearHistoryDialogOpen) {
        ClearHistoryDialog(
            onConfirm = onConfirmClearHistory,
            onDismiss = onDismissClearHistory
        )
    }

    // Confirmation dialog for destructive analytics/history reset
    if (uiState.isClearAnalyticsDialogOpen) {
        ClearAnalyticsDialog(
            onConfirm = onConfirmClearAnalytics,
            onDismiss = onDismissClearAnalytics
        )
    }

    // About Section Dialogs
    when (uiState.activeAboutDialog) {
        AboutDialogType.PROJECT_INFO -> {
            ProjectInfoDialog(onDismiss = onDismissAboutDialog)
        }
        AboutDialogType.LICENSES -> {
            LicensesDialog(onDismiss = onDismissAboutDialog)
        }
        AboutDialogType.ACKNOWLEDGEMENTS -> {
            AcknowledgementsDialog(onDismiss = onDismissAboutDialog)
        }
        null -> { /* No about dialog active */ }
    }
}
