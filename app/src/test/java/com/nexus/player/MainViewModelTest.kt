package com.nexus.player

import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.common.storage.StorageAccessState
import com.nexus.player.core.navigation.HomeRoute
import com.nexus.player.core.navigation.OnboardingRoute
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startupTriggersScanWhenOnboardedAndAccessGranted() = runTest {
        val fakeRepo = FakeStorageRepo(
            initialState = StorageAccessState(
                isOnboardingCompleted = true,
                isPermissionGranted = true,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        val fakeOrchestrator = FakeScanOrchestrator()

        val viewModel = MainViewModel(
            storageAccessRepository = fakeRepo,
            mediaScanOrchestrator = fakeOrchestrator
        )

        val state = viewModel.appLaunchState.first { it is AppLaunchState.Ready }
        assertEquals(HomeRoute, (state as AppLaunchState.Ready).startDestination)
        assertEquals(1, fakeOrchestrator.startupScanTriggerCount)
    }

    @Test
    fun startupDoesNotTriggerScanWhenNotOnboarded() = runTest {
        val fakeRepo = FakeStorageRepo(
            initialState = StorageAccessState(
                isOnboardingCompleted = false,
                isPermissionGranted = false,
                accessMode = StorageAccessMode.ALL_MEDIA
            )
        )
        val fakeOrchestrator = FakeScanOrchestrator()

        val viewModel = MainViewModel(
            storageAccessRepository = fakeRepo,
            mediaScanOrchestrator = fakeOrchestrator
        )

        val state = viewModel.appLaunchState.first { it is AppLaunchState.Ready }
        assertEquals(OnboardingRoute, (state as AppLaunchState.Ready).startDestination)
        assertEquals(0, fakeOrchestrator.startupScanTriggerCount)
    }
}

private class FakeStorageRepo(initialState: StorageAccessState) : StorageAccessRepository {
    private val state = MutableStateFlow(initialState)
    override val storageAccessState: Flow<StorageAccessState> = state

    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun setStorageAccessMode(mode: StorageAccessMode) {}
    override suspend fun addSelectedFolderUri(uriString: String) {}
    override suspend fun removeSelectedFolderUri(uriString: String) {}
    override suspend fun clearSelectedFolders() {}
    override fun isPermissionGranted(): Boolean = state.value.isPermissionGranted
    override fun getRequiredPermissions(): List<String> = emptyList()
}

private class FakeScanOrchestrator : MediaScanOrchestrator {
    var startupScanTriggerCount = 0
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    override val isScanning: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun triggerStartupScan(): Boolean {
        startupScanTriggerCount++
        return true
    }

    override fun triggerManualScan(): Boolean = true
    override fun cancelScan() {}
}
