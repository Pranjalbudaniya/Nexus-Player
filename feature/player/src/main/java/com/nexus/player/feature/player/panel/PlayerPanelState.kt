package com.nexus.player.feature.player.panel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State machine representing the panel visibility lifecycle.
 */
enum class PanelVisibilityState {
    Closed,
    Opening,
    Open,
    Closing;

    val isVisible: Boolean
        get() = this == Opening || this == Open
}

/**
 * Destinations within the hierarchical settings panel.
 */
sealed interface PanelDestination {
    val title: String

    data object Root : PanelDestination {
        override val title: String = "Player Settings"
    }

    data object Playback : PanelDestination {
        override val title: String = "Playback"
    }

    data object Audio : PanelDestination {
        override val title: String = "Audio"
    }

    data object Subtitles : PanelDestination {
        override val title: String = "Subtitles"
    }

    data object Video : PanelDestination {
        override val title: String = "Video"
    }

    data object Display : PanelDestination {
        override val title: String = "Display"
    }

    data object More : PanelDestination {
        override val title: String = "More"
    }
}

/**
 * State holder managing panel transitions and backstack navigation.
 */
@Stable
class PlayerPanelState(
    initialVisibility: PanelVisibilityState = PanelVisibilityState.Closed
) {
    var visibilityState by mutableStateOf(initialVisibility)
        private set

    var currentDestination by mutableStateOf<PanelDestination>(PanelDestination.Root)
        private set

    val backStack = mutableStateListOf<PanelDestination>()

    val isOpen: Boolean
        get() = visibilityState.isVisible

    fun open() {
        if (visibilityState == PanelVisibilityState.Closed || visibilityState == PanelVisibilityState.Closing) {
            visibilityState = PanelVisibilityState.Opening
        }
    }

    fun markOpened() {
        if (visibilityState == PanelVisibilityState.Opening) {
            visibilityState = PanelVisibilityState.Open
        }
    }

    fun close() {
        if (visibilityState == PanelVisibilityState.Open || visibilityState == PanelVisibilityState.Opening) {
            visibilityState = PanelVisibilityState.Closing
        }
    }

    fun markClosed() {
        visibilityState = PanelVisibilityState.Closed
        backStack.clear()
        currentDestination = PanelDestination.Root
    }

    fun navigateTo(destination: PanelDestination) {
        if (destination != currentDestination) {
            backStack.add(currentDestination)
            currentDestination = destination
        }
    }

    /**
     * Navigates back.
     * @return true if popped a sub-destination, false if was already at Root.
     */
    fun navigateBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentDestination = backStack.removeAt(backStack.size - 1)
            true
        } else {
            false
        }
    }
}

@Composable
fun rememberPlayerPanelState(
    initialVisibility: PanelVisibilityState = PanelVisibilityState.Closed
): PlayerPanelState = remember {
    PlayerPanelState(initialVisibility)
}
