package com.nexus.player.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
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
        playbackPercentage: Float = 0.0f
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
        playbackPercentage = playbackPercentage
    )

    private fun createViewModel(savedStateHandle: SavedStateHandle): PlayerViewModel {
        val viewModel = PlayerViewModel(
            savedStateHandle = savedStateHandle,
            videoRepository = fakeVideoRepository,
            player = fakePlayer,
            playerPreferencesRepository = fakePreferencesRepository,
            subtitleRepository = fakeSubtitleRepository,
            ioDispatcher = testDispatcher,
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
    // Test Fake VideoRepository
    // =========================================================================

    data class ProgressUpdate(
        val id: String,
        val positionMs: Long,
        val percentage: Float,
        val lastPlayedAt: Long
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
            lastPlayedAt: Long
        ) {
            progressUpdates.add(ProgressUpdate(id, positionMs, percentage, lastPlayedAt))
            videos[id]?.let { v ->
                videos[id] = v.copy(
                    playbackPositionMs = positionMs,
                    playbackPercentage = percentage,
                    lastPlayedAt = lastPlayedAt
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
        val audioDelayFlow = MutableStateFlow(0L)
        val subtitleDelayFlow = MutableStateFlow(0L)
        val subtitleAppearanceFlow = MutableStateFlow(SubtitleAppearance())
        val externalSubtitlesMap = mutableMapOf<String, MutableStateFlow<List<ExternalSubtitle>>>()

        override val playbackSpeed: Flow<Float> = speedFlow
        override val resizeMode: Flow<Int> = resizeFlow
        override val decoderMode: Flow<DecoderMode> = decoderFlow
        override val preferredAudioLanguage: Flow<String?> = preferredAudioLanguageFlow
        override val preferredSubtitleLanguage: Flow<String?> = preferredSubtitleLanguageFlow
        override val areSubtitlesEnabled: Flow<Boolean> = areSubtitlesEnabledFlow
        override val audioDelayMs: Flow<Long> = audioDelayFlow
        override val subtitleDelayMs: Flow<Long> = subtitleDelayFlow
        override val subtitleAppearance: Flow<SubtitleAppearance> = subtitleAppearanceFlow

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

        override suspend fun setAudioDelayMs(delayMs: Long) {
            audioDelayFlow.value = delayMs
        }

        override suspend fun setSubtitleDelayMs(delayMs: Long) {
            subtitleDelayFlow.value = delayMs
        }

        override suspend fun setSubtitleAppearance(appearance: SubtitleAppearance) {
            subtitleAppearanceFlow.value = appearance
        }

        override fun getExternalSubtitles(videoId: String): Flow<List<ExternalSubtitle>> {
            return externalSubtitlesMap.getOrPut(videoId) { MutableStateFlow(emptyList()) }
        }

        override suspend fun addExternalSubtitle(videoId: String, subtitle: ExternalSubtitle) {
            val flow = externalSubtitlesMap.getOrPut(videoId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + subtitle
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
