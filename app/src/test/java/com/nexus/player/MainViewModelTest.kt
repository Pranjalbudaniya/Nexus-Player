package com.nexus.player

import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.AppearanceSettings
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.designsystem.theme.NexusAccentColor
import com.nexus.player.core.designsystem.theme.NexusThemeMode
import com.nexus.player.core.navigation.HomeRoute
import com.nexus.player.core.navigation.OnboardingRoute
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startupTriggersScanWhenOnboardedAndAccessGranted() = runTest {
        val fakeRepo = FakeStorageRepo(
            initialState = StorageAccessState(
                isOnboardingCompleted = true,
                isPermissionGranted = true,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        val fakeOrchestrator = FakeScanOrchestrator()
        val fakeSettings = FakeMainSettingsRepo()

        val viewModel = MainViewModel(
            storageAccessRepository = fakeRepo,
            mediaScanOrchestrator = fakeOrchestrator,
            settingsRepository = fakeSettings
        )

        val state = viewModel.appLaunchState.first { it is AppLaunchState.Ready }
        assertEquals(HomeRoute, (state as AppLaunchState.Ready).startDestination)
        assertEquals(1, fakeOrchestrator.startupScanTriggerCount)
    }

    @Test
    fun startupDoesNotTriggerScanWhenNotOnboarded() = runTest {
        val fakeRepo = FakeStorageRepo(
            initialState = StorageAccessState(
                isOnboardingCompleted = false,
                isPermissionGranted = false,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        val fakeOrchestrator = FakeScanOrchestrator()
        val fakeSettings = FakeMainSettingsRepo()

        val viewModel = MainViewModel(
            storageAccessRepository = fakeRepo,
            mediaScanOrchestrator = fakeOrchestrator,
            settingsRepository = fakeSettings
        )

        val state = viewModel.appLaunchState.first { it is AppLaunchState.Ready }
        assertEquals(OnboardingRoute, (state as AppLaunchState.Ready).startDestination)
        assertEquals(0, fakeOrchestrator.startupScanTriggerCount)
    }

    @Test
    fun themeConfig_emitsTransformedSettingsAppearance() = runTest {
        val fakeRepo = FakeStorageRepo(
            initialState = StorageAccessState(
                isOnboardingCompleted = true,
                isPermissionGranted = true,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        val fakeOrchestrator = FakeScanOrchestrator()
        val fakeSettings = FakeMainSettingsRepo(
            initialSettings = NexusSettings(
                appearance = AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    useAmoledMode = true,
                    useDynamicColor = false,
                    accentColor = AccentColor.BLUE
                )
            )
        )

        val viewModel = MainViewModel(
            storageAccessRepository = fakeRepo,
            mediaScanOrchestrator = fakeOrchestrator,
            settingsRepository = fakeSettings
        )

        val config = viewModel.themeConfig.first { it.themeMode == NexusThemeMode.DARK }
        assertEquals(NexusThemeMode.DARK, config.themeMode)
        assertTrue(config.isAmoled)
        assertFalse(config.dynamicColor)
        assertEquals(NexusAccentColor.BLUE, config.accentColor)
    }
}

private class FakeStorageRepo(initialState: StorageAccessState) : StorageAccessRepository {
    private val state = MutableStateFlow(initialState)
    override val storageAccessState: Flow<StorageAccessState> = state

    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun setStorageAccessMode(mode: StorageAccessMode) {}
    override suspend fun addSelectedFolderUri(uriString: String) {}
    override suspend fun removeSelectedFolderUri(uriString: String) {}
    override suspend fun clearSelectedFolders() {}
    override fun isPermissionGranted(): Boolean = state.value.isPermissionGranted
    override fun getRequiredPermissions(): List<String> = emptyList()
}

private class FakeScanOrchestrator : MediaScanOrchestrator {
    var startupScanTriggerCount = 0
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun triggerStartupScan(): Boolean {
        startupScanTriggerCount++
        return true
    }

    override fun triggerManualScan(): Boolean = true
    override fun cancelScan() {}
}


private class FakeMainSettingsRepo(
    initialSettings: NexusSettings = NexusSettings()
) : SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<NexusSettings> = _settings.asStateFlow()

    override suspend fun setDefaultPlaybackSpeed(speed: Float) {}
    override suspend fun setAutoNextEnabled(enabled: Boolean) {}
    override suspend fun setRepeatMode(mode: RepeatModeSetting) {}
    override suspend fun setResumeBehavior(behavior: ResumeBehavior) {}
    override suspend fun setSeekDurationSeconds(seconds: Int) {}
    override suspend fun setDoubleTapSeekEnabled(enabled: Boolean) {}
    override suspend fun setPressAndHoldSpeed(speed: Float) {}
    override suspend fun setDefaultDisplayMode(mode: VideoDisplayMode) {}
    override suspend fun setSubtitlesEnabled(enabled: Boolean) {}
    override suspend fun setPreferredSubtitleLanguage(language: String) {}
    override suspend fun setSubtitleFontScale(scale: Float) {}
    override suspend fun setDefaultLayoutMode(layout: LibraryLayout) {}
    override suspend fun setDefaultSortOption(sort: LibrarySort) {}
    override suspend fun setScanBehavior(behavior: ScanBehavior) {}
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun setAmoledMode(enabled: Boolean) {}
    override suspend fun setDynamicColor(enabled: Boolean) {}
    override suspend fun setAccentColor(accent: AccentColor) {}
    override suspend fun setPreferredAudioLanguage(language: String) {}
    override suspend fun setAudioBoostEnabled(enabled: Boolean) {}
    override suspend fun setAudioDelayMs(delayMs: Long) {}
    override suspend fun setCacheThumbnailMaxEntries(maxEntries: Int) {}
    override suspend fun setPreserveStagedDeletions(preserve: Boolean) {}
    override suspend fun setHardwareAcceleration(enabled: Boolean) {}
    override suspend fun setDebugLogging(enabled: Boolean) {}
}
