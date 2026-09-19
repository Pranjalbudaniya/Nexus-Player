package com.nexus.player.feature.onboarding

import com.nexus.player.core.common.storage.StorageAccessMode

enum class PermissionStatus {
    NotRequested,
    Granted,
    Denied,
    PermanentlyDenied
}

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Ready(
        val permissionStatus: PermissionStatus = PermissionStatus.NotRequested,
        val accessMode: StorageAccessMode = StorageAccessMode.ALL_MEDIA,
        val selectedFoldersCount: Int = 0,
        val canProceed: Boolean = false,
        val isCompleted: Boolean = false
    ) : OnboardingUiState
}
