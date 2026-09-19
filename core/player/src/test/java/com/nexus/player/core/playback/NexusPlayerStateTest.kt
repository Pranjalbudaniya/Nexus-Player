package com.nexus.player.core.playback

import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.core.playback.model.NexusMediaItem
import com.nexus.player.core.playback.model.PlaybackError
import com.nexus.player.core.playback.model.PlaybackStatus
import com.nexus.player.core.playback.model.PlayerState
import com.nexus.player.core.playback.testing.FakeNexusPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests player state transitions, playback operations, and lifecycle
 * using the [FakeNexusPlayer] test double.
 */
class NexusPlayerStateTest {

    private lateinit var player: FakeNexusPlayer

    private val testMediaItem = NexusMediaItem(
        mediaId = "video_123",
        uri = "content://media/external/video/123",
        title = "Test Video"
    )

    private val httpMediaItem = NexusMediaItem(
        mediaId = "http_video",
        uri = "https://example.com/video.mp4",
        title = "Network Video"
    )

    @Before
    fun setUp() {
        player = FakeNexusPlayer()
    }

    // =========================================================================
    // Initial state
    // =========================================================================

    @Test
    fun initialState_isIdleWithDefaults() {
        val state = player.state.value
        assertEquals(PlaybackStatus.Idle, state.playbackState)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(0L, state.bufferedPosition)
        assertEquals(1.0f, state.playbackSpeed)
        assertFalse(state.isSeekable)
        assertNull(state.currentMediaId)
        assertNull(state.error)
    }

    // =========================================================================
    // State transitions: Prepare
    // =========================================================================

    @Test
    fun prepare_transitionsToLoadingWithMediaId() {
        player.prepare(testMediaItem)

        val state = player.state.value
        assertEquals(PlaybackStatus.Loading, state.playbackState)
        assertEquals("video_123", state.currentMediaId)
        assertFalse(state.isPlaying)
    }

    @Test
    fun prepare_recordsMediaItem() {
        player.prepare(testMediaItem)
        player.prepare(httpMediaItem)

        assertEquals(2, player.preparedMediaItems.size)
        assertEquals("video_123", player.preparedMediaItems[0].mediaId)
        assertEquals("http_video", player.preparedMediaItems[1].mediaId)
    }

    @Test
    fun prepare_withHttpUrl_transitionsToLoading() {
        player.prepare(httpMediaItem)

        val state = player.state.value
        assertEquals(PlaybackStatus.Loading, state.playbackState)
        assertEquals("http_video", state.currentMediaId)
    }

