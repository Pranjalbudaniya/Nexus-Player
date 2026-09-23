package com.nexus.player.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.common.settings.model.DefaultSubtitleTrackBehavior
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.ExternalSubtitle
import com.nexus.player.core.playback.model.PlaybackError
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.model.PlayerTrack
import com.nexus.player.core.playback.model.SubtitleAppearance
import com.nexus.player.core.playback.model.SubtitleBackgroundStyle
import com.nexus.player.core.playback.model.SubtitlePosition
import com.nexus.player.core.playback.model.SubtitleTextColor
import com.nexus.player.core.playback.model.SubtitleTextSize
import com.nexus.player.core.playback.model.VideoScaleMode
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.core.playback.queue.RepeatMode
import com.nexus.player.core.playback.repository.SubtitleRepository
import com.nexus.player.core.playback.testing.FakeNexusPlayer
import com.nexus.player.feature.player.preferences.PlayerPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakePlayer: FakeNexusPlayer
    private lateinit var fakeVideoRepository: TestFakeVideoRepository
    private lateinit var fakePreferencesRepository: TestFakePlayerPreferencesRepository
    private lateinit var fakeSubtitleRepository: TestFakeSubtitleRepository
    private var virtualTime = 100_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlayer = FakeNexusPlayer()
        fakeVideoRepository = TestFakeVideoRepository()
        fakePreferencesRepository = TestFakePlayerPreferencesRepository()
        fakeSubtitleRepository = TestFakeSubtitleRepository()
        virtualTime = 100_000L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleVideo(
        id: String = "video_1",
        title: String = "Test Video",
        durationMs: Long = 100_000L,
        playbackPositionMs: Long = 0L,
        playbackPercentage: Float = 0.0f,
        isCompleted: Boolean = false
    ) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/emulated/0/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage/emulated/0/Movies",
        sizeBytes = 50_000_000L,
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        dateAdded = 1000L,
        lastModified = 1000L,
        playbackPositionMs = playbackPositionMs,
        playbackPercentage = playbackPercentage,
        isCompleted = isCompleted
    )

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): PlayerViewModel {
        val viewModel = PlayerViewModel(
            savedStateHandle = savedStateHandle,
            videoRepository = fakeVideoRepository,
            player = fakePlayer,
            playerPreferencesRepository = fakePreferencesRepository,
            subtitleRepository = fakeSubtitleRepository,
            ioDispatcher = testDispatcher,
            appContext = null,
            timeProvider = { virtualTime }
        )
        testScope.backgroundScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }

    @Test
    fun initialLoad_inProgressVideo_restoresSavedPosition() = testScope.runTest {
        val video = createSampleVideo(
            id = "vid_resume",
            playbackPositionMs = 42_000L,
            playbackPercentage = 0.42f
        )
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_resume"))
        val viewModel = createViewModel(savedStateHandle)

        advanceUntilIdle()

        assertEquals(1, fakePlayer.preparedMediaItems.size)
        assertEquals("vid_resume", fakePlayer.preparedMediaItems.first().mediaId)
        assertEquals(listOf(42_000L), fakePlayer.initialPositions)
        assertEquals(1, fakePlayer.playCount)

        val state = viewModel.uiState.value
        assertTrue(state is PlayerUiState.Ready)
        val ready = state as PlayerUiState.Ready
        assertEquals("Test Video", ready.videoTitle)
        assertEquals(42_000L, ready.currentPositionMs)
    }

    @Test
    fun initialLoad_completedVideo_restartsFromBeginning() = testScope.runTest {
        val video = createSampleVideo(
            id = "vid_completed",
            playbackPositionMs = 96_000L,
            playbackPercentage = 0.96f
        )
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_completed"))
        val viewModel = createViewModel(savedStateHandle)

        advanceUntilIdle()

        // >= 95% completion should restart at 0
        assertEquals(listOf(0L), fakePlayer.initialPositions)
        val state = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(0L, state.currentPositionMs)
    }

    @Test
    fun initialLoad_explicitCompletedVideo_restartsFromBeginningAndResetsStateInRepo() = testScope.runTest {
        val video = createSampleVideo(
            id = "vid_explicit_completed",
            playbackPositionMs = 50_000L,
            playbackPercentage = 0.50f,
            isCompleted = true
        )
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_explicit_completed"))
        val viewModel = createViewModel(savedStateHandle)

        advanceUntilIdle()

        // Marked completed should restart at 0
        assertEquals(listOf(0L), fakePlayer.initialPositions)
        val state = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(0L, state.currentPositionMs)

        // Verify repository state was reset
        val repoVideo = fakeVideoRepository.getVideoById("vid_explicit_completed")
        assertNotNull(repoVideo)
        assertEquals(0L, repoVideo?.playbackPositionMs)
        assertEquals(0.0f, repoVideo?.playbackPercentage ?: 1f, 0.001f)
        assertFalse(repoVideo?.isCompleted ?: true)
    }

    @Test
    fun initialLoad_missingVideo_emitsMissingFileError() = testScope.runTest {
        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "non_existent_id"))
        val viewModel = createViewModel(savedStateHandle)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PlayerUiState.Error)
        val error = state as PlayerUiState.Error
        assertEquals(ErrorCategory.MissingFile, error.category)
        assertFalse(error.canRetry)
    }

    @Test
    fun initialLoad_directUri_preparesDirectly() = testScope.runTest {
        val directUri = "https://example.com/stream/sample.mp4"
        val savedStateHandle = SavedStateHandle(mapOf("videoId" to directUri))
        val viewModel = createViewModel(savedStateHandle)

        advanceUntilIdle()

        assertEquals(1, fakePlayer.preparedMediaItems.size)
        assertEquals(directUri, fakePlayer.preparedMediaItems.first().uri)
        assertEquals(listOf(0L), fakePlayer.initialPositions)
    }

    @Test
    fun togglePlayPause_whenPlaying_pausesAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_1", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_1"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.simulatePositionUpdate(positionMs = 15_000L)
        advanceUntilIdle()

        // Toggle to pause
        viewModel.togglePlayPause()
        advanceUntilIdle()

        assertEquals(1, fakePlayer.pauseCount)
        // Position immediately persisted to repository on pause
        assertEquals(1, fakeVideoRepository.progressUpdates.size)
        val update = fakeVideoRepository.progressUpdates.first()
        assertEquals("vid_1", update.id)
        assertEquals(15_000L, update.positionMs)
        assertEquals(0.15f, update.percentage, 0.001f)
    }

    @Test
    fun seekTo_updatesPositionAndPersistsImmediately() = testScope.runTest {
        val video = createSampleVideo(id = "vid_seek", durationMs = 60_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_seek"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 60_000L)
        advanceUntilIdle()

        viewModel.seekTo(30_000L)
        advanceUntilIdle()

        assertEquals(listOf(30_000L), fakePlayer.seekPositions)
        assertEquals(1, fakeVideoRepository.progressUpdates.size)
        assertEquals(30_000L, fakeVideoRepository.progressUpdates.first().positionMs)
    }

    @Test
    fun controlsVisibility_autoHidesAfterDelayWhilePlaying() = testScope.runTest {
        val video = createSampleVideo(id = "vid_controls")
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_controls"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady()
        fakePlayer.play()
        viewModel.setControlsVisible(true)
        advanceTimeBy(50L)

        var state = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue(state.controlsVisible)

        // Advance past the 3s auto-hide window
        advanceTimeBy(3500L)

        state = viewModel.uiState.value as PlayerUiState.Ready
        assertFalse(state.controlsVisible)
    }

    @Test
    fun progressUpdates_persistsThrottled() = testScope.runTest {
        val video = createSampleVideo(id = "vid_throttle", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_throttle"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.play()
        advanceUntilIdle()

        // Multiple rapid position updates within debounce window (< 3000ms)
        virtualTime += 250L
        fakePlayer.simulatePositionUpdate(250L)
        advanceTimeBy(250L)

        virtualTime += 250L
        fakePlayer.simulatePositionUpdate(500L)
        advanceTimeBy(250L)
        advanceUntilIdle()

        // Should NOT have written to DB yet (only 500ms elapsed)
        assertEquals(0, fakeVideoRepository.progressUpdates.size)

        // Advance virtual time and test time beyond 3000ms debounce interval
        virtualTime += 3000L
        fakePlayer.simulatePositionUpdate(3500L)
        advanceTimeBy(3000L)
        advanceUntilIdle()

        assertEquals(1, fakeVideoRepository.progressUpdates.size)
        assertEquals(3500L, fakeVideoRepository.progressUpdates.first().positionMs)
    }

    @Test
    fun progressUpdates_reaches95Percent_persistsCompletion() = testScope.runTest {
        val video = createSampleVideo(id = "vid_95", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_95"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.play()
        advanceUntilIdle()

        // Jump to 95%
        fakePlayer.simulatePositionUpdate(95_000L)
        advanceUntilIdle()

        // Reaching 95% triggers immediate persistence
        assertEquals(1, fakeVideoRepository.progressUpdates.size)
        val update = fakeVideoRepository.progressUpdates.first()
        assertEquals("vid_95", update.id)
        assertEquals(95_000L, update.positionMs)
        assertEquals(0.95f, update.percentage, 0.001f)
        assertTrue(update.isCompleted)
    }

    @Test
    fun lifecycle_onPause_pausesAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_bg", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_bg"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.play()
        fakePlayer.simulatePositionUpdate(12_000L)
        advanceUntilIdle()

        // Screen goes to background (isChangingConfigurations = false)
        viewModel.onPauseLifecycle(isChangingConfigurations = false)
        advanceUntilIdle()

        assertEquals(1, fakePlayer.pauseCount)
        assertEquals(1, fakeVideoRepository.progressUpdates.size)
        assertEquals(12_000L, fakeVideoRepository.progressUpdates.first().positionMs)
    }

    @Test
    fun lifecycle_configurationChange_doesNotPause() = testScope.runTest {
        val video = createSampleVideo(id = "vid_rot", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_rot"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.play()
        advanceUntilIdle()

        // Orientation rotation: isChangingConfigurations = true
        viewModel.onPauseLifecycle(isChangingConfigurations = true)
        advanceUntilIdle()

        assertEquals(0, fakePlayer.pauseCount)
        assertTrue(fakePlayer.state.value.isPlaying)
    }

    @Test
    fun retry_afterError_reloadsMedia() = testScope.runTest {
        val video = createSampleVideo(id = "vid_err")
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_err"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        fakePlayer.simulateError(
            PlaybackError(
                category = ErrorCategory.NetworkFailure,
                userMessage = "Network timeout"
            )
        )
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertTrue(errorState is PlayerUiState.Error)
        assertTrue((errorState as PlayerUiState.Error).canRetry)

        // Tap Retry
        viewModel.retry()
        advanceUntilIdle()

        // Media re-prepared
        assertEquals(2, fakePlayer.preparedMediaItems.size)
    }

    @Test
    fun setPlaybackSpeed_updatesPlayerAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_speed")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_speed")))
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(1.5f)
        advanceUntilIdle()

        assertEquals(1.5f, fakePlayer.state.value.playbackSpeed)
        assertEquals(1.5f, fakePreferencesRepository.speedFlow.value)
    }

    @Test
    fun setVideoResizeMode_updatesPlayerAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_resize")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_resize")))
        advanceUntilIdle()

        viewModel.setVideoResizeMode(4) // RESIZE_MODE_ZOOM
        advanceUntilIdle()

        assertEquals(listOf(0, 4), fakePlayer.resizeModes)
        assertEquals(4, fakePreferencesRepository.resizeFlow.value)
    }

    @Test
    fun setDecoderMode_updatesPlayerAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_dec")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_dec")))
        advanceUntilIdle()

        viewModel.setDecoderMode(DecoderMode.Software)
        advanceUntilIdle()

        assertEquals(listOf(DecoderMode.Hardware, DecoderMode.Software), fakePlayer.decoderModes)
        assertEquals(DecoderMode.Software, fakePreferencesRepository.decoderFlow.value)
    }

    @Test
    fun toggleMute_togglesMuteOnPlayer() = testScope.runTest {
        val video = createSampleVideo(id = "vid_mute")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_mute")))
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.isMuted)

        viewModel.toggleMute()
        advanceUntilIdle()

        assertTrue(fakePlayer.state.value.isMuted)

        viewModel.toggleMute()
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.isMuted)
    }

    @Test
    fun setPanelOpen_keepsControlsVisibleAndSuppressesAutoHide() = testScope.runTest {
        val video = createSampleVideo(id = "vid_panel")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_panel")))
        advanceUntilIdle()

        fakePlayer.simulateReady()
        fakePlayer.play()
        viewModel.setControlsVisible(true)
        viewModel.setPanelOpen(true)
        advanceUntilIdle()

        // Advance well past the 3s auto-hide threshold
        advanceTimeBy(5000L)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue("Controls must stay visible while panel is open", state.controlsVisible)
        assertTrue(state.isPanelOpen)

        // Now close the panel -> auto-hide timer resumes and hides controls
        viewModel.setPanelOpen(false)
        advanceTimeBy(3500L)
        advanceUntilIdle()

        val stateAfterClose = viewModel.uiState.value as PlayerUiState.Ready
        assertFalse(stateAfterClose.controlsVisible)
        assertFalse(stateAfterClose.isPanelOpen)
    }

    @Test
    fun setSliderDragging_keepsControlsVisibleAndSuppressesAutoHide() = testScope.runTest {
        val video = createSampleVideo(id = "vid_drag")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_drag")))
        advanceUntilIdle()

        fakePlayer.simulateReady()
        fakePlayer.play()
        viewModel.setControlsVisible(true)
        viewModel.setSliderDragging(true)
        advanceUntilIdle()

        advanceTimeBy(5000L)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue("Controls must stay visible while dragging slider", state.controlsVisible)

        // Release dragging
        viewModel.setSliderDragging(false)
        advanceTimeBy(3500L)
        advanceUntilIdle()

        val stateAfterRelease = viewModel.uiState.value as PlayerUiState.Ready
        assertFalse(stateAfterRelease.controlsVisible)
    }

    // =========================================================================
    // Step 18 Audio & Subtitle Track Management Tests
    // =========================================================================

    @Test
    fun selectAudioTrack_updatesPlayerAndPersistsPreference() = testScope.runTest {
        val video = createSampleVideo(id = "vid_audio")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_audio")))
        advanceUntilIdle()

        val audioTrack = PlayerTrack(
            id = "audio_1",
            label = "English",
            language = "eng",
            channelCount = 6,
            codec = "E-AC3"
        )
        fakePlayer.simulateTracks(audioTracks = listOf(audioTrack))
        advanceUntilIdle()

        viewModel.selectAudioTrack("audio_1")
        advanceUntilIdle()

        assertEquals("audio_1", fakePlayer.state.value.selectedAudioTrackId)
        assertEquals("eng", fakePreferencesRepository.preferredAudioLanguageFlow.value)
    }

    @Test
    fun selectSubtitleTrack_updatesPlayerAndPersistsPreference() = testScope.runTest {
        val video = createSampleVideo(id = "vid_sub")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_sub")))
        advanceUntilIdle()

        val subTrack = PlayerTrack(
            id = "sub_1",
            label = "French",
            language = "fra"
        )
        fakePlayer.simulateTracks(subtitleTracks = listOf(subTrack))
        advanceUntilIdle()

        viewModel.selectSubtitleTrack("sub_1")
        advanceUntilIdle()

        assertEquals("sub_1", fakePlayer.state.value.selectedSubtitleTrackId)
        assertTrue(fakePlayer.state.value.areSubtitlesEnabled)
        assertEquals("fra", fakePreferencesRepository.preferredSubtitleLanguageFlow.value)
        assertTrue(fakePreferencesRepository.areSubtitlesEnabledFlow.value)
    }

    @Test
    fun selectSubtitleTrack_null_disablesSubtitles() = testScope.runTest {
        val video = createSampleVideo(id = "vid_sub_off")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_sub_off")))
        advanceUntilIdle()

        viewModel.selectSubtitleTrack(null)
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.areSubtitlesEnabled)
        assertFalse(fakePreferencesRepository.areSubtitlesEnabledFlow.value)
    }

    @Test
    fun setAudioDelayMs_updatesPlayerAndPreferences() = testScope.runTest {
        val video = createSampleVideo(id = "vid_delay")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_delay")))
        advanceUntilIdle()

        viewModel.setAudioDelayMs(250L)
        advanceUntilIdle()

        assertEquals(250L, fakePlayer.state.value.audioDelayMs)
        assertEquals(250L, fakePreferencesRepository.audioDelayFlow.value)
    }

    @Test
    fun setSubtitleDelayMs_updatesPlayerAndPreferences() = testScope.runTest {
        val video = createSampleVideo(id = "vid_sub_delay")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_sub_delay")))
        advanceUntilIdle()

        viewModel.setSubtitleDelayMs(-500L)
        advanceUntilIdle()

        assertEquals(-500L, fakePlayer.state.value.subtitleDelayMs)
        assertEquals(-500L, fakePreferencesRepository.subtitleDelayFlow.value)
    }

    @Test
    fun setSubtitleAppearance_updatesPlayerAndPreferences() = testScope.runTest {
        val video = createSampleVideo(id = "vid_appearance")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_appearance")))
        advanceUntilIdle()

        val appearance = SubtitleAppearance(
            textSize = SubtitleTextSize.Large,
            textColor = SubtitleTextColor.Yellow,
            backgroundStyle = SubtitleBackgroundStyle.Box,
            position = SubtitlePosition.Top
        )
        viewModel.setSubtitleAppearance(appearance)
        advanceUntilIdle()

        assertEquals(appearance, fakePlayer.state.value.subtitleAppearance)
        assertEquals(appearance, fakePreferencesRepository.subtitleAppearanceFlow.value)
    }

    @Test
    fun importExternalSubtitle_addsToPlayerAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_external")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_external")))
        advanceUntilIdle()

        val uri = Uri.parse("content://media/external/subtitles/movie.srt")
        viewModel.importExternalSubtitle(uri)
        advanceUntilIdle()

        assertEquals(1, fakePlayer.state.value.externalSubtitles.size)
        val sub = fakePlayer.state.value.externalSubtitles.first()
        assertEquals(uri.toString(), sub.uri)
        assertEquals("movie.srt", sub.label)
        assertEquals(sub.id, fakePlayer.state.value.selectedSubtitleTrackId)

        val persistedSubs = fakePreferencesRepository.externalSubtitlesMap["vid_external"]?.value
        assertEquals(1, persistedSubs?.size)
        assertEquals(uri.toString(), persistedSubs?.first()?.uri)
    }

    @Test
    fun initialLoad_autoSelectsPreferredAudioAndSubtitleTracks() = testScope.runTest {
        fakePreferencesRepository.setPreferredAudioLanguage("jpn")
        fakePreferencesRepository.setPreferredSubtitleLanguage("eng")
        fakePreferencesRepository.setSubtitlesEnabled(true)

        val video = createSampleVideo(id = "vid_auto")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_auto")))
        advanceUntilIdle()

        val audioEng = PlayerTrack(id = "a_eng", label = "English", language = "eng")
        val audioJpn = PlayerTrack(id = "a_jpn", label = "Japanese", language = "jpn")
        val subEng = PlayerTrack(id = "s_eng", label = "English", language = "eng")
        val subSpa = PlayerTrack(id = "s_spa", label = "Spanish", language = "spa")

        fakePlayer.simulateTracks(
            audioTracks = listOf(audioEng, audioJpn),
            subtitleTracks = listOf(subEng, subSpa)
        )
        advanceUntilIdle()

        assertEquals("a_jpn", fakePlayer.state.value.selectedAudioTrackId)
        assertEquals("s_eng", fakePlayer.state.value.selectedSubtitleTrackId)
    }

    // =========================================================================
    // Step 19 Playback Speed, Seeking & Playback Behavior Tests
    // =========================================================================

    @Test
    fun setPlaybackSpeed_clampsExtremeValues_andPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_speed_clamp")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_speed_clamp")))
        advanceUntilIdle()

        // Setting speed > 3.0f should clamp to 3.0f
        viewModel.setPlaybackSpeed(5.0f)
        advanceUntilIdle()
        assertEquals(3.0f, fakePlayer.state.value.playbackSpeed)
        assertEquals(3.0f, fakePreferencesRepository.speedFlow.value)

        // Setting speed < 0.25f should clamp to 0.25f
        viewModel.setPlaybackSpeed(0.1f)
        advanceUntilIdle()
        assertEquals(0.25f, fakePlayer.state.value.playbackSpeed)
        assertEquals(0.25f, fakePreferencesRepository.speedFlow.value)

        // Setting fine custom speed 1.35f
        viewModel.setPlaybackSpeed(1.35f)
        advanceUntilIdle()
        assertEquals(1.35f, fakePlayer.state.value.playbackSpeed, 0.001f)
        assertEquals(1.35f, fakePreferencesRepository.speedFlow.value, 0.001f)
    }

    @Test
    fun setTemporarySpeedBoost_temporarilyChangesSpeed_restoresPreviousSpeedOnRelease_andDoesNotPersist() = testScope.runTest {
        val video = createSampleVideo(id = "vid_boost")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_boost")))
        advanceUntilIdle()

        // Base speed is 1.0f
        assertEquals(1.0f, fakePlayer.state.value.playbackSpeed)
        assertEquals(1.0f, fakePreferencesRepository.speedFlow.value)

        // Press and hold: boost active
        val boostSpeed = viewModel.setTemporarySpeedBoost(true)
        advanceUntilIdle()
        assertEquals(2.0f, boostSpeed)
        assertEquals(2.0f, fakePlayer.state.value.playbackSpeed)
        // CRITICAL: Preferences repo MUST NOT be modified by temporary boost!
        assertEquals(1.0f, fakePreferencesRepository.speedFlow.value)

        // Release hold: restores previous user speed
        val restoredSpeed = viewModel.setTemporarySpeedBoost(false)
        advanceUntilIdle()
        assertEquals(1.0f, restoredSpeed)
        assertEquals(1.0f, fakePlayer.state.value.playbackSpeed)
        assertEquals(1.0f, fakePreferencesRepository.speedFlow.value)
    }

    @Test
    fun setTemporarySpeedBoost_whenAlreadyAtTwoX_boostsToTwoPointFive() = testScope.runTest {
        val video = createSampleVideo(id = "vid_boost_2x")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_boost_2x")))
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(2.0f)
        advanceUntilIdle()
        assertEquals(2.0f, fakePlayer.state.value.playbackSpeed)

        // Holding when already at 2.0x boosts to 2.5x
        val boostSpeed = viewModel.setTemporarySpeedBoost(true)
        advanceUntilIdle()
        assertEquals(2.5f, boostSpeed)
        assertEquals(2.5f, fakePlayer.state.value.playbackSpeed)
        assertEquals(2.0f, fakePreferencesRepository.speedFlow.value)

        // Releasing restores 2.0x
        val restoredSpeed = viewModel.setTemporarySpeedBoost(false)
        advanceUntilIdle()
        assertEquals(2.0f, restoredSpeed)
        assertEquals(2.0f, fakePlayer.state.value.playbackSpeed)
    }

    @Test
    fun setSeekDurationSeconds_updatesUiStateAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_seek_duration")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_seek_duration")))
        advanceUntilIdle()

        val readyState = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(10, readyState.seekDurationSeconds)

        viewModel.setSeekDurationSeconds(15)
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(15, updatedState.seekDurationSeconds)
        assertEquals(15, fakePreferencesRepository.seekDurationFlow.value)
    }

    @Test
    fun seekRelativeDirection_usesConfiguredSeekDuration_andClamps() = testScope.runTest {
        val video = createSampleVideo(id = "vid_seek_rel", durationMs = 60_000L)
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_seek_rel")))
        advanceUntilIdle()

        fakePlayer.simulateReady(durationMs = 60_000L)
        fakePlayer.simulatePositionUpdate(20_000L)
        advanceUntilIdle()

        // Seek forward by default 10s -> 30_000ms
        viewModel.seekRelativeDirection(1)
        advanceUntilIdle()
        assertEquals(listOf(30_000L), fakePlayer.seekPositions)

        // Set seek duration to 15s and seek backward -> 15_000ms
        viewModel.setSeekDurationSeconds(15)
        advanceUntilIdle()
        fakePlayer.simulatePositionUpdate(30_000L)
        viewModel.seekRelativeDirection(-1)
        advanceUntilIdle()
        assertEquals(listOf(30_000L, 15_000L), fakePlayer.seekPositions)

        // Clamp backward to 0
        fakePlayer.simulatePositionUpdate(5_000L)
        viewModel.seekRelativeDirection(-1)
        advanceUntilIdle()
        assertEquals(listOf(30_000L, 15_000L, 0L), fakePlayer.seekPositions)

        // Clamp forward to duration (60_000L)
        fakePlayer.simulatePositionUpdate(55_000L)
        viewModel.seekRelativeDirection(1)
        advanceUntilIdle()
        assertEquals(listOf(30_000L, 15_000L, 0L, 60_000L), fakePlayer.seekPositions)
    }

    @Test
    fun setAutoNextEnabled_updatesStateAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_auto_next")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_auto_next")))
        advanceUntilIdle()

        val readyState = viewModel.uiState.value as PlayerUiState.Ready
        assertFalse(readyState.isAutoNextEnabled)

        viewModel.setAutoNextEnabled(true)
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue(updatedState.isAutoNextEnabled)
        assertTrue(fakePreferencesRepository.isAutoNextFlow.value)
    }

    @Test
    fun setPressAndHoldSpeedEnabled_updatesAndPersists() = testScope.runTest {
        val video = createSampleVideo(id = "vid_press_hold")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_press_hold")))
        advanceUntilIdle()

        assertTrue(viewModel.isPressAndHoldSpeedEnabled.value)

        viewModel.setPressAndHoldSpeedEnabled(false)
        advanceUntilIdle()

        assertFalse(fakePreferencesRepository.isPressAndHoldSpeedFlow.value)
    }

    @Test
    fun completionBehavior_emitsVideoCompletedEvent_andAvoidsDuplicateRoomWrites() = testScope.runTest {
        val video = createSampleVideo(id = "vid_complete_events", durationMs = 100_000L)
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_complete_events")))

        val events = mutableListOf<PlayerEvent>()
        val job = testScope.backgroundScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            viewModel.playerEvents.collect { events.add(it) }
        }

        advanceUntilIdle()
        fakePlayer.simulateReady(durationMs = 100_000L)
        fakePlayer.play()
        advanceUntilIdle()

        // Jump to 95% completion
        fakePlayer.simulatePositionUpdate(95_000L)
        advanceUntilIdle()

        // Verify VideoCompleted event fired
        assertEquals(1, events.size)
        assertTrue(events.first() is PlayerEvent.VideoCompleted)
        assertEquals("vid_complete_events", (events.first() as PlayerEvent.VideoCompleted).videoId)

        val writesCountAfterFirstCompletion = fakeVideoRepository.progressUpdates.size
        assertEquals(1, writesCountAfterFirstCompletion)

        // Subsequent ticks at 96% and 97% should NOT fire duplicate completion events or write duplicate DB entries
        fakePlayer.simulatePositionUpdate(96_000L)
        fakePlayer.simulatePositionUpdate(97_000L)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(writesCountAfterFirstCompletion, fakeVideoRepository.progressUpdates.size)

        job.cancel()
    }

    @Test
    fun setAudioBoost_updatesControllerAndPreferences_clampedBetween100And200() = testScope.runTest {
        val video = createSampleVideo("vid_boost")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_boost")))
        advanceUntilIdle()

        viewModel.setAudioBoost(150)
        advanceUntilIdle()
        assertEquals(150, fakePlayer.audioEffectsController.boostPercent.value)
        assertEquals(150, fakePreferencesRepository.audioBoostFlow.value)
        val readyState = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(150, readyState.audioBoostPercent)

        // Clamping check
        viewModel.setAudioBoost(250)
        advanceUntilIdle()
        assertEquals(200, fakePlayer.audioEffectsController.boostPercent.value)
        assertEquals(200, fakePreferencesRepository.audioBoostFlow.value)

        viewModel.setAudioBoost(50)
        advanceUntilIdle()
        assertEquals(100, fakePlayer.audioEffectsController.boostPercent.value)
        assertEquals(100, fakePreferencesRepository.audioBoostFlow.value)
    }

    @Test
    fun setEqualizer_updatesControllerAndPreferences() = testScope.runTest {
        val video = createSampleVideo("vid_eq")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_eq")))
        advanceUntilIdle()

        viewModel.setEqualizerEnabled(true)
        advanceUntilIdle()
        assertTrue(fakePlayer.audioEffectsController.isEqualizerEnabled.value)
        assertTrue(fakePreferencesRepository.isEqualizerEnabledFlow.value)
        assertTrue((viewModel.uiState.value as PlayerUiState.Ready).isEqualizerEnabled)

        viewModel.setEqualizerPreset("Rock")
        advanceUntilIdle()
        assertEquals("Rock", fakePlayer.audioEffectsController.currentPreset.value)
        assertEquals("Rock", fakePreferencesRepository.equalizerPresetFlow.value)
        assertEquals("Rock", (viewModel.uiState.value as PlayerUiState.Ready).equalizerPreset)

        viewModel.setEqualizerBandLevel(0, 500)
        advanceUntilIdle()
        assertEquals(500, fakePlayer.audioEffectsController.bandLevels.value[0])
    }

    @Test
    fun volumeAndBrightness_updatesReadyUiState() = testScope.runTest {
        val video = createSampleVideo("vid_vol_bright")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_vol_bright")))
        advanceUntilIdle()

        viewModel.setVolumePercent(75)
        viewModel.setBrightnessPercent(80)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(75, state.volumePercent)
        assertEquals(80, state.brightnessPercent)
    }

    @Test
    fun sleepTimer_countdownTicks_pausesPlaybackAtZero_emitsCompletedEvent() = testScope.runTest {
        val video = createSampleVideo("vid_timer")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_timer")))
        advanceUntilIdle()

        val events = mutableListOf<PlayerEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.playerEvents.collect { events.add(it) }
        }

        fakePlayer.play()
        assertTrue(fakePlayer.state.value.isPlaying)

        viewModel.startSleepTimer(1) // 1 minute = 60 seconds
        testScheduler.runCurrent()
        val stateWithTimer = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals(60L, stateWithTimer.sleepTimerRemainingSeconds)

        // Advance 30 seconds
        testScheduler.advanceTimeBy(30_000L)
        testScheduler.runCurrent()
        assertEquals(30L, (viewModel.uiState.value as PlayerUiState.Ready).sleepTimerRemainingSeconds)

        // Advance remaining 30 seconds to reach 0
        testScheduler.advanceTimeBy(30_000L)
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.isPlaying)
        assertEquals(null, (viewModel.uiState.value as PlayerUiState.Ready).sleepTimerRemainingSeconds)
        assertTrue(events.any { it is PlayerEvent.SleepTimerCompleted && it.videoId == "vid_timer" })

        job.cancel()
    }

    @Test
    fun sleepTimer_cancel_clearsCountdownState() = testScope.runTest {
        val video = createSampleVideo("vid_timer_cancel")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_timer_cancel")))
        advanceUntilIdle()

        viewModel.startSleepTimer(10)
        testScheduler.runCurrent()
        assertEquals(600L, (viewModel.uiState.value as PlayerUiState.Ready).sleepTimerRemainingSeconds)

        viewModel.cancelSleepTimer()
        testScheduler.runCurrent()
        assertEquals(null, (viewModel.uiState.value as PlayerUiState.Ready).sleepTimerRemainingSeconds)
    }

    @Test
    fun takeScreenshot_nullBitmap_emitsScreenshotFailed() = testScope.runTest {
        val video = createSampleVideo("vid_snap")
        fakeVideoRepository.addVideo(video)
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "vid_snap")))
        advanceUntilIdle()

        val events = mutableListOf<PlayerEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.playerEvents.collect { events.add(it) }
        }

        // Frame capture returns null by default in fakePlayer
        viewModel.takeScreenshot()
        advanceUntilIdle()

        assertTrue(events.any { it is PlayerEvent.ScreenshotFailed })

        job.cancel()
    }

    @Test
    fun cycleVideoScaleMode_cyclesThroughAllFiveModesInOrder() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Order: Fit (0) -> Fill (1) -> Crop (2) -> Stretch (3) -> Original (4) -> Fit (0)
        assertEquals(VideoScaleMode.Fit, fakePlayer.state.value.scaleMode)

        val mode1 = viewModel.cycleVideoScaleMode()
        assertEquals(VideoScaleMode.Fill, mode1)
        assertEquals(VideoScaleMode.Fill, fakePlayer.state.value.scaleMode)

        val mode2 = viewModel.cycleVideoScaleMode()
        assertEquals(VideoScaleMode.Crop, mode2)
        assertEquals(VideoScaleMode.Crop, fakePlayer.state.value.scaleMode)

        val mode3 = viewModel.cycleVideoScaleMode()
        assertEquals(VideoScaleMode.Stretch, mode3)
        assertEquals(VideoScaleMode.Stretch, fakePlayer.state.value.scaleMode)

        val mode4 = viewModel.cycleVideoScaleMode()
        assertEquals(VideoScaleMode.Original, mode4)
        assertEquals(VideoScaleMode.Original, fakePlayer.state.value.scaleMode)

        val mode5 = viewModel.cycleVideoScaleMode()
        assertEquals(VideoScaleMode.Fit, mode5)
        assertEquals(VideoScaleMode.Fit, fakePlayer.state.value.scaleMode)
    }

    @Test
    fun setVideoScaleMode_persistsPerVideoIdAndRestoresOnReopen() = testScope.runTest {
        val video1 = createSampleVideo(id = "vid_1", title = "Video One")
        val video2 = createSampleVideo(id = "vid_2", title = "Video Two")
        fakeVideoRepository.addVideo(video1)
        fakeVideoRepository.addVideo(video2)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Load video 1 and set mode to Crop
        viewModel.loadMedia("vid_1")
        advanceUntilIdle()
        viewModel.setVideoScaleMode(VideoScaleMode.Crop)
        advanceUntilIdle()
        assertEquals(VideoScaleMode.Crop, fakePlayer.state.value.scaleMode)

        // Load video 2 - verify it does NOT inherit video 1's mode, defaults to Fit
        viewModel.loadMedia("vid_2")
        advanceUntilIdle()
        assertEquals(VideoScaleMode.Fit, fakePlayer.state.value.scaleMode)

        // Set video 2 to Stretch
        viewModel.setVideoScaleMode(VideoScaleMode.Stretch)
        advanceUntilIdle()
        assertEquals(VideoScaleMode.Stretch, fakePlayer.state.value.scaleMode)

        // Reopen video 1 - verify previous Crop mode is restored
        viewModel.loadMedia("vid_1")
        advanceUntilIdle()
        assertEquals(VideoScaleMode.Crop, fakePlayer.state.value.scaleMode)

        // Reopen video 2 - verify previous Stretch mode is restored
        viewModel.loadMedia("vid_2")
        advanceUntilIdle()
        assertEquals(VideoScaleMode.Stretch, fakePlayer.state.value.scaleMode)
    }

    @Test
    fun onZoomChange_clampsBetweenSensibleLimits() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1.0f, viewModel.zoom.value)

        // Pinch inward below 1.0f -> clamped to 1.0f
        viewModel.onZoomChange(0.5f)
        assertEquals(1.0f, viewModel.zoom.value)

        // Pinch outward to 2.5x -> smooth scaling
        viewModel.onZoomChange(2.5f)
        assertEquals(2.5f, viewModel.zoom.value)

        // Pinch outward beyond 4.0x -> clamped to 4.0f (no infinite zoom)
        viewModel.onZoomChange(2.0f) // 2.5 * 2 = 5.0 -> clamped to 4.0f
        assertEquals(4.0f, viewModel.zoom.value)

        // Reset zoom
        viewModel.resetZoom()
        assertEquals(1.0f, viewModel.zoom.value)
        assertEquals(0f, viewModel.panOffsetX.value)
        assertEquals(0f, viewModel.panOffsetY.value)
    }

    @Test
    fun onPanChange_whenZoomedIn_clampsToViewportBounds() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Panning when zoom == 1.0 does nothing
        viewModel.onPanChange(100f, 100f, 1000f, 500f)
        assertEquals(0f, viewModel.panOffsetX.value)
        assertEquals(0f, viewModel.panOffsetY.value)

        // Zoom to 2.0x
        viewModel.onZoomChange(2.0f)
        assertEquals(2.0f, viewModel.zoom.value)

        // Container: width 1000, height 500
        // maxPanX = (1000 * (2 - 1)) / 2 = 500
        // maxPanY = (500 * (2 - 1)) / 2 = 250
        viewModel.onPanChange(1000f, 500f, 1000f, 500f)
        assertEquals(500f, viewModel.panOffsetX.value)
        assertEquals(250f, viewModel.panOffsetY.value)

        // Pan to opposite extreme
        viewModel.onPanChange(-2000f, -1000f, 1000f, 500f)
        assertEquals(-500f, viewModel.panOffsetX.value)
        assertEquals(-250f, viewModel.panOffsetY.value)
    }

    @Test
    fun loadMedia_resetsZoomAndPan() = testScope.runTest {
        val video1 = createSampleVideo(id = "v1")
        val video2 = createSampleVideo(id = "v2")
        fakeVideoRepository.addVideo(video1)
        fakeVideoRepository.addVideo(video2)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMedia("v1")
        advanceUntilIdle()

        viewModel.onZoomChange(3.0f)
        viewModel.onPanChange(50f, 50f, 1000f, 500f)
        assertEquals(3.0f, viewModel.zoom.value)

        // Loading new media resets zoom and pan
        viewModel.loadMedia("v2")
        advanceUntilIdle()
        assertEquals(1.0f, viewModel.zoom.value)
        assertEquals(0f, viewModel.panOffsetX.value)
        assertEquals(0f, viewModel.panOffsetY.value)
    }

    @Test
    fun changingScaleMode_doesNotResetManualZoom() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onZoomChange(2.2f)
        assertEquals(2.2f, viewModel.zoom.value)

        // Cycling scale mode keeps zoom separate and intact
        viewModel.cycleVideoScaleMode()
        advanceUntilIdle()
        assertEquals(2.2f, viewModel.zoom.value)
    }

    @Test
    fun queueNavigation_nextAndPrevious() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1")
        val v2 = createSampleVideo(id = "q_vid_2")
        val v3 = createSampleVideo(id = "q_vid_3")
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)
        fakeVideoRepository.addVideo(v3)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_1")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2", "q_vid_3"), "q_vid_1")
        advanceUntilIdle()

        // Move to next
        viewModel.onNextClick()
        advanceUntilIdle()
        assertEquals("q_vid_2", viewModel.video.value?.id)
        assertEquals("q_vid_2", viewModel.playbackQueueManager.queueState.value.currentVideoId)

        // Previous when at 1000ms (<=3000ms) goes back to q_vid_1
        fakePlayer.simulatePositionUpdate(1000L)
        viewModel.onPreviousClick()
        advanceUntilIdle()
        assertEquals("q_vid_1", viewModel.video.value?.id)
    }

    @Test
    fun queueNavigation_previousPastThreshold_restartsCurrentVideo() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1")
        val v2 = createSampleVideo(id = "q_vid_2")
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_2")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2"), "q_vid_2")
        advanceUntilIdle()

        // Position at 5000ms (> 3000ms threshold)
        fakePlayer.simulatePositionUpdate(5000L)
        fakePlayer.play()
        advanceUntilIdle()

        viewModel.onPreviousClick()
        advanceUntilIdle()

        // Still on q_vid_2, but seeked to 0
        assertEquals("q_vid_2", viewModel.video.value?.id)
        assertEquals(0L, fakePlayer.state.value.currentPosition)
    }

    @Test
    fun autoNext_whenEnabled_playsNextVideoOnCompletion() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1", durationMs = 10_000L)
        val v2 = createSampleVideo(id = "q_vid_2", durationMs = 20_000L)
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_1")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2"), "q_vid_1")
        viewModel.setAutoNextEnabled(true)
        advanceUntilIdle()

        // Simulate video ending
        fakePlayer.simulateEnded()
        advanceUntilIdle()

        // Automatically loaded and playing v2
        assertEquals("q_vid_2", viewModel.video.value?.id)
        assertEquals(1, viewModel.playbackQueueManager.queueState.value.currentIndex)
    }

    @Test
    fun autoNext_whenDisabled_stopsAtEndOfVideo() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1", durationMs = 10_000L)
        val v2 = createSampleVideo(id = "q_vid_2", durationMs = 20_000L)
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_1")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2"), "q_vid_1")
        viewModel.setAutoNextEnabled(false)
        advanceUntilIdle()

        // Simulate video ending
        fakePlayer.simulateEnded()
        advanceUntilIdle()

        // Stays on v1
        assertEquals("q_vid_1", viewModel.video.value?.id)
        assertEquals(0, viewModel.playbackQueueManager.queueState.value.currentIndex)
    }

    @Test
    fun repeatOne_replaysCurrentVideoOnEnd() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1", durationMs = 10_000L)
        val v2 = createSampleVideo(id = "q_vid_2", durationMs = 20_000L)
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_1")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2"), "q_vid_1")
        viewModel.setRepeatMode(RepeatMode.REPEAT_ONE)
        advanceUntilIdle()

        fakePlayer.simulateEnded()
        advanceUntilIdle()

        // Still on v1, seeked to 0
        assertEquals("q_vid_1", viewModel.video.value?.id)
        assertEquals(0L, fakePlayer.state.value.currentPosition)
    }

    @Test
    fun repeatAll_loopsToFirstVideoOnEnd() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1", durationMs = 10_000L)
        val v2 = createSampleVideo(id = "q_vid_2", durationMs = 20_000L)
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_2")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2"), "q_vid_2")
        viewModel.setRepeatMode(RepeatMode.REPEAT_ALL)
        advanceUntilIdle()

        fakePlayer.simulateEnded()
        advanceUntilIdle()

        // Wrapped to v1
        assertEquals("q_vid_1", viewModel.video.value?.id)
        assertEquals(0, viewModel.playbackQueueManager.queueState.value.currentIndex)
    }

    @Test
    fun shuffle_togglesWithoutRestartingCurrentVideo() = testScope.runTest {
        val v1 = createSampleVideo(id = "q_vid_1")
        val v2 = createSampleVideo(id = "q_vid_2")
        val v3 = createSampleVideo(id = "q_vid_3")
        fakeVideoRepository.addVideo(v1)
        fakeVideoRepository.addVideo(v2)
        fakeVideoRepository.addVideo(v3)

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "q_vid_2")))
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_1", "q_vid_2", "q_vid_3"), "q_vid_2")
        advanceUntilIdle()

        val enabled = viewModel.toggleShuffle()
        assertTrue(enabled)
        advanceUntilIdle()
        val ready = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue(ready.isShuffleEnabled)
        assertEquals("q_vid_2", viewModel.video.value?.id)

        val disabled = viewModel.toggleShuffle()
        assertFalse(disabled)
        advanceUntilIdle()
        val readyRestored = viewModel.uiState.value as PlayerUiState.Ready
        assertFalse(readyRestored.isShuffleEnabled)
        assertEquals("q_vid_2", viewModel.video.value?.id)
    }

    @Test
    fun invalidQueueItem_skippedSafelyWhenAutoNextEnabled() = testScope.runTest {
        // q_vid_missing is NOT in fakeVideoRepository, q_vid_valid IS
        val vValid = createSampleVideo(id = "q_vid_valid")
        fakeVideoRepository.addVideo(vValid)

        val viewModel = createViewModel(SavedStateHandle())
        viewModel.playbackQueueManager.setQueue(listOf("q_vid_missing", "q_vid_valid"), "q_vid_missing")
        viewModel.setAutoNextEnabled(true)
        advanceUntilIdle()

        viewModel.loadMedia("q_vid_missing")
        advanceUntilIdle()

        // Auto-skipped to q_vid_valid!
        assertEquals("q_vid_valid", viewModel.video.value?.id)
    }

    @Test
    fun subtitleAutoSelection_forcedOnly_selectsForcedTrack() = testScope.runTest {
        val video = createSampleVideo(id = "sub_vid_1")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.areSubtitlesEnabledFlow.value = true
        fakePreferencesRepository.defaultSubtitleTrackBehaviorFlow.value = DefaultSubtitleTrackBehavior.FORCED_ONLY

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "sub_vid_1"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val subNormal = PlayerTrack(id = "sub_norm", label = "English", language = "eng", isForced = false)
        val subForced = PlayerTrack(id = "sub_frc", label = "English [Forced]", language = "eng", isForced = true)
        fakePlayer.simulateTracks(subtitleTracks = listOf(subNormal, subForced))
        advanceUntilIdle()

        assertEquals("sub_frc", fakePlayer.state.value.selectedSubtitleTrackId)
        assertTrue(fakePlayer.state.value.areSubtitlesEnabled)
    }

    @Test
    fun subtitleAutoSelection_subtitlesDisabled_disablesSubtitlesInPlayer() = testScope.runTest {
        val video = createSampleVideo(id = "sub_vid_2")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.areSubtitlesEnabledFlow.value = false

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "sub_vid_2"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val subTrack = PlayerTrack(id = "sub_1", label = "English", language = "eng")
        fakePlayer.simulateTracks(subtitleTracks = listOf(subTrack))
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.areSubtitlesEnabled)
    }

    @Test
    fun subtitleAutoSelection_trackBehaviorOff_disablesSubtitlesInPlayer() = testScope.runTest {
        val video = createSampleVideo(id = "sub_vid_3")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.areSubtitlesEnabledFlow.value = true
        fakePreferencesRepository.defaultSubtitleTrackBehaviorFlow.value = DefaultSubtitleTrackBehavior.OFF

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "sub_vid_3"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val subTrack = PlayerTrack(id = "sub_1", label = "English", language = "eng")
        fakePlayer.simulateTracks(subtitleTracks = listOf(subTrack))
        advanceUntilIdle()

        assertFalse(fakePlayer.state.value.areSubtitlesEnabled)
    }

    @Test
    fun subtitleAutoSelection_firstAvailable_selectsFirstTrackWhenNoLangMatch() = testScope.runTest {
        val video = createSampleVideo(id = "sub_vid_4")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.areSubtitlesEnabledFlow.value = true
        fakePreferencesRepository.defaultSubtitleTrackBehaviorFlow.value = DefaultSubtitleTrackBehavior.FIRST_AVAILABLE
        fakePreferencesRepository.preferredSubtitleLanguageFlow.value = "Japanese"

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "sub_vid_4"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val subFr = PlayerTrack(id = "sub_fr", label = "French", language = "fra")
        val subDe = PlayerTrack(id = "sub_de", label = "German", language = "deu")
        fakePlayer.simulateTracks(subtitleTracks = listOf(subFr, subDe))
        advanceUntilIdle()

        assertEquals("sub_fr", fakePlayer.state.value.selectedSubtitleTrackId)
        assertTrue(fakePlayer.state.value.areSubtitlesEnabled)
    }

    @Test
    fun audioSettings_perVideoAudioDelayRestoredWhenRememberEnabled() = testScope.runTest {
        val video = createSampleVideo(id = "vid_audio_pv")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.rememberPerVideoAudioSettingsFlow.value = true
        fakePreferencesRepository.audioDelayFlow.value = 0L
        fakePreferencesRepository.setVideoAudioDelayMs("vid_audio_pv", 150L)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_audio_pv"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertEquals(150L, fakePlayer.state.value.audioDelayMs)
    }

    @Test
    fun audioSettings_globalAudioDelayUsedWhenRememberDisabled() = testScope.runTest {
        val video = createSampleVideo(id = "vid_audio_global")
        fakeVideoRepository.addVideo(video)
        fakePreferencesRepository.rememberPerVideoAudioSettingsFlow.value = false
        fakePreferencesRepository.audioDelayFlow.value = 50L
        fakePreferencesRepository.setVideoAudioDelayMs("vid_audio_global", 200L)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_audio_global"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertEquals(50L, fakePlayer.state.value.audioDelayMs)
    }

    @Test
    fun audioSettings_setEqualizerBandLevelPersistsCustomPresetAndLevel() = testScope.runTest {
        val video = createSampleVideo(id = "vid_audio_eq")
        fakeVideoRepository.addVideo(video)

        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "vid_audio_eq"))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.setEqualizerBandLevel(0, 450)
        advanceUntilIdle()

        assertEquals(450, fakePlayer.audioEffectsController.bandLevels.value[0])
        assertEquals(450, fakePreferencesRepository.customBandLevelsFlow.value[0])
        assertEquals("Custom", fakePreferencesRepository.equalizerPresetFlow.value)
    }

    // =========================================================================
    // Network Video Playback Tests
    // =========================================================================

    @Test
    fun networkPlayback_cleanNetworkTitle_extractsFilenameOrHost() {
        assertEquals("video.mp4", PlayerViewModel.cleanNetworkTitle("http://example.com/videos/video.mp4"))
        assertEquals("bunny.mkv", PlayerViewModel.cleanNetworkTitle("https://cdn.example.org/stream/bunny.mkv?token=123&exp=456#part1"))
        assertEquals("movie clip.mp4", PlayerViewModel.cleanNetworkTitle("https://example.com/movie%20clip.mp4"))
        assertEquals("stream.live.com", PlayerViewModel.cleanNetworkTitle("https://stream.live.com/"))
    }

    @Test
    fun networkPlayback_sanitizeMediaId_decodesUrlEncodedStrings() {
        assertEquals("https://example.com/video.mp4", PlayerViewModel.sanitizeMediaId("https%3A%2F%2Fexample.com%2Fvideo.mp4"))
        assertEquals("http://example.com/stream", PlayerViewModel.sanitizeMediaId("http%3A%2F%2Fexample.com%2Fstream"))
        assertEquals("local_video_123", PlayerViewModel.sanitizeMediaId("local_video_123"))
    }

    @Test
    fun networkPlayback_validHttpUrl_preparesPlayerAndSetsReadyState() = testScope.runTest {
        val viewModel = createViewModel(SavedStateHandle())
        val url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        viewModel.loadMedia(url)
        advanceUntilIdle()

        assertEquals(1, fakePlayer.preparedMediaItems.size)
        val prepared = fakePlayer.preparedMediaItems.last()
        assertEquals(url, prepared.mediaId)
        assertEquals(url, prepared.uri)
        assertEquals("BigBuckBunny.mp4", prepared.title)
        assertEquals(listOf(0L), fakePlayer.initialPositions)
        assertEquals(1, fakePlayer.playCount)

        // Ensure no local database entity is set or persisted
        assertEquals(null, viewModel.video.value)
        assertTrue(fakeVideoRepository.progressUpdates.isEmpty())

        // Verify Ready state has isNetworkMedia flag set to true
        val ready = viewModel.uiState.value as PlayerUiState.Ready
        assertTrue(ready.isNetworkMedia)
        assertEquals("BigBuckBunny.mp4", ready.videoTitle)
        assertEquals(url, ready.videoId)
    }

    @Test
    fun networkPlayback_validHttpsUrlWithQueryParams_extractsCleanTitle() = testScope.runTest {
        val viewModel = createViewModel(SavedStateHandle())
        val url = "https://cdn.example.org/path/nature_documentary.mkv?auth_token=xyz987&signature=abc#t=30"

        viewModel.loadMedia(url)
        advanceUntilIdle()

        assertEquals(1, fakePlayer.preparedMediaItems.size)
        assertEquals("nature_documentary.mkv", fakePlayer.preparedMediaItems.last().title)
        val ready = viewModel.uiState.value as PlayerUiState.Ready
        assertEquals("nature_documentary.mkv", ready.videoTitle)
        assertTrue(ready.isNetworkMedia)
    }

    @Test
    fun networkPlayback_urlEncodedNavRoute_decodesProperly() = testScope.runTest {
        val rawEncoded = "https%3A%2F%2Fexample.com%2Fstream%2Fclip.mkv"
        val expectedDecoded = "https://example.com/stream/clip.mkv"

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to rawEncoded)))
        advanceUntilIdle()

        assertEquals(1, fakePlayer.preparedMediaItems.size)
        assertEquals(expectedDecoded, fakePlayer.preparedMediaItems.last().uri)
        assertEquals("clip.mkv", fakePlayer.preparedMediaItems.last().title)
    }

    @Test
    fun networkPlayback_unsupportedScheme_setsInvalidUrlError() = testScope.runTest {
        val viewModel = createViewModel(SavedStateHandle())
        viewModel.loadMedia("ftp://files.example.com/unsupported_stream.mp4")
        advanceUntilIdle()

        assertTrue(fakePlayer.preparedMediaItems.isEmpty())
        val errorState = viewModel.uiState.value as PlayerUiState.Error
        assertEquals(ErrorCategory.InvalidUrl, errorState.category)
        assertTrue(errorState.userMessage.contains("Unsupported stream protocol"))
        assertFalse(errorState.canRetry)
    }

    @Test
    fun networkPlayback_missingHost_setsInvalidUrlError() = testScope.runTest {
        val viewModel = createViewModel(SavedStateHandle())
        viewModel.loadMedia("http:///missing_host.mp4")
        advanceUntilIdle()

        assertTrue(fakePlayer.preparedMediaItems.isEmpty())
        val errorState = viewModel.uiState.value as PlayerUiState.Error
        assertEquals(ErrorCategory.InvalidUrl, errorState.category)
        assertTrue(errorState.userMessage.contains("Invalid stream address"))
        assertFalse(errorState.canRetry)
    }

    @Test
    fun networkPlayback_errorTechnicalDetail_stripsSensitiveTokens() = testScope.runTest {
        val viewModel = createViewModel(SavedStateHandle())
        viewModel.loadMedia("rtsp://stream.example.com/live?token=secretKey123&pass=confidential#part2")
        advanceUntilIdle()

        val errorState = viewModel.uiState.value as PlayerUiState.Error
        assertEquals(ErrorCategory.InvalidUrl, errorState.category)
        val detail = errorState.technicalDetail ?: ""
        assertFalse(detail.contains("secretKey123"))
        assertFalse(detail.contains("confidential"))
        assertTrue(detail.contains("rtsp://stream.example.com/live"))
    }

    @Test
    fun networkPlayback_sessionPositionRetention_restoresPositionOnReload() = testScope.runTest {
        val url = "https://example.com/session_test.mp4"
        val viewModel = createViewModel(SavedStateHandle())

        // Initial load
        viewModel.loadMedia(url)
        advanceUntilIdle()
        assertEquals(0L, fakePlayer.initialPositions.last())

        // Simulate session position progress
        viewModel.sessionPositions[url] = 52_000L

        // Reload the same URL in the same session
        viewModel.loadMedia(url)
        advanceUntilIdle()

        // Should restore the session position (52_000L)
        assertEquals(52_000L, fakePlayer.initialPositions.last())
    }

    @Test
    fun networkPlayback_completion_clearsSessionPosition() = testScope.runTest {
        val url = "https://example.com/completed_test.mp4"
        val viewModel = createViewModel(SavedStateHandle())

        viewModel.loadMedia(url)
        advanceUntilIdle()
        viewModel.sessionPositions[url] = 25_000L

        // Simulate stream ended
        fakePlayer.simulateEnded()
        advanceUntilIdle()

        // Session position should be cleared on completion
        assertEquals(null, viewModel.sessionPositions[url])
    }

    @Test
    fun networkPlayback_networkError_allowsRetryAndReloads() = testScope.runTest {
        val url = "https://example.com/timeout_stream.mp4"
        val viewModel = createViewModel(SavedStateHandle())

        viewModel.loadMedia(url)
        advanceUntilIdle()
        assertEquals(1, fakePlayer.preparedMediaItems.size)

        // Simulate network failure
        val networkError = PlaybackError(
            category = ErrorCategory.NetworkFailure,
            userMessage = "Network connection failed",
            technicalDetail = "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT"
        )
        fakePlayer.simulateError(networkError)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlayerUiState.Error
        assertEquals(ErrorCategory.NetworkFailure, state.category)
        assertTrue(state.canRetry)

        // Trigger retry
        viewModel.retry()
        advanceUntilIdle()

        // Verifies that media is prepared again
        assertEquals(2, fakePlayer.preparedMediaItems.size)
        assertEquals(url, fakePlayer.preparedMediaItems.last().uri)
    }

    // =========================================================================
    // Test Fake VideoRepository
    // =========================================================================

    data class ProgressUpdate(
        val id: String,
        val positionMs: Long,
        val percentage: Float,
        val lastPlayedAt: Long,
        val isCompleted: Boolean = false
    )

    private class TestFakeVideoRepository : VideoRepository {
        private val videos = mutableMapOf<String, Video>()
        val progressUpdates = mutableListOf<ProgressUpdate>()

        fun addVideo(video: Video) {
            videos[video.id] = video
        }

        override suspend fun getVideoById(id: String): Video? = videos[id]

        override suspend fun updatePlaybackProgress(
            id: String,
            positionMs: Long,
            percentage: Float,
            lastPlayedAt: Long,
            isCompleted: Boolean
        ) {
            progressUpdates.add(ProgressUpdate(id, positionMs, percentage, lastPlayedAt, isCompleted))
            videos[id]?.let { v ->
                videos[id] = v.copy(
                    playbackPositionMs = positionMs,
                    playbackPercentage = percentage,
                    lastPlayedAt = lastPlayedAt,
                    isCompleted = isCompleted
                )
            }
        }

        override fun getAllHistoryVideos(): Flow<List<Video>> = emptyFlow()
        override suspend fun clearHistoryForVideo(id: String) {
            videos[id]?.let { v ->
                videos[id] = v.copy(
                    playbackPositionMs = 0L,
                    playbackPercentage = 0f,
                    lastPlayedAt = null,
                    isCompleted = false
                )
            }
        }
        override suspend fun clearAllHistory() {
            videos.keys.forEach { clearHistoryForVideo(it) }
        }
        override suspend fun restartPlayback(id: String, startTimeMs: Long) {
            videos[id]?.let { v ->
                videos[id] = v.copy(
                    playbackPositionMs = 0L,
                    playbackPercentage = 0f,
                    isCompleted = false,
                    lastPlayedAt = startTimeMs,
                    watchCount = v.watchCount + 1
                )
            }
        }

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = emptyFlow()
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = emptyFlow()
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = emptyFlow()
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> = emptyFlow()
        override fun getFolders(): Flow<List<VideoFolder>> = emptyFlow()
        override suspend fun getVideoByUri(mediaUri: String): Video? = null
        override suspend fun getVideosCount(): Int = videos.size
        override suspend fun insertVideo(video: Video): Long = 1L
        override suspend fun upsertVideo(video: Video) {}
        override suspend fun upsertVideos(videos: List<Video>) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun deleteVideo(id: String) {}
        override suspend fun deleteVideoByUri(mediaUri: String) {}
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {}
    }

    private class TestFakePlayerPreferencesRepository : PlayerPreferencesRepository {
        val speedFlow = MutableStateFlow(1.0f)
        val resizeFlow = MutableStateFlow(0)
        val decoderFlow = MutableStateFlow(DecoderMode.Hardware)
        val preferredAudioLanguageFlow = MutableStateFlow<String?>(null)
        val preferredSubtitleLanguageFlow = MutableStateFlow<String?>(null)
        val areSubtitlesEnabledFlow = MutableStateFlow(true)
        val defaultSubtitleTrackBehaviorFlow = MutableStateFlow(DefaultSubtitleTrackBehavior.AUTO)
        val audioDelayFlow = MutableStateFlow(0L)
        val subtitleDelayFlow = MutableStateFlow(0L)
        val subtitleAppearanceFlow = MutableStateFlow(SubtitleAppearance())
        val seekDurationFlow = MutableStateFlow(10)
        val isAutoNextFlow = MutableStateFlow(false)
        val isPressAndHoldSpeedFlow = MutableStateFlow(true)
        val audioBoostFlow = MutableStateFlow(100)
        val isEqualizerEnabledFlow = MutableStateFlow(false)
        val equalizerPresetFlow = MutableStateFlow("Flat")
        val repeatModeFlow = MutableStateFlow(RepeatMode.OFF)
        val isShuffleFlow = MutableStateFlow(false)
        val customBandLevelsFlow = MutableStateFlow<Map<Int, Int>>((0 until 5).associateWith { 0 })
        val rememberPerVideoAudioSettingsFlow = MutableStateFlow(false)
        val videoAudioDelayMap = mutableMapOf<String, MutableStateFlow<Long?>>()
        val externalSubtitlesMap = mutableMapOf<String, MutableStateFlow<List<ExternalSubtitle>>>()

        override val playbackSpeed: Flow<Float> = speedFlow
        override val resizeMode: Flow<Int> = resizeFlow
        override val decoderMode: Flow<DecoderMode> = decoderFlow
        override val preferredAudioLanguage: Flow<String?> = preferredAudioLanguageFlow
        override val preferredSubtitleLanguage: Flow<String?> = preferredSubtitleLanguageFlow
        override val areSubtitlesEnabled: Flow<Boolean> = areSubtitlesEnabledFlow
        override val defaultSubtitleTrackBehavior: Flow<DefaultSubtitleTrackBehavior> = defaultSubtitleTrackBehaviorFlow
        override val audioDelayMs: Flow<Long> = audioDelayFlow
        override val subtitleDelayMs: Flow<Long> = subtitleDelayFlow
        override val subtitleAppearance: Flow<SubtitleAppearance> = subtitleAppearanceFlow
        override val seekDurationSeconds: Flow<Int> = seekDurationFlow
        override val isAutoNextEnabled: Flow<Boolean> = isAutoNextFlow
        override val isPressAndHoldSpeedEnabled: Flow<Boolean> = isPressAndHoldSpeedFlow
        override val audioBoostPercent: Flow<Int> = audioBoostFlow
        override val isEqualizerEnabled: Flow<Boolean> = isEqualizerEnabledFlow
        override val equalizerPreset: Flow<String> = equalizerPresetFlow
        override val repeatMode: Flow<RepeatMode> = repeatModeFlow
        override val isShuffleEnabled: Flow<Boolean> = isShuffleFlow
        override val customBandLevels: Flow<Map<Int, Int>> = customBandLevelsFlow
        override val rememberPerVideoAudioSettings: Flow<Boolean> = rememberPerVideoAudioSettingsFlow

        override suspend fun setPlaybackSpeed(speed: Float) {
            speedFlow.value = speed
        }

        override suspend fun setResizeMode(mode: Int) {
            resizeFlow.value = mode
        }

        override suspend fun setDecoderMode(mode: DecoderMode) {
            decoderFlow.value = mode
        }

        override suspend fun setPreferredAudioLanguage(language: String?) {
            preferredAudioLanguageFlow.value = language
        }

        override suspend fun setPreferredSubtitleLanguage(language: String?) {
            preferredSubtitleLanguageFlow.value = language
        }

        override suspend fun setSubtitlesEnabled(enabled: Boolean) {
            areSubtitlesEnabledFlow.value = enabled
        }

        override suspend fun setDefaultSubtitleTrackBehavior(behavior: DefaultSubtitleTrackBehavior) {
            defaultSubtitleTrackBehaviorFlow.value = behavior
        }

        override suspend fun setAudioDelayMs(delayMs: Long) {
            audioDelayFlow.value = delayMs
        }

        override suspend fun setSubtitleDelayMs(delayMs: Long) {
            subtitleDelayFlow.value = delayMs
        }

        override suspend fun setSubtitleAppearance(appearance: SubtitleAppearance) {
            subtitleAppearanceFlow.value = appearance
        }

        override suspend fun setSeekDurationSeconds(duration: Int) {
            seekDurationFlow.value = duration
        }

        override suspend fun setAutoNextEnabled(enabled: Boolean) {
            isAutoNextFlow.value = enabled
        }

        override suspend fun setPressAndHoldSpeedEnabled(enabled: Boolean) {
            isPressAndHoldSpeedFlow.value = enabled
        }

        override suspend fun setAudioBoost(percent: Int) {
            audioBoostFlow.value = percent
        }

        override suspend fun setEqualizerEnabled(enabled: Boolean) {
            isEqualizerEnabledFlow.value = enabled
        }

        override suspend fun setEqualizerPreset(preset: String) {
            equalizerPresetFlow.value = preset
        }

        override suspend fun setCustomBandLevels(levels: Map<Int, Int>) {
            customBandLevelsFlow.value = levels
            equalizerPresetFlow.value = "Custom"
        }

        override suspend fun setCustomBandLevel(bandIndex: Int, levelmB: Int) {
            val current = customBandLevelsFlow.value.toMutableMap()
            current[bandIndex] = levelmB
            customBandLevelsFlow.value = current
            equalizerPresetFlow.value = "Custom"
        }

        override suspend fun setRememberPerVideoAudioSettings(remember: Boolean) {
            rememberPerVideoAudioSettingsFlow.value = remember
        }

        override fun getVideoAudioDelayMs(videoId: String): Flow<Long?> {
            return videoAudioDelayMap.getOrPut(videoId) { MutableStateFlow(null) }
        }

        override suspend fun setVideoAudioDelayMs(videoId: String, delayMs: Long?) {
            val flow = videoAudioDelayMap.getOrPut(videoId) { MutableStateFlow(null) }
            flow.value = delayMs
        }

        override suspend fun setRepeatMode(mode: RepeatMode) {
            repeatModeFlow.value = mode
        }

        override suspend fun setShuffleEnabled(enabled: Boolean) {
            isShuffleFlow.value = enabled
        }

        override fun getExternalSubtitles(videoId: String): Flow<List<ExternalSubtitle>> {
            return externalSubtitlesMap.getOrPut(videoId) { MutableStateFlow(emptyList()) }
        }

        override suspend fun addExternalSubtitle(videoId: String, subtitle: ExternalSubtitle) {
            val flow = externalSubtitlesMap.getOrPut(videoId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + subtitle
        }

        val scaleModeMap = mutableMapOf<String, MutableStateFlow<VideoScaleMode>>()

        override fun getVideoScaleMode(videoId: String): Flow<VideoScaleMode> {
            return scaleModeMap.getOrPut(videoId) { MutableStateFlow(VideoScaleMode.Fit) }
        }

        override suspend fun setVideoScaleMode(videoId: String, mode: VideoScaleMode) {
            val flow = scaleModeMap.getOrPut(videoId) { MutableStateFlow(VideoScaleMode.Fit) }
            flow.value = mode
        }
    }

    private class TestFakeSubtitleRepository : SubtitleRepository {
        var isSupported = true

        override fun isSupportedSubtitle(uri: Uri): Boolean = isSupported

        override fun createExternalSubtitle(uri: Uri): ExternalSubtitle? {
            if (!isSupported) return null
            val path = uri.path ?: uri.toString()
            val fileName = path.substringAfterLast('/')
            return ExternalSubtitle(
                uri = uri.toString(),
                label = fileName,
                language = "eng",
                mimeType = "application/x-subrip"
            )
        }
    }
}
