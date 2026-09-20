package com.nexus.player.feature.more.settings

import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.NexusSettings
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        fakeVideoRepository = FakeVideoRepository()

        viewModel = SettingsViewModel(
            settingsRepository = fakeSettingsRepository,
            videoRepository = fakeVideoRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_loadsDefaultSettingsAndPlaybackExpanded() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1.0f, state.settings.playback.defaultPlaybackSpeed)
        assertTrue(state.expandedSections.contains(SettingSection.PLAYBACK))
        assertFalse(state.isClearAnalyticsDialogOpen)
        assertNull(state.userMessage)
    }

    @Test
    fun toggleSectionExpanded_togglesSectionInSet() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Collapse PLAYBACK
        viewModel.toggleSectionExpanded(SettingSection.PLAYBACK)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.expandedSections.contains(SettingSection.PLAYBACK))

        // Expand PLAYER
        viewModel.toggleSectionExpanded(SettingSection.PLAYER)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedSections.contains(SettingSection.PLAYER))
    }

    @Test
    fun updatePlaybackSettings_delegatesToRepository() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.setDefaultPlaybackSpeed(1.5f)
        viewModel.setAutoNextEnabled(false)
        viewModel.setRepeatMode(RepeatModeSetting.ALL)
        viewModel.setResumeBehavior(ResumeBehavior.NEVER)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1.5f, state.settings.playback.defaultPlaybackSpeed)
        assertFalse(state.settings.playback.isAutoNextEnabled)
        assertEquals(RepeatModeSetting.ALL, state.settings.playback.repeatMode)
        assertEquals(ResumeBehavior.NEVER, state.settings.playback.resumeBehavior)
    }

    @Test
    fun updatePlayerAndLibrary_delegatesToRepository() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.setSeekDurationSeconds(30)
        viewModel.setDoubleTapSeekEnabled(false)
        viewModel.setDefaultDisplayMode(VideoDisplayMode.STRETCH)
        viewModel.setDefaultLayoutMode(LibraryLayout.LIST)
        viewModel.setDefaultSortOption(LibrarySort.SIZE_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(30, state.settings.player.seekDurationSeconds)
        assertFalse(state.settings.player.isDoubleTapSeekEnabled)
        assertEquals(VideoDisplayMode.STRETCH, state.settings.player.defaultDisplayMode)
        assertEquals(LibraryLayout.LIST, state.settings.library.defaultLayoutMode)
        assertEquals(LibrarySort.SIZE_DESC, state.settings.library.defaultSortOption)
    }

    @Test
    fun updateAppearanceSettings_delegatesToRepository() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setAmoledMode(true)
        viewModel.setDynamicColor(false)
        viewModel.setAccentColor(AccentColor.ROSE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.DARK, state.settings.appearance.themeMode)
        assertTrue(state.settings.appearance.useAmoledMode)
        assertFalse(state.settings.appearance.useDynamicColor)
        assertEquals(AccentColor.ROSE, state.settings.appearance.accentColor)
    }

    @Test
    fun clearAnalytics_triggersRepositoryResetAndShowsMessage() = testScope.runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.setClearAnalyticsDialogOpen(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isClearAnalyticsDialogOpen)

        viewModel.clearAllAnalytics()
        advanceUntilIdle()

        assertTrue(fakeVideoRepository.clearedAnalyticsAndHistory)
        assertFalse(viewModel.uiState.value.isClearAnalyticsDialogOpen)
        assertNotNull(viewModel.uiState.value.userMessage)

        viewModel.clearUserMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.userMessage)
    }

    // --- Test Doubles ---

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(NexusSettings())
        override val settings = _settings.asStateFlow()

        override suspend fun setDefaultPlaybackSpeed(speed: Float) {
            _settings.value = _settings.value.copy(
                playback = _settings.value.playback.copy(defaultPlaybackSpeed = speed)
            )
        }

        override suspend fun setAutoNextEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                playback = _settings.value.playback.copy(isAutoNextEnabled = enabled)
            )
        }

        override suspend fun setRepeatMode(mode: RepeatModeSetting) {
            _settings.value = _settings.value.copy(
                playback = _settings.value.playback.copy(repeatMode = mode)
            )
        }

        override suspend fun setResumeBehavior(behavior: ResumeBehavior) {
            _settings.value = _settings.value.copy(
                playback = _settings.value.playback.copy(resumeBehavior = behavior)
            )
        }

        override suspend fun setSeekDurationSeconds(seconds: Int) {
            _settings.value = _settings.value.copy(
                player = _settings.value.player.copy(seekDurationSeconds = seconds)
            )
        }

        override suspend fun setDoubleTapSeekEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                player = _settings.value.player.copy(isDoubleTapSeekEnabled = enabled)
            )
        }

        override suspend fun setPressAndHoldSpeed(speed: Float) {
            _settings.value = _settings.value.copy(
                player = _settings.value.player.copy(pressAndHoldSpeed = speed)
            )
        }

        override suspend fun setDefaultDisplayMode(mode: VideoDisplayMode) {
            _settings.value = _settings.value.copy(
                player = _settings.value.player.copy(defaultDisplayMode = mode)
            )
        }

        override suspend fun setSubtitlesEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                subtitles = _settings.value.subtitles.copy(areSubtitlesEnabled = enabled)
            )
        }

        override suspend fun setPreferredSubtitleLanguage(language: String) {
            _settings.value = _settings.value.copy(
                subtitles = _settings.value.subtitles.copy(preferredSubtitleLanguage = language)
            )
        }

        override suspend fun setSubtitleFontScale(scale: Float) {
            _settings.value = _settings.value.copy(
                subtitles = _settings.value.subtitles.copy(fontSizeScale = scale)
            )
        }

        override suspend fun setDefaultLayoutMode(layout: LibraryLayout) {
            _settings.value = _settings.value.copy(
                library = _settings.value.library.copy(defaultLayoutMode = layout)
            )
        }

        override suspend fun setDefaultSortOption(sort: LibrarySort) {
            _settings.value = _settings.value.copy(
                library = _settings.value.library.copy(defaultSortOption = sort)
            )
        }

        override suspend fun setScanBehavior(behavior: ScanBehavior) {
            _settings.value = _settings.value.copy(
                library = _settings.value.library.copy(scanBehavior = behavior)
            )
        }

        override suspend fun setThemeMode(mode: ThemeMode) {
            _settings.value = _settings.value.copy(
                appearance = _settings.value.appearance.copy(themeMode = mode)
            )
        }

        override suspend fun setAmoledMode(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                appearance = _settings.value.appearance.copy(useAmoledMode = enabled)
            )
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                appearance = _settings.value.appearance.copy(useDynamicColor = enabled)
            )
        }

        override suspend fun setAccentColor(accent: AccentColor) {
            _settings.value = _settings.value.copy(
                appearance = _settings.value.appearance.copy(accentColor = accent)
            )
        }

        override suspend fun setPreferredAudioLanguage(language: String) {
            _settings.value = _settings.value.copy(
                audio = _settings.value.audio.copy(preferredAudioLanguage = language)
            )
        }

        override suspend fun setAudioBoostEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                audio = _settings.value.audio.copy(isAudioBoostEnabled = enabled)
            )
        }

        override suspend fun setAudioDelayMs(delayMs: Long) {
            _settings.value = _settings.value.copy(
                audio = _settings.value.audio.copy(audioDelayMs = delayMs)
            )
        }

        override suspend fun setCacheThumbnailMaxEntries(maxEntries: Int) {
            _settings.value = _settings.value.copy(
                storage = _settings.value.storage.copy(cacheThumbnailMaxEntries = maxEntries)
            )
        }

        override suspend fun setPreserveStagedDeletions(preserve: Boolean) {
            _settings.value = _settings.value.copy(
                storage = _settings.value.storage.copy(preserveStagedDeletions = preserve)
            )
        }

        override suspend fun setHardwareAcceleration(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                advanced = _settings.value.advanced.copy(hardwareAcceleration = enabled)
            )
        }

        override suspend fun setDebugLogging(enabled: Boolean) {
            _settings.value = _settings.value.copy(
                advanced = _settings.value.advanced.copy(debugLogging = enabled)
            )
        }
    }

    private class FakeVideoRepository : VideoRepository {
        var clearedAnalyticsAndHistory = false

        override suspend fun clearAllAnalyticsAndHistory() {
            clearedAnalyticsAndHistory = true
        }

        override suspend fun clearAllHistory() {}
        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = flowOf(emptyList())
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getFavoriteVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getFavoriteVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getAllHistoryVideos(): Flow<List<Video>> = flowOf(emptyList())
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = flowOf(emptyList())
        override fun getFolders(): Flow<List<VideoFolder>> = flowOf(emptyList())
        override suspend fun getVideoById(id: String): Video? = null
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = 0
        override suspend fun insertVideo(video: Video): Long = 1L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }
}
