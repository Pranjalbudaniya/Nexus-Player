package com.nexus.player.feature.onboarding

import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

private class FakeStorageAccessRepository : StorageAccessRepository {
    private val _state = MutableStateFlow(StorageAccessState())
    override val storageAccessState: Flow<StorageAccessState> = _state.asStateFlow()

    var permissionGranted: Boolean = false

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _state.value = _state.value.copy(isOnboardingCompleted = completed)
    }

    override suspend fun setStorageAccessMode(mode: StorageAccessMode) {
        _state.value = _state.value.copy(accessMode = mode)
    }

    override suspend fun addSelectedFolderUri(uriString: String) {
        val current = _state.value.selectedFolderUris
        _state.value = _state.value.copy(
            selectedFolderUris = current + uriString,
            accessMode = StorageAccessMode.SELECTED_FOLDERS
        )
    }

    override suspend fun removeSelectedFolderUri(uriString: String) {
        val current = _state.value.selectedFolderUris
        _state.value = _state.value.copy(selectedFolderUris = current - uriString)
    }

    override suspend fun clearSelectedFolders() {
        _state.value = _state.value.copy(
            selectedFolderUris = emptySet(),
            accessMode = StorageAccessMode.ALL_MEDIA
        )
    }

    override fun isPermissionGranted(): Boolean = permissionGranted

    override fun getRequiredPermissions(): List<String> = listOf("android.permission.READ_MEDIA_VIDEO")
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPermissionResult_granted_enablesCanProceed() = runTest {
        val repository = FakeStorageAccessRepository()
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()

        viewModel.onPermissionResult(isGranted = true, shouldShowRationale = false)

        val state = viewModel.uiState.value as? OnboardingUiState.Ready
        assertTrue(state != null)
        assertEquals(PermissionStatus.Granted, state?.permissionStatus)
        assertTrue(state?.canProceed == true)
    }

    @Test
    fun onPermissionResult_deniedWithRationale_setsDenied() = runTest {
        val repository = FakeStorageAccessRepository()
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()

        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = true)

        val state = viewModel.uiState.value as? OnboardingUiState.Ready
        assertTrue(state != null)
        assertEquals(PermissionStatus.Denied, state?.permissionStatus)
        assertFalse(state?.canProceed == true)
    }

    @Test
    fun onPermissionResult_deniedWithoutRationale_setsPermanentlyDenied() = runTest {
        val repository = FakeStorageAccessRepository()
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()

        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = false)

        val state = viewModel.uiState.value as? OnboardingUiState.Ready
        assertTrue(state != null)
        assertEquals(PermissionStatus.PermanentlyDenied, state?.permissionStatus)
        assertFalse(state?.canProceed == true)
    }

    @Test
    fun onFolderSelected_addsFolderAndEnablesCanProceed() = runTest {
        val repository = FakeStorageAccessRepository()
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()

        viewModel.onFolderSelected("content://com.android.externalstorage.documents/tree/Videos")
        advanceUntilIdle()

        val state = viewModel.uiState.value as? OnboardingUiState.Ready
        assertTrue(state != null)
        assertEquals(1, state?.selectedFoldersCount)
        assertTrue(state?.canProceed == true)
    }

    @Test
    fun completeOnboarding_invokesCallback() = runTest {
        val repository = FakeStorageAccessRepository()
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()

        var finishedCalled = false
        viewModel.completeOnboarding {
            finishedCalled = true
        }
        advanceUntilIdle()

        assertTrue(finishedCalled)
    }
}
