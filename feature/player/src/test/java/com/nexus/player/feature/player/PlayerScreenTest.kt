package com.nexus.player.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.testing.FakeNexusPlayer
import com.nexus.player.feature.player.panel.PlayerPanelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1920dp-h1080dp")
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakePlayer = FakeNexusPlayer()

    @Test
    fun loadingState_showsLoadingOverlayAndSpinner() {
        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = PlayerUiState.Loading(videoTitle = "Loading Video"),
                    player = fakePlayer,
                    onToggleControls = {},
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeek = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("player_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading Video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_loading_back_button").assertIsDisplayed()
    }

    @Test
    fun readyState_rendersControlsWithTitleAndTimestamps() {
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            isPlaying = true,
            playbackStatus = PlaybackStatus.Ready,
            currentPositionMs = 45_000L,
            durationMs = 120_000L,
            bufferedPositionMs = 60_000L,
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onToggleControls = {},
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeek = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("player_video_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inception").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_current_time").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:45 / 02:00").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_seek_slider").assertIsDisplayed()
    }

    @Test
    fun readyState_playPauseClick_invokesCallback() {
        var playPauseClicked = false
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            isPlaying = true,
            playbackStatus = PlaybackStatus.Ready,
            currentPositionMs = 45_000L,
            durationMs = 120_000L,
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onToggleControls = {},
                    onBackClick = {},
                    onPlayPauseClick = { playPauseClicked = true },
                    onSeek = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
        assertTrue(playPauseClicked)
    }

    @Test
    fun readyState_backClick_invokesCallback() {
        var backClicked = false
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onToggleControls = {},
                    onBackClick = { backClicked = true },
                    onPlayPauseClick = {},
                    onSeek = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("player_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun errorState_showsErrorMessageAndRetryButton() {
        var retryClicked = false
        var backClicked = false
        val errorState = PlayerUiState.Error(
            category = ErrorCategory.NetworkFailure,
            userMessage = "Network connection failed. Check your internet connection.",
            canRetry = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = errorState,
                    player = fakePlayer,
                    onToggleControls = {},
                    onBackClick = { backClicked = true },
                    onPlayPauseClick = {},
                    onSeek = {},
                    onRetry = { retryClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("player_error_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network connection failed. Check your internet connection.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_error_retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_error_back_action").assertIsDisplayed()

        composeTestRule.onNodeWithTag("player_error_retry_button").performClick()
        assertTrue(retryClicked)

        composeTestRule.onNodeWithTag("player_error_back_action").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun surfaceTap_invokesToggleControls() {
        var toggleControlsClicked = false
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = false
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onToggleControls = { toggleControlsClicked = true },
                    onBackClick = {},
                    onPlayPauseClick = {},
                    onSeek = {},
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("player_surface_tap_target").performClick()
        composeTestRule.mainClock.advanceTimeBy(350)
        assertTrue(toggleControlsClicked)
    }

    @Test
    fun readyState_cropButton_invokesCallback() {
        var cropClicked = false
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onCycleCropMode = { cropClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("player_crop_mode_button").performClick()
        assertTrue(cropClicked)
    }

    @Test
    fun readyState_audioSubtitlesButton_invokesCallback() {
        var audioSubClicked = false
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    onOpenAudioSubtitles = { audioSubClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("player_audio_subtitles_button").performClick()
        assertTrue(audioSubClicked)
    }

    @Test
    fun readyState_settingsButton_opensPanelInSideRegion() {
        val panelState = PlayerPanelState()
        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    panelState = panelState
                )
            }
        }

        composeTestRule.onNodeWithTag("player_settings_button").performClick()
        assertTrue(panelState.isOpen)
        composeTestRule.onNodeWithTag("player_settings_side_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("more_settings_view").assertIsDisplayed()
    }

    @Test
    fun settingsPanel_closeButton_closesPanel() {
        val panelState = PlayerPanelState()
        panelState.open()

        val readyState = PlayerUiState.Ready(
            videoId = "v1",
            videoTitle = "Inception",
            controlsVisible = true
        )

        composeTestRule.setContent {
            NexusTheme {
                PlayerScreen(
                    uiState = readyState,
                    player = fakePlayer,
                    panelState = panelState
                )
            }
        }

        composeTestRule.onNodeWithTag("panel_close_button").performClick()
        assertFalse(panelState.isOpen)
    }
}
