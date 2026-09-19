package com.nexus.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.navigation.HomeRoute
import com.nexus.player.core.navigation.NexusRoute
import com.nexus.player.core.navigation.OnboardingRoute
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppLaunchState {
    data object Loading : AppLaunchState
    data class Ready(val startDestination: NexusRoute) : AppLaunchState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    storageAccessRepository: StorageAccessRepository,
    private val mediaScanOrchestrator: MediaScanOrchestrator
) : ViewModel() {

    init {
        viewModelScope.launch {
            storageAccessRepository.storageAccessState.collect { accessState ->
                if (accessState.isOnboardingCompleted && accessState.hasValidStorageAccess) {
                    mediaScanOrchestrator.triggerStartupScan()
                }
            }
        }
    }

    val appLaunchState: StateFlow<AppLaunchState> = storageAccessRepository.storageAccessState
        .map { accessState ->
            val destination = if (accessState.isOnboardingCompleted && accessState.hasValidStorageAccess) {
                HomeRoute
            } else {
                OnboardingRoute
            }
            AppLaunchState.Ready(destination)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLaunchState.Loading
        )
}
