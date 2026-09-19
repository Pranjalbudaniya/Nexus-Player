package com.nexus.player.feature.player.panel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.DecoderMode
import com.nexus.player.feature.player.PlayerUiState

/**
 * Modal bottom sheet presentation for portrait phone and narrow layouts.
 *
 * Avoids squeezing the video surface into an unwatchable tiny column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsBottomSheet(
    panelState: PlayerPanelState,
    state: PlayerUiState.Ready,
    video: Video?,
    currentDecoderMode: DecoderMode,
    onDecoderModeSelected: (DecoderMode) -> Unit,
    onShareClick: () -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClosePanel,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.testTag("player_settings_bottom_sheet")
    ) {
        PlayerSettingsContent(
            panelState = panelState,
            state = state,
            video = video,
            currentDecoderMode = currentDecoderMode,
            onDecoderModeSelected = onDecoderModeSelected,
            onShareClick = onShareClick,
            onClosePanel = onClosePanel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = NexusTheme.spacing.large)
        )
    }
}
