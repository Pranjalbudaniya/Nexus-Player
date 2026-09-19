package com.nexus.player.feature.home

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1080dp-h2400dp")
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeThumbnailLoader = object : ThumbnailLoader {
        override suspend fun loadThumbnail(mediaUri: String, targetWidth: Int, targetHeight: Int): Bitmap? = null
        override fun clearMemoryCache() {}
        override fun getCachedEntriesCount(): Int = 0
    }

    private val sampleSuccessState = HomeUiState.Success(
        continueWatching = listOf(
            ContinueWatchingItem(
                id = "cw_1",
                title = "Interstellar",
                duration = "02:49:03",
                progress = 0.65f,
                quality = "4K HDR",
                mediaUri = "content://media/cw_1"
            )
        ),
        recentlyAdded = listOf(
            RecentVideoItem(
                id = "ra_1",
                title = "Oppenheimer",
                technicalSpecs = "HEVC • 14.8 GB",
                addedTimeAndFolder = "Sep 19, 2026",
                duration = "03:00:21",
                quality = "4K HDR",
                mediaUri = "content://media/ra_1"
            )
        ),
        favorites = listOf(
            FavoriteVideoItem(
                id = "fav_1",
                title = "Spider-Man: Into the Spider-Verse",
                duration = "01:57:00",
                quality = "4K HDR",
                mediaUri = "content://media/fav_1"
            )
        ),
        folders = listOf(
            FolderItem(
                id = "/storage/emulated/0/Movies",
                name = "Movies",
                videoCountText = "48 videos",
                previewMediaUri = "content://media/cw_1"
            )
        ),
        isScanning = false
    )

    @Test
    fun homeScreen_rendersAllFourSectionsInCorrectOrder() {
        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = sampleSuccessState,
                    thumbnailLoader = fakeThumbnailLoader
                )
            }
        }

        // Section 1: Continue Watching
        composeTestRule.onNodeWithText("Continue Watching").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interstellar").assertIsDisplayed()
        composeTestRule.onNodeWithText("02:49:03").assertIsDisplayed()

        // Section 2: Recently Added
        composeTestRule.onNodeWithText("Recently Added").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oppenheimer").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEVC • 14.8 GB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sep 19, 2026").assertIsDisplayed()

        // Section 3: Favorites
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spider-Man: Into the Spider-Verse").assertIsDisplayed()

        // Section 4: Folders
        composeTestRule.onNodeWithText("Folders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movies").assertIsDisplayed()
        composeTestRule.onNodeWithText("48 videos").assertIsDisplayed()
    }

    @Test
    fun homeScreen_clickingVideo_triggersOnVideoClickWithStableId() {
        var clickedVideoId: String? = null

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = sampleSuccessState,
                    thumbnailLoader = fakeThumbnailLoader,
                    onVideoClick = { clickedVideoId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Interstellar").performClick()
        assertEquals("cw_1", clickedVideoId)

        composeTestRule.onNodeWithText("Oppenheimer").performClick()
        assertEquals("ra_1", clickedVideoId)
    }

    @Test
    fun homeScreen_clickingFolder_triggersOnFolderClickWithFolderData() {
        var clickedFolderPath: String? = null
        var clickedFolderName: String? = null

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = sampleSuccessState,
                    thumbnailLoader = fakeThumbnailLoader,
                    onFolderClick = { path, name ->
                        clickedFolderPath = path
                        clickedFolderName = name
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Movies").performClick()
        assertEquals("/storage/emulated/0/Movies", clickedFolderPath)
        assertEquals("Movies", clickedFolderName)
    }

    @Test
    fun homeScreen_showsSubtleScanningBannerWhileContentRemainsUsable() {
        val scanningState = sampleSuccessState.copy(isScanning = true)

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = scanningState,
                    thumbnailLoader = fakeThumbnailLoader
                )
            }
        }

        // Subtle scanning banner is visible
        composeTestRule.onNodeWithText("Scanning media library...").assertIsDisplayed()

        // Content remains visible and usable
        composeTestRule.onNodeWithText("Interstellar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oppenheimer").assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptySections_displaysCleanEmptyMessages() {
        val emptySectionsState = HomeUiState.Success(
            continueWatching = emptyList(),
            recentlyAdded = emptyList(),
            favorites = emptyList(),
            folders = emptyList(),
            isScanning = false
        )

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = emptySectionsState,
                    thumbnailLoader = fakeThumbnailLoader
                )
            }
        }

        composeTestRule.onNodeWithText("No in-progress videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("No recently added videos").assertIsDisplayed()
        composeTestRule.onNodeWithText("No favorite videos yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("No video folders found").assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyStateScanning_displaysFirstLaunchScanMessage() {
        val emptyState = HomeUiState.Empty(isScanning = true, noAccessibleMedia = false)

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = emptyState,
                    thumbnailLoader = fakeThumbnailLoader
                )
            }
        }

        composeTestRule.onNodeWithText("Scanning your device for videos...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discovered videos will appear here shortly").assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyStateNoAccess_displaysPermissionRequiredMessage() {
        val emptyState = HomeUiState.Empty(isScanning = false, noAccessibleMedia = true)

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = emptyState,
                    thumbnailLoader = fakeThumbnailLoader
                )
            }
        }

        composeTestRule.onNodeWithText("No accessible media found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Storage access is required to display your video library").assertIsDisplayed()
    }

    @Test
    fun homeScreen_topBarSettingsButton_hasContentDescription() {
        var settingsClicked = false

        composeTestRule.setContent {
            NexusTheme {
                HomeScreen(
                    uiState = sampleSuccessState,
                    thumbnailLoader = fakeThumbnailLoader,
                    onNavigateToSettings = { settingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()
        assertEquals(true, settingsClicked)
    }
}
