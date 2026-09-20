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
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.more.analytics.component.ClearAnalyticsDialog
import com.nexus.player.feature.more.settings.component.SettingActionRow
import com.nexus.player.feature.more.settings.component.SettingInfoRow
import com.nexus.player.feature.more.settings.component.SettingSectionCard
import com.nexus.player.feature.more.settings.component.SettingSelectRow
import com.nexus.player.feature.more.settings.component.SettingSelectionDialog
import com.nexus.player.feature.more.settings.component.SettingSwitchRow

private enum class ActiveDialog {
    PLAYBACK_SPEED,
    REPEAT_MODE,
    RESUME_BEHAVIOR,
    SEEK_DURATION,
    PRESS_HOLD_SPEED,
    DISPLAY_MODE,
    SUBTITLE_LANG,
    SUBTITLE_SCALE,
    LIBRARY_LAYOUT,
    LIBRARY_SORT,
    SCAN_BEHAVIOR,
    THEME_MODE,
    AUDIO_LANG,
    AUDIO_DELAY
}

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
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
        onSetSubtitleFontScale = viewModel::setSubtitleFontScale,
        onSetDefaultLayoutMode = viewModel::setDefaultLayoutMode,
        onSetDefaultSortOption = viewModel::setDefaultSortOption,
        onSetScanBehavior = viewModel::setScanBehavior,
        onSetThemeMode = viewModel::setThemeMode,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSetPreferredAudioLanguage = viewModel::setPreferredAudioLanguage,
        onSetAudioBoostEnabled = viewModel::setAudioBoostEnabled,
        onSetAudioDelayMs = viewModel::setAudioDelayMs,
        onSetHardwareAcceleration = viewModel::setHardwareAcceleration,
        onSetDebugLogging = viewModel::setDebugLogging,
        onOpenClearAnalyticsDialog = { viewModel.setClearAnalyticsDialogOpen(true) },
        onConfirmClearAnalytics = viewModel::clearAllAnalytics,
        onDismissClearAnalytics = { viewModel.setClearAnalyticsDialogOpen(false) },
        onClearUserMessage = viewModel::clearUserMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
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
    onSetSubtitleFontScale: (Float) -> Unit,
    onSetDefaultLayoutMode: (LibraryLayout) -> Unit,
    onSetDefaultSortOption: (LibrarySort) -> Unit,
    onSetScanBehavior: (ScanBehavior) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetPreferredAudioLanguage: (String) -> Unit,
    onSetAudioBoostEnabled: (Boolean) -> Unit,
    onSetAudioDelayMs: (Long) -> Unit,
    onSetHardwareAcceleration: (Boolean) -> Unit,
    onSetDebugLogging: (Boolean) -> Unit,
    onOpenClearAnalyticsDialog: () -> Unit,
    onConfirmClearAnalytics: () -> Unit,
    onDismissClearAnalytics: () -> Unit,
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
                SettingSectionCard(
                    title = SettingSection.SUBTITLES.title,
                    subtitle = SettingSection.SUBTITLES.subtitle,
                    icon = Icons.Default.Subtitles,
                    isExpanded = uiState.expandedSections.contains(SettingSection.SUBTITLES),
                    onToggleExpand = { onToggleSection(SettingSection.SUBTITLES) }
                ) {
                    SettingSwitchRow(
                        title = "Enable Subtitles",
                        checked = uiState.settings.subtitles.areSubtitlesEnabled,
                        onCheckedChange = onSetSubtitlesEnabled,
                        description = "Render subtitles automatically when tracks exist"
                    )
                    SettingSelectRow(
                        title = "Preferred Language",
                        currentValue = uiState.settings.subtitles.preferredSubtitleLanguage,
                        onClick = { activeDialog = ActiveDialog.SUBTITLE_LANG },
                        description = "Default language track selected for captions"
                    )
                    SettingSelectRow(
                        title = "Font Size Scale",
                        currentValue = "${uiState.settings.subtitles.fontSizeScale}x",
                        onClick = { activeDialog = ActiveDialog.SUBTITLE_SCALE },
                        description = "Relative size of on-screen subtitle captions"
                    )
                }
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
                    SettingSelectRow(
                        title = "Scan Behavior",
                        currentValue = uiState.settings.library.scanBehavior.label,
                        onClick = { activeDialog = ActiveDialog.SCAN_BEHAVIOR },
                        description = "Media library indexing on app launch"
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
                        title = "Dynamic Colors",
                        checked = uiState.settings.appearance.useDynamicColor,
                        onCheckedChange = onSetDynamicColor,
                        description = "Derive accent palette from device wallpaper (Material You)"
                    )
                }
            }

            // 6. AUDIO
            item(key = "section_audio") {
                SettingSectionCard(
                    title = SettingSection.AUDIO.title,
                    subtitle = SettingSection.AUDIO.subtitle,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    isExpanded = uiState.expandedSections.contains(SettingSection.AUDIO),
                    onToggleExpand = { onToggleSection(SettingSection.AUDIO) }
                ) {
                    SettingSelectRow(
                        title = "Preferred Audio Language",
                        currentValue = uiState.settings.audio.preferredAudioLanguage,
                        onClick = { activeDialog = ActiveDialog.AUDIO_LANG },
                        description = "Preferred language when multiple audio streams exist"
                    )
                    SettingSwitchRow(
                        title = "Audio Boost",
                        checked = uiState.settings.audio.isAudioBoostEnabled,
                        onCheckedChange = onSetAudioBoostEnabled,
                        description = "Amplify dialog and quiet soundtracks"
                    )
                    SettingSelectRow(
                        title = "Audio Delay (Sync)",
                        currentValue = "${uiState.settings.audio.audioDelayMs} ms",
                        onClick = { activeDialog = ActiveDialog.AUDIO_DELAY },
                        description = "Offset audio relative to video for Bluetooth latency"
                    )
                }
            }

            // 7. STORAGE
            item(key = "section_storage") {
                SettingSectionCard(
                    title = SettingSection.STORAGE.title,
                    subtitle = SettingSection.STORAGE.subtitle,
                    icon = Icons.Default.Storage,
                    isExpanded = uiState.expandedSections.contains(SettingSection.STORAGE),
                    onToggleExpand = { onToggleSection(SettingSection.STORAGE) }
                ) {
                    SettingInfoRow(
                        title = "Thumbnail Cache Capacity",
                        value = "${uiState.settings.storage.cacheThumbnailMaxEntries} entries",
                        description = "In-memory LRU cache capacity for video thumbnails"
                    )
                    SettingInfoRow(
                        title = "Preserve Staged Deletions",
                        value = if (uiState.settings.storage.preserveStagedDeletions) "Enabled" else "Disabled",
                        description = "Safeguard media files against accidental deletion"
                    )
                }
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
                        title = "Privacy Architecture",
                        value = "100% Offline",
                        description = "Nexus Player never transmits analytics or telemetry off-device."
                    )
                    SettingActionRow(
                        title = "Clear Watch History & Analytics",
                        description = "Reset playback progress, watch counts, and statistics.",
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
                        value = uiState.settings.about.appName
                    )
                    SettingInfoRow(
                        title = "Version",
                        value = "${uiState.settings.about.versionName} (${uiState.settings.about.buildType})"
                    )
                    SettingInfoRow(
                        title = "License",
                        value = uiState.settings.about.license
                    )
                    SettingInfoRow(
                        title = "Security Standard",
                        value = uiState.settings.about.privacyStatus
                    )
                }
            }
        }
    }

    // Modal Selection Dialogs
    when (activeDialog) {
        ActiveDialog.PLAYBACK_SPEED -> {
            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
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
            val seekOptions = listOf(5, 10, 15, 30)
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
        ActiveDialog.SUBTITLE_SCALE -> {
            val scales = listOf(0.8f, 1.0f, 1.25f, 1.5f)
            SettingSelectionDialog(
                title = "Subtitle Font Size",
                options = scales,
                selectedOption = uiState.settings.subtitles.fontSizeScale,
                onOptionSelected = onSetSubtitleFontScale,
                onDismissRequest = { activeDialog = null },
                getLabel = { scale ->
                    when (scale) {
                        0.8f -> "Small (0.8x)"
                        1.0f -> "Normal (1.0x)"
                        1.25f -> "Large (1.25x)"
                        1.5f -> "Extra Large (1.5x)"
                        else -> "${scale}x"
                    }
                }
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
        ActiveDialog.SCAN_BEHAVIOR -> {
            SettingSelectionDialog(
                title = "Scan Behavior",
                options = ScanBehavior.values().toList(),
                selectedOption = uiState.settings.library.scanBehavior,
                onOptionSelected = onSetScanBehavior,
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

    // Confirmation dialog for destructive analytics/history reset
    if (uiState.isClearAnalyticsDialogOpen) {
        ClearAnalyticsDialog(
            onConfirm = onConfirmClearAnalytics,
            onDismiss = onDismissClearAnalytics
        )
    }
}