    @Test
    fun prepare_calledMultipleTimes_resetsState() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 60_000L)
        player.play()
        player.simulatePositionUpdate(30_000L)

        // Second prepare resets everything
        player.prepare(httpMediaItem)

        val state = player.state.value
        assertEquals(PlaybackStatus.Loading, state.playbackState)
        assertEquals("http_video", state.currentMediaId)
        assertFalse(state.isPlaying)
    }

    // =========================================================================
    // State transitions: Ready
    // =========================================================================

    @Test
    fun simulateReady_transitionsToReadyWithDuration() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 120_000L)

        val state = player.state.value
        assertEquals(PlaybackStatus.Ready, state.playbackState)
        assertEquals(120_000L, state.duration)
        assertTrue(state.isSeekable)
    }

    // =========================================================================
    // Play / Pause
    // =========================================================================

    @Test
    fun play_setsIsPlayingTrue() {
        player.prepare(testMediaItem)
        player.simulateReady()
        player.play()

        assertTrue(player.state.value.isPlaying)
        assertEquals(1, player.playCount)
    }

    @Test
    fun pause_setsIsPlayingFalse() {
        player.prepare(testMediaItem)
        player.simulateReady()
        player.play()
        player.pause()

        assertFalse(player.state.value.isPlaying)
        assertEquals(1, player.pauseCount)
    }

    @Test
    fun playPauseToggle_cyclesCorrectly() {
        player.prepare(testMediaItem)
        player.simulateReady()

        player.play()
        assertTrue(player.state.value.isPlaying)

        player.pause()
        assertFalse(player.state.value.isPlaying)

        player.play()
        assertTrue(player.state.value.isPlaying)

        assertEquals(2, player.playCount)
        assertEquals(1, player.pauseCount)
    }

    // =========================================================================
    // Seeking
    // =========================================================================

    @Test
    fun seekTo_updatesCurrentPosition() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 60_000L)

        player.seekTo(30_000L)

        assertEquals(30_000L, player.state.value.currentPosition)
        assertEquals(listOf(30_000L), player.seekPositions)
    }

    @Test
    fun seekTo_clampsToDuration() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 60_000L)

        player.seekTo(100_000L)

        assertEquals(60_000L, player.state.value.currentPosition)
    }

    @Test
    fun seekTo_clampsToZero() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 60_000L)

        player.seekTo(-5_000L)

        assertEquals(0L, player.state.value.currentPosition)
    }

    // =========================================================================
    // Playback speed
    // =========================================================================

    @Test
    fun setPlaybackSpeed_updatesState() {
        player.prepare(testMediaItem)
        player.simulateReady()

        player.setPlaybackSpeed(2.0f)

        assertEquals(2.0f, player.state.value.playbackSpeed)
        assertEquals(listOf(2.0f), player.playbackSpeeds)
    }

    @Test
    fun setPlaybackSpeed_invalidSpeed_isIgnored() {
        player.prepare(testMediaItem)
        player.simulateReady()

        player.setPlaybackSpeed(0f)
        player.setPlaybackSpeed(-1f)

        assertEquals(1.0f, player.state.value.playbackSpeed)
        assertTrue(player.playbackSpeeds.isEmpty())
    }

    @Test
    fun setPlaybackSpeed_multipleChanges_tracksAll() {
        player.setPlaybackSpeed(0.5f)
        player.setPlaybackSpeed(1.5f)
        player.setPlaybackSpeed(2.0f)

        assertEquals(listOf(0.5f, 1.5f, 2.0f), player.playbackSpeeds)
        assertEquals(2.0f, player.state.value.playbackSpeed)
    }

    // =========================================================================
    // Position updates
    // =========================================================================

    @Test
    fun simulatePositionUpdate_updatesPositionAndBuffer() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 120_000L)
        player.play()

        player.simulatePositionUpdate(positionMs = 45_000L, bufferedPositionMs = 60_000L)

        val state = player.state.value
        assertEquals(45_000L, state.currentPosition)
        assertEquals(60_000L, state.bufferedPosition)
    }

    // =========================================================================
    // Playback position snapshot
    // =========================================================================

    @Test
    fun getCurrentPlaybackPosition_returnsCorrectSnapshot() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 100_000L)
        player.play()
        player.simulatePositionUpdate(65_000L)

        val position = player.getCurrentPlaybackPosition()

        assertNotNull(position!!)
        assertEquals("video_123", position.mediaId)
        assertEquals(65_000L, position.positionMs)
        assertEquals(100_000L, position.durationMs)
        assertEquals(0.65f, position.completionPercentage, 0.01f)
        assertTrue(position.timestamp > 0)
    }

    @Test
    fun getCurrentPlaybackPosition_noMedia_returnsNull() {
        assertNull(player.getCurrentPlaybackPosition())
    }

    @Test
    fun getCurrentPlaybackPosition_afterRelease_returnsNull() {
        player.prepare(testMediaItem)
        player.simulateReady()
        player.release()

        assertNull(player.getCurrentPlaybackPosition())
    }

    // =========================================================================
    // Ended state
    // =========================================================================

    @Test
    fun simulateEnded_transitionsToEndedAtFullDuration() {
        player.prepare(testMediaItem)
        player.simulateReady(durationMs = 90_000L)
        player.play()
        player.simulateEnded()

        val state = player.state.value
        assertEquals(PlaybackStatus.Ended, state.playbackState)
        assertFalse(state.isPlaying)
        assertEquals(90_000L, state.currentPosition)
    }

    // =========================================================================
    // Buffering
    // =========================================================================

    @Test
    fun simulateBuffering_transitionsToBufferingState() {
        player.prepare(testMediaItem)
        player.simulateReady()
        player.play()
        player.simulateBuffering(bufferedPosition = 25_000L)

        val state = player.state.value
        assertEquals(PlaybackStatus.Buffering, state.playbackState)
        assertFalse(state.isPlaying)
        assertEquals(25_000L, state.bufferedPosition)
    }

    // =========================================================================
    // Error handling
    // =========================================================================

    @Test
    fun simulateError_transitionsToErrorState() {
        val error = PlaybackError(
            category = ErrorCategory.UnsupportedCodec,
            userMessage = "This video format is not supported on your device",
            technicalDetail = "HEVC not supported"
        )
        player.prepare(testMediaItem)
        player.simulateReady()
        player.play()
        player.simulateError(error)

        val state = player.state.value
        assertEquals(PlaybackStatus.Error, state.playbackState)
        assertFalse(state.isPlaying)
        assertNotNull(state.error)
        assertEquals(ErrorCategory.UnsupportedCodec, state.error!!.category)
        assertEquals("This video format is not supported on your device", state.error!!.userMessage)
    }

    @Test
    fun simulateError_missingFile_hasCorrectCategory() {
        val error = PlaybackError(
            category = ErrorCategory.MissingFile,
            userMessage = "Video file not found"
        )
        player.prepare(testMediaItem)
        player.simulateError(error)

        assertEquals(ErrorCategory.MissingFile, player.state.value.error?.category)
    }

    @Test
    fun simulateError_networkFailure_hasCorrectCategory() {
        val error = PlaybackError(
            category = ErrorCategory.NetworkFailure,
            userMessage = "Network connection failed"
        )
        player.prepare(httpMediaItem)
        player.simulateError(error)

        assertEquals(ErrorCategory.NetworkFailure, player.state.value.error?.category)
    }

    // =========================================================================
    // Decoder mode
    // =========================================================================

    @Test
    fun setDecoderMode_recordsMode() {
        player.setDecoderMode(DecoderMode.Hardware)
        player.setDecoderMode(DecoderMode.Software)
        player.setDecoderMode(DecoderMode.Auto)

        assertEquals(
            listOf(DecoderMode.Hardware, DecoderMode.Software, DecoderMode.Auto),
            player.decoderModes
        )
    }

    // =========================================================================
    // Release behavior
    // =========================================================================

    @Test
    fun release_resetsToInitialState() {
        player.prepare(testMediaItem)
        player.simulateReady()
        player.play()

        player.release()

        val state = player.state.value
        assertEquals(PlayerState.INITIAL, state)
        assertTrue(player.isReleased)
    }

    @Test
    fun release_subsequentCallsAreNoOps() {
        player.prepare(testMediaItem)
        player.release()

        // These should all be silently ignored
        player.prepare(httpMediaItem)
        player.play()
        player.pause()
        player.seekTo(10_000L)
        player.setPlaybackSpeed(2.0f)

        // Only the first prepare was recorded (before release)
        assertEquals(1, player.preparedMediaItems.size)
        assertEquals(0, player.playCount)
        assertEquals(0, player.pauseCount)
        assertTrue(player.seekPositions.isEmpty())
        assertTrue(player.playbackSpeeds.isEmpty())
    }

    // =========================================================================
    // Full lifecycle
    // =========================================================================

    @Test
    fun fullPlaybackLifecycle_transitionsCorrectly() {
        // 1. Initial
        assertEquals(PlaybackStatus.Idle, player.state.value.playbackState)

        // 2. Prepare → Loading
        player.prepare(testMediaItem)
        assertEquals(PlaybackStatus.Loading, player.state.value.playbackState)

        // 3. Ready
        player.simulateReady(durationMs = 180_000L)
        assertEquals(PlaybackStatus.Ready, player.state.value.playbackState)
        assertEquals(180_000L, player.state.value.duration)

        // 4. Play
        player.play()
        assertTrue(player.state.value.isPlaying)

        // 5. Position advancing
        player.simulatePositionUpdate(30_000L, 60_000L)
        assertEquals(30_000L, player.state.value.currentPosition)

        // 6. Seek
        player.seekTo(90_000L)
        assertEquals(90_000L, player.state.value.currentPosition)

        // 7. Speed change
        player.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, player.state.value.playbackSpeed)

        // 8. Buffering mid-playback
        player.simulateBuffering(100_000L)
        assertEquals(PlaybackStatus.Buffering, player.state.value.playbackState)

        // 9. Resume after buffer
        player.play()
        assertTrue(player.state.value.isPlaying)

        // 10. Pause
        player.pause()
        assertFalse(player.state.value.isPlaying)

        // 11. Get position snapshot
        val snapshot = player.getCurrentPlaybackPosition()
        assertNotNull(snapshot)

        // 12. Release
        player.release()
        assertEquals(PlaybackStatus.Idle, player.state.value.playbackState)
        assertTrue(player.isReleased)
    }
}
