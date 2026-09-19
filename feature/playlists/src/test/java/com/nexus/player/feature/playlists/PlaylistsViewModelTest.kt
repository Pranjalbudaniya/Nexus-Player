package com.nexus.player.feature.playlists

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePlaylistRepository
    private lateinit var viewModel: PlaylistsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePlaylistRepository()
        viewModel = PlaylistsViewModel(playlistRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_emptyPlaylists() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.playlists.size)

        collectJob.cancel()
    }

    @Test
    fun createPlaylist_successUpdatesUiState() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.openCreateDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCreateDialogOpen)

        var createdId: String? = null
        viewModel.createPlaylist("Action Movies", "Cool action movies") { id ->
            createdId = id
        }
        advanceUntilIdle()

        assertNotNull(createdId)
        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
        assertEquals(1, viewModel.uiState.value.playlists.size)
        assertEquals("Action Movies", viewModel.uiState.value.playlists[0].name)

        collectJob.cancel()
    }

    @Test
    fun createPlaylist_duplicateOrBlankFails() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.createPlaylist("Movies", null)
        advanceUntilIdle()

        // Attempt blank
        viewModel.createPlaylist("   ", null)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Attempt duplicate
        viewModel.createPlaylist("movies", null)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage?.contains("already exists") == true)

        collectJob.cancel()
    }

    @Test
    fun searchFilter_filtersPlaylistsByNameAndDescription() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.createPlaylist("Sci-Fi Thrillers", "Alien movies")
        viewModel.createPlaylist("Comedy Night", "Funny sketches")
        viewModel.createPlaylist("Documentaries", "Space exploration")
        advanceUntilIdle()

        viewModel.onSearchQueryChange("space")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredPlaylists.size)
        assertEquals("Documentaries", viewModel.uiState.value.filteredPlaylists[0].name)

        viewModel.onSearchQueryChange("comedy")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredPlaylists.size)
        assertEquals("Comedy Night", viewModel.uiState.value.filteredPlaylists[0].name)

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.filteredPlaylists.size)

        collectJob.cancel()
    }

    @Test
    fun renamePlaylist_updatesName() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.createPlaylist("Old Name", null)
        advanceUntilIdle()

        val playlist = viewModel.uiState.value.playlists.first()
        viewModel.openRenameDialog(playlist)
        advanceUntilIdle()
        assertEquals(playlist, viewModel.uiState.value.playlistToRename)

        viewModel.renamePlaylist(playlist.id, "New Name")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.playlistToRename)
        assertEquals("New Name", viewModel.uiState.value.playlists.first().name)

        collectJob.cancel()
    }

    @Test
    fun deletePlaylist_removesFromState() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.createPlaylist("To Delete", null)
        advanceUntilIdle()

        val playlist = viewModel.uiState.value.playlists.first()
        viewModel.openDeleteConfirmation(playlist)
        advanceUntilIdle()
        assertEquals(playlist, viewModel.uiState.value.playlistToDelete)

        viewModel.confirmDeletePlaylist()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.playlistToDelete)
        assertTrue(viewModel.uiState.value.isEmpty)

        collectJob.cancel()
    }
}
