package com.nexus.player.feature.more

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.feature.more.component.UrlValidationResult
import com.nexus.player.feature.more.component.validateNetworkUrl
import com.nexus.player.feature.more.favorites.FavoritesScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h2400dp")
class MoreScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMetadata = MediaMetadata(
        id = "test_vid_1",
        mediaUri = "content://media/test_vid_1",
        filePath = "/storage/Movies/sample.mp4",
        fileName = "sample.mp4",
        title = "Sample Video Title",
        folderName = "Movies",
        folderPath = "/storage/Movies",
        formattedDuration = "02:15",
        durationMs = 135000L,
        resolutionLabel = "1080p",
        dimensionsLabel = "1920x1080",
        width = 1920,
        height = 1080,
        videoCodec = "H.264",
        audioCodec = "AAC",
        formattedFps = "30 fps",
        frameRate = 30f,
        formattedBitrate = "4.5 Mbps",
        videoBitrate = 4500000L,
        formattedSize = "15 MB",
        sizeBytes = 15000000L,
        formattedModifiedDate = "Jan 1, 2026",
        lastModified = 1000L,
        audioTrackCount = 1,
        subtitleTrackCount = 0,
        isFavorite = true,
        playbackPositionMs = 0L,
        playbackPercentage = 0f,
        watchCount = 0,
        lastPlayedAt = null
    )

    private fun sampleVideo(id: String, title: String) = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "/storage/Movies/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = "Movies",
        folderPath = "/storage/Movies",
        sizeBytes = 1000000L,
        durationMs = 120000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        dateAdded = 1000L,
        lastModified = 1000L,
        lastPlayedAt = null,
        playbackPositionMs = 0L,
        playbackPercentage = 0f,
        isFavorite = true,
        isCompleted = false,
        watchCount = 0
    )

    @Test
    fun moreScreen_displaysAllEightItemsAndSettings() {
        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = sampleMetadata,
                    onNavigateToHistory = {},
                    onNavigateToAnalytics = {},
                    onNavigateToFavorites = {},
                    onNavigateToPlaylists = {},
                    onNavigateToStorageLocations = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Top bar title and separate Settings button
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Flat List Items
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Playback Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Playlists").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network Stream").assertIsDisplayed()
        composeTestRule.onNodeWithText("File & Codec Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Storage & Scanning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Source Licenses").assertIsDisplayed()
        composeTestRule.onNodeWithText("About Nexus Player").assertIsDisplayed()
    }

    @Test
    fun moreScreen_navigationCallbacksTriggerCorrectly() {
        var historyClicked = false
        var analyticsClicked = false
        var favoritesClicked = false
        var playlistsClicked = false
        var storageClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = sampleMetadata,
                    onNavigateToHistory = { historyClicked = true },
                    onNavigateToAnalytics = { analyticsClicked = true },
                    onNavigateToFavorites = { favoritesClicked = true },
                    onNavigateToPlaylists = { playlistsClicked = true },
                    onNavigateToStorageLocations = { storageClicked = true },
                    onNavigateToSettings = { settingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("History").performClick()
        assertTrue(historyClicked)

        composeTestRule.onNodeWithText("Playback Analytics").performClick()
        assertTrue(analyticsClicked)

        composeTestRule.onNodeWithText("Favorites").performClick()
        assertTrue(favoritesClicked)

        composeTestRule.onNodeWithText("Playlists").performClick()
        assertTrue(playlistsClicked)

        composeTestRule.onNodeWithText("Storage & Scanning").performClick()
        assertTrue(storageClicked)

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(settingsClicked)
    }

    @Test
    fun moreScreen_codecInfoItemOpensDialog() {
        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = sampleMetadata,
                    onNavigateToHistory = {},
                    onNavigateToAnalytics = {},
                    onNavigateToFavorites = {},
                    onNavigateToPlaylists = {},
                    onNavigateToStorageLocations = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("File & Codec Information").performClick()
        composeTestRule.onNodeWithText("File Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video Codec").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio Codec").assertIsDisplayed()
    }

    @Test
    fun moreScreen_licensesItemOpensDialog() {
        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = null,
                    onNavigateToHistory = {},
                    onNavigateToAnalytics = {},
                    onNavigateToFavorites = {},
                    onNavigateToPlaylists = {},
                    onNavigateToStorageLocations = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Open Source Licenses").performClick()
        composeTestRule.onNodeWithText("AndroidX Media3 / ExoPlayer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun moreScreen_aboutItemOpensDialog() {
        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = null,
                    onNavigateToHistory = {},
                    onNavigateToAnalytics = {},
                    onNavigateToFavorites = {},
                    onNavigateToPlaylists = {},
                    onNavigateToStorageLocations = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithText("About Nexus Player").performClick()
        composeTestRule.onNodeWithText("Key Capabilities:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_emptyStateDisplaysCorrectly() {
        composeTestRule.setContent {
            NexusTheme {
                FavoritesScreen(
                    favoriteVideos = emptyList(),
                    selectedVideoForMenu = null,
                    folders = emptyList(),
                    thumbnailLoader = null,
                    onNavigateBack = {},
                    onNavigateToPlayer = {},
                    onVideoLongClick = {},
                    onDismissContextMenu = {},
                    onAddToPlaylist = {},
                    onToggleFavorite = {},
                    onShare = {},
                    onOpenContainingFolder = { _, _ -> },
                    onRenameConfirm = { _, _ -> },
                    onMoveConfirm = { _, _ -> },
                    onCopyConfirm = { _, _ -> },
                    onDeleteConfirm = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Favorites Yet").assertIsDisplayed()
    }

    @Test
    fun favoritesScreen_displaysVideosAndTriggersPlayback() {
        var playedVideoId: String? = null
        val videos = listOf(sampleVideo("v1", "First Favorite"), sampleVideo("v2", "Second Favorite"))

        composeTestRule.setContent {
            NexusTheme {
                FavoritesScreen(
                    favoriteVideos = videos,
                    selectedVideoForMenu = null,
                    folders = emptyList(),
                    thumbnailLoader = null,
                    onNavigateBack = {},
                    onNavigateToPlayer = { playedVideoId = it },
                    onVideoLongClick = {},
                    onDismissContextMenu = {},
                    onAddToPlaylist = {},
                    onToggleFavorite = {},
                    onShare = {},
                    onOpenContainingFolder = { _, _ -> },
                    onRenameConfirm = { _, _ -> },
                    onMoveConfirm = { _, _ -> },
                    onCopyConfirm = { _, _ -> },
                    onDeleteConfirm = {}
                )
            }
        }

        composeTestRule.onNodeWithText("First Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Favorite").assertIsDisplayed()

        composeTestRule.onNodeWithText("First Favorite").performClick()
        assertTrue(playedVideoId == "v1")
    }

    @Test
    fun validateNetworkUrl_validationRules() {
        assertEquals(UrlValidationResult.Empty, validateNetworkUrl(""))
        assertEquals(UrlValidationResult.Empty, validateNetworkUrl("   "))

        assertTrue(validateNetworkUrl("http://example.com/video.mp4") is UrlValidationResult.Valid)
        assertTrue(validateNetworkUrl("https://commondatastorage.googleapis.com/test.mkv?token=123") is UrlValidationResult.Valid)
        assertTrue(validateNetworkUrl("HTTP://UPPERCASE.COM/STREAM.MP4") is UrlValidationResult.Valid)

        val ftpResult = validateNetworkUrl("ftp://files.example.com/video.mp4")
        assertTrue(ftpResult is UrlValidationResult.Invalid)
        assertTrue((ftpResult as UrlValidationResult.Invalid).reason.contains("Unsupported scheme"))

        val fileResult = validateNetworkUrl("file:///storage/emulated/0/video.mp4")
        assertTrue(fileResult is UrlValidationResult.Invalid)

        val invalidScheme = validateNetworkUrl("javascript:alert(1)")
        assertTrue(invalidScheme is UrlValidationResult.Invalid)

        val missingHost = validateNetworkUrl("http://")
        assertTrue(missingHost is UrlValidationResult.Invalid)
    }

    @Test
    fun moreScreen_clickNetworkStream_opensDialogAndPlaysValidUrl() {
        var playedUrl: String? = null

        composeTestRule.setContent {
            NexusTheme {
                MoreScreen(
                    recentVideo = sampleMetadata,
                    onNavigateToHistory = {},
                    onNavigateToAnalytics = {},
                    onNavigateToFavorites = {},
                    onNavigateToPlaylists = {},
                    onNavigateToStorageLocations = {},
                    onNavigateToSettings = {},
                    onNavigateToPlayer = { playedUrl = it }
                )
            }
        }

        // Open Dialog
        composeTestRule.onNodeWithText("Network Stream").performClick()
        composeTestRule.onNodeWithTag("network_url_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Network Stream").assertIsDisplayed()

        // Play button disabled initially (empty URL)
        composeTestRule.onNodeWithTag("network_url_play_button").assertIsNotEnabled()

        // Enter invalid protocol
        composeTestRule.onNodeWithTag("network_url_input").performTextInput("ftp://myfiles.com/stream.mp4")
        composeTestRule.onNodeWithTag("network_url_error_text", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("network_url_play_button").assertIsNotEnabled()

        // Clear input
        composeTestRule.onNodeWithTag("network_url_clear_button").performClick()

        // Enter valid HTTPS url
        val targetUrl = "https://example.com/videos/bunny.mp4"
        composeTestRule.onNodeWithTag("network_url_input").performTextInput(targetUrl)

        // Play button now enabled
        composeTestRule.onNodeWithTag("network_url_play_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(targetUrl, playedUrl)
    }
}
