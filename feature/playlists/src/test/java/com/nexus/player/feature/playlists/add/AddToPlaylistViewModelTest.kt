package com.nexus.player.feature.playlists.add

import com.nexus.player.feature.playlists.FakePlaylistRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddToPlaylistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePlaylistRepository
    private lateinit var viewModel: AddToPlaylistViewModel

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePlaylistRepository()
        fakeRepository.createPlaylist("Favorites")
        fakeRepository.createPlaylist("Watch Later")
        viewModel = AddToPlaylistViewModel(playlistRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPlaylistsAndIdentifiesExistingMemberships() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val playlists = fakeRepository.getPlaylists()
        fakeRepository.addVideoToPlaylist(playlists[0].id, "test_vid")

        viewModel.loadExistingMemberships("test_vid")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.playlists.size)
        assertTrue(state.playlistsContainingVideo.contains(playlists[0].id))
        assertFalse(state.playlistsContainingVideo.contains(playlists[1].id))

        collectJob.cancel()
    }

    @Test
    fun addVideoToPlaylist_successAddsAndUpdatesMembership() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val playlist = fakeRepository.getPlaylists()[1]
        var completed = false
        viewModel.addVideoToPlaylist(playlist, "new_vid") {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        assertTrue(viewModel.uiState.value.playlistsContainingVideo.contains(playlist.id))
        assertNotNull(viewModel.uiState.value.successMessage)

        collectJob.cancel()
    }

    @Test
    fun addVideoToPlaylist_duplicateFails() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val playlist = fakeRepository.getPlaylists()[0]
        fakeRepository.addVideoToPlaylist(playlist.id, "dup_vid")

        viewModel.addVideoToPlaylist(playlist, "dup_vid") {}
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("already in this playlist") == true)

        collectJob.cancel()
    }

    @Test
    fun createAndAddVideo_createsPlaylistAndAddsVideo() = runTest {
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var completed = false
        viewModel.createAndAddVideo("Brand New Playlist", "video_xyz") {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        assertEquals(3, viewModel.uiState.value.playlists.size)
        val created = fakeRepository.getPlaylists().find { it.name == "Brand New Playlist" }
        assertNotNull(created)
        assertTrue(viewModel.uiState.value.playlistsContainingVideo.contains(created!!.id))

        collectJob.cancel()
    }
}
