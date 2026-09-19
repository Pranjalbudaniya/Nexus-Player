package com.nexus.player.feature.player

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nexus.player.feature.player.component.formatPlaybackSpeed
import com.nexus.player.feature.player.preferences.PlayerPreferencesRepository
import com.nexus.player.feature.player.preferences.PlayerPreferencesRepositoryImpl
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private var testJob = Job()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PlayerPreferencesRepository

    @Before
    fun setUp() {
        testJob = Job()
        val dataStoreScope = CoroutineScope(testDispatcher + testJob)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFolder.newFile("test_prefs_${System.nanoTime()}.preferences_pb") }
        )
        repository = PlayerPreferencesRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        testJob.cancel()
    }

    @Test
    fun playbackSpeed_defaultIsOne() = runTest(testDispatcher) {
        assertEquals(1.0f, repository.playbackSpeed.first())
    }

    @Test
    fun playbackSpeed_updatesAndPersists() = runTest(testDispatcher) {
        repository.setPlaybackSpeed(1.25f)
        assertEquals(1.25f, repository.playbackSpeed.first())

        repository.setPlaybackSpeed(1.35f)
        assertEquals(1.35f, repository.playbackSpeed.first(), 0.001f)
    }

    @Test
    fun seekDurationSeconds_defaultIsTen() = runTest(testDispatcher) {
        assertEquals(10, repository.seekDurationSeconds.first())
    }

    @Test
    fun seekDurationSeconds_updatesAndPersists() = runTest(testDispatcher) {
        repository.setSeekDurationSeconds(15)
        assertEquals(15, repository.seekDurationSeconds.first())

        repository.setSeekDurationSeconds(30)
        assertEquals(30, repository.seekDurationSeconds.first())
    }

    @Test
    fun isAutoNextEnabled_defaultIsFalse() = runTest(testDispatcher) {
        assertFalse(repository.isAutoNextEnabled.first())
    }

    @Test
    fun isAutoNextEnabled_updatesAndPersists() = runTest(testDispatcher) {
        repository.setAutoNextEnabled(true)
        assertTrue(repository.isAutoNextEnabled.first())

        repository.setAutoNextEnabled(false)
        assertFalse(repository.isAutoNextEnabled.first())
    }

    @Test
    fun formatPlaybackSpeed_formatsCorrectly() {
        assertEquals("1×", formatPlaybackSpeed(1.0f))
        assertEquals("0.5×", formatPlaybackSpeed(0.5f))
        assertEquals("0.75×", formatPlaybackSpeed(0.75f))
        assertEquals("1.25×", formatPlaybackSpeed(1.25f))
        assertEquals("1.5×", formatPlaybackSpeed(1.5f))
        assertEquals("1.75×", formatPlaybackSpeed(1.75f))
        assertEquals("2×", formatPlaybackSpeed(2.0f))
        assertEquals("1.35×", formatPlaybackSpeed(1.35f))
        assertEquals("2.5×", formatPlaybackSpeed(2.5f))
        assertEquals("3×", formatPlaybackSpeed(3.0f))
    }
}
