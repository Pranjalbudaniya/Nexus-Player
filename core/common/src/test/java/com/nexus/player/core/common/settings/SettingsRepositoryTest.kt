package com.nexus.player.core.common.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nexus.player.core.common.settings.model.LibraryLayout
import com.nexus.player.core.common.settings.model.LibrarySort
import com.nexus.player.core.common.settings.model.RepeatModeSetting
import com.nexus.player.core.common.settings.model.ResumeBehavior
import com.nexus.player.core.common.settings.model.ScanBehavior
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.settings.model.VideoDisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private var testJob = Job()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        testJob = Job()
        val dataStoreScope = CoroutineScope(testDispatcher + testJob)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFolder.newFile("test_settings_${System.nanoTime()}.preferences_pb") }
        )
        repository = SettingsRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        testJob.cancel()
    }

    @Test
    fun initialSettings_hasExpectedDefaultValues() = runTest(testDispatcher) {
        val settings = repository.settings.first()

        assertEquals(1.0f, settings.playback.defaultPlaybackSpeed)
        assertTrue(settings.playback.isAutoNextEnabled)
        assertEquals(RepeatModeSetting.OFF, settings.playback.repeatMode)
        assertEquals(ResumeBehavior.ALWAYS, settings.playback.resumeBehavior)

        assertEquals(10, settings.player.seekDurationSeconds)
        assertTrue(settings.player.isDoubleTapSeekEnabled)
        assertEquals(2.0f, settings.player.pressAndHoldSpeed)
        assertEquals(VideoDisplayMode.FIT, settings.player.defaultDisplayMode)

        assertTrue(settings.subtitles.areSubtitlesEnabled)
        assertEquals("Auto", settings.subtitles.preferredSubtitleLanguage)

        assertEquals(LibraryLayout.GRID, settings.library.defaultLayoutMode)
        assertEquals(LibrarySort.DATE_ADDED_DESC, settings.library.defaultSortOption)
        assertEquals(ScanBehavior.AUTOMATIC, settings.library.scanBehavior)

        assertEquals(ThemeMode.SYSTEM, settings.appearance.themeMode)
        assertTrue(settings.appearance.useDynamicColor)
    }

    @Test
    fun updatePlaybackSettings_persistsAndEmitsNewValues() = runTest(testDispatcher) {
        repository.setDefaultPlaybackSpeed(1.5f)
        repository.setAutoNextEnabled(false)
        repository.setRepeatMode(RepeatModeSetting.ALL)
        repository.setResumeBehavior(ResumeBehavior.ASK)

        val updated = repository.settings.first()
        assertEquals(1.5f, updated.playback.defaultPlaybackSpeed)
        assertFalse(updated.playback.isAutoNextEnabled)
        assertEquals(RepeatModeSetting.ALL, updated.playback.repeatMode)
        assertEquals(ResumeBehavior.ASK, updated.playback.resumeBehavior)
    }

    @Test
    fun updatePlayerSettings_persistsAndEmitsNewValues() = runTest(testDispatcher) {
        repository.setSeekDurationSeconds(15)
        repository.setDoubleTapSeekEnabled(false)
        repository.setPressAndHoldSpeed(2.5f)
        repository.setDefaultDisplayMode(VideoDisplayMode.CROP)

        val updated = repository.settings.first()
        assertEquals(15, updated.player.seekDurationSeconds)
        assertFalse(updated.player.isDoubleTapSeekEnabled)
        assertEquals(2.5f, updated.player.pressAndHoldSpeed)
        assertEquals(VideoDisplayMode.CROP, updated.player.defaultDisplayMode)
    }

    @Test
    fun updateLibraryAndAppearance_persistsAndEmits() = runTest(testDispatcher) {
        repository.setDefaultLayoutMode(LibraryLayout.LIST)
        repository.setDefaultSortOption(LibrarySort.TITLE_ASC)
        repository.setScanBehavior(ScanBehavior.MANUAL)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setDynamicColor(false)

        val updated = repository.settings.first()
        assertEquals(LibraryLayout.LIST, updated.library.defaultLayoutMode)
        assertEquals(LibrarySort.TITLE_ASC, updated.library.defaultSortOption)
        assertEquals(ScanBehavior.MANUAL, updated.library.scanBehavior)
        assertEquals(ThemeMode.DARK, updated.appearance.themeMode)
        assertFalse(updated.appearance.useDynamicColor)
    }

    @Test
    fun updateSubtitlesAndAudio_persistsAndEmits() = runTest(testDispatcher) {
        repository.setSubtitlesEnabled(false)
        repository.setPreferredSubtitleLanguage("Spanish")
        repository.setSubtitleFontScale(1.25f)
        repository.setPreferredAudioLanguage("Japanese")
        repository.setAudioBoostEnabled(true)
        repository.setAudioDelayMs(100L)

        val updated = repository.settings.first()
        assertFalse(updated.subtitles.areSubtitlesEnabled)
        assertEquals("Spanish", updated.subtitles.preferredSubtitleLanguage)
        assertEquals(1.25f, updated.subtitles.fontSizeScale)
        assertEquals("Japanese", updated.audio.preferredAudioLanguage)
        assertTrue(updated.audio.isAudioBoostEnabled)
        assertEquals(100L, updated.audio.audioDelayMs)
    }
}
