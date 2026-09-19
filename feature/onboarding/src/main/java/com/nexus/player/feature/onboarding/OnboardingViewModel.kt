package com.nexus.player.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val storageAccessRepository: StorageAccessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        storageAccessRepository.storageAccessState
            .onEach { accessState ->
                val permissionGranted = storageAccessRepository.isPermissionGranted()
                val permStatus = if (permissionGranted) {
                    PermissionStatus.Granted
                } else {
                    val currentStatus = (_uiState.value as? OnboardingUiState.Ready)?.permissionStatus
                    currentStatus ?: PermissionStatus.NotRequested
                }

                _uiState.update {
                    OnboardingUiState.Ready(
                        permissionStatus = permStatus,
                        accessMode = accessState.accessMode,
                        selectedFoldersCount = accessState.selectedFolderUris.size,
                        canProceed = permissionGranted || accessState.selectedFolderUris.isNotEmpty(),
                        isCompleted = accessState.isOnboardingCompleted
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun getRequiredPermissions(): List<String> {
        return storageAccessRepository.getRequiredPermissions()
    }

    fun refreshPermissionStatus() {
        val granted = storageAccessRepository.isPermissionGranted()
        _uiState.update { current ->
            if (current is OnboardingUiState.Ready) {
                current.copy(
                    permissionStatus = if (granted) PermissionStatus.Granted else current.permissionStatus,
                    canProceed = granted || current.selectedFoldersCount > 0
                )
            } else current
        }
    }

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        val status = when {
            isGranted -> PermissionStatus.Granted
            !shouldShowRationale -> PermissionStatus.PermanentlyDenied
            else -> PermissionStatus.Denied
        }

        _uiState.update { current ->
            if (current is OnboardingUiState.Ready) {
                current.copy(
                    permissionStatus = status,
                    canProceed = isGranted || current.selectedFoldersCount > 0
                )
            } else current
        }
    }

    fun onFolderSelected(uriString: String) {
        viewModelScope.launch {
            storageAccessRepository.addSelectedFolderUri(uriString)
        }
    }

    fun onUseAllMedia() {
        viewModelScope.launch {
            storageAccessRepository.setStorageAccessMode(StorageAccessMode.ALL_MEDIA)
        }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            storageAccessRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }
}
