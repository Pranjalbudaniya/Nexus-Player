package com.nexus.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.settings.model.AccentColor
import com.nexus.player.core.common.settings.model.ThemeMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.designsystem.theme.NexusAccentColor
import com.nexus.player.core.designsystem.theme.NexusThemeMode
import com.nexus.player.core.designsystem.theme.ThemeConfig
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
    private val mediaScanOrchestrator: MediaScanOrchestrator,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private var hasTriggeredStartupScan = false

    init {
        viewModelScope.launch {
            storageAccessRepository.storageAccessState.collect { accessState ->
                if (!hasTriggeredStartupScan && accessState.isOnboardingCompleted && accessState.hasValidStorageAccess) {
                    hasTriggeredStartupScan = true
                    mediaScanOrchestrator.triggerStartupScan()
                }
            }
        }
    }

    val themeConfig: StateFlow<ThemeConfig> = settingsRepository.settings
        .map { settings ->
            val appearance = settings.appearance
            ThemeConfig(
                themeMode = when (appearance.themeMode) {
                    ThemeMode.SYSTEM -> NexusThemeMode.SYSTEM
                    ThemeMode.LIGHT -> NexusThemeMode.LIGHT
                    ThemeMode.DARK -> NexusThemeMode.DARK
                },
                isAmoled = appearance.useAmoledMode,
                dynamicColor = appearance.useDynamicColor,
                accentColor = when (appearance.accentColor) {
                    AccentColor.DEFAULT -> NexusAccentColor.DEFAULT
                    AccentColor.BLUE -> NexusAccentColor.BLUE
                    AccentColor.TEAL -> NexusAccentColor.TEAL
                    AccentColor.EMERALD -> NexusAccentColor.EMERALD
                    AccentColor.AMBER -> NexusAccentColor.AMBER
                    AccentColor.ROSE -> NexusAccentColor.ROSE
                    AccentColor.PURPLE -> NexusAccentColor.PURPLE
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig()
        )


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
