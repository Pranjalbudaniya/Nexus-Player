package com.nexus.player.core.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun topLevelRoutes_serializeCorrectly() {
        assertEquals(HomeRoute, Json.decodeFromString<HomeRoute>(Json.encodeToString(HomeRoute)))
        assertEquals(LibraryRoute, Json.decodeFromString<LibraryRoute>(Json.encodeToString(LibraryRoute)))
        assertEquals(PlaylistsRoute, Json.decodeFromString<PlaylistsRoute>(Json.encodeToString(PlaylistsRoute)))
        assertEquals(MoreRoute, Json.decodeFromString<MoreRoute>(Json.encodeToString(MoreRoute)))
    }

    @Test
    fun deepFeatureRoutes_serializeCorrectly() {
        val playerRoute = PlayerRoute(videoId = "vid_4k_hdr")
        assertEquals(playerRoute, Json.decodeFromString<PlayerRoute>(Json.encodeToString(playerRoute)))

        val folderRoute = FolderRoute(folderPath = "/storage/emulated/0/Movies", folderName = "Movies")
        assertEquals(folderRoute, Json.decodeFromString<FolderRoute>(Json.encodeToString(folderRoute)))

        val seriesRoute = SeriesRoute(seriesId = "series_101")
        assertEquals(seriesRoute, Json.decodeFromString<SeriesRoute>(Json.encodeToString(seriesRoute)))

        val videoDetailsRoute = VideoDetailsRoute(videoId = "vid_4k_hdr")
        assertEquals(videoDetailsRoute, Json.decodeFromString<VideoDetailsRoute>(Json.encodeToString(videoDetailsRoute)))

        val playlistDetailsRoute = PlaylistDetailsRoute(playlistId = "pl_favorites")
        assertEquals(playlistDetailsRoute, Json.decodeFromString<PlaylistDetailsRoute>(Json.encodeToString(playlistDetailsRoute)))

        assertEquals(SettingsRoute, Json.decodeFromString<SettingsRoute>(Json.encodeToString(SettingsRoute)))
        assertEquals(SearchRoute, Json.decodeFromString<SearchRoute>(Json.encodeToString(SearchRoute)))
    }

    @Test
    fun topLevelDestinations_containsExactlyFourBottomBarTabs() {
        val destinations = TopLevelDestination.entries
        assertEquals(4, destinations.size)
        assertEquals(listOf(TopLevelDestination.HOME, TopLevelDestination.LIBRARY, TopLevelDestination.PLAYLISTS, TopLevelDestination.MORE), destinations)
    }

    @Test
    fun topLevelDestinations_routesMatchExpectedContracts() {
        assertEquals(HomeRoute, TopLevelDestination.HOME.route)
        assertEquals(LibraryRoute, TopLevelDestination.LIBRARY.route)
        assertEquals(PlaylistsRoute, TopLevelDestination.PLAYLISTS.route)
        assertEquals(MoreRoute, TopLevelDestination.MORE.route)
    }

    @Test
    fun settingsRoute_isNotPartOfBottomNavigation() {
        val bottomNavRoutes = TopLevelDestination.entries.map { it.route }
        assertFalse("SettingsRoute must NOT be present in bottom navigation", bottomNavRoutes.contains(SettingsRoute))
    }
}
