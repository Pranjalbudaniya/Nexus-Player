package com.nexus.player.feature.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.playback.model.ErrorCategory
import com.nexus.player.feature.player.PlayerUiState

/**
 * Clean, user-friendly error recovery overlay for playback failures.
 */
@Composable
fun PlayerErrorOverlay(
    state: PlayerUiState.Error,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("player_error_overlay")
    ) {
        // Top back navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(NexusTheme.spacing.medium)
                .align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("player_error_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Center error content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = NexusTheme.spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val icon = when (state.category) {
                ErrorCategory.NetworkFailure, ErrorCategory.InvalidUrl ->
                    Icons.Filled.SignalWifiConnectedNoInternet4
                ErrorCategory.MissingFile, ErrorCategory.PermissionDenied ->
                    Icons.Filled.VideoFile
                ErrorCategory.UnsupportedCodec, ErrorCategory.UnsupportedContainer ->
                    Icons.Filled.WarningAmber
                else ->
                    Icons.Filled.ErrorOutline
            }

            Icon(
                imageVector = icon,
                contentDescription = "Error icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("player_error_icon")
            )

            Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

            Text(
                text = state.userMessage,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("player_error_message")
            )

            Spacer(modifier = Modifier.height(NexusTheme.spacing.large))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canRetry) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("player_error_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(NexusTheme.spacing.extraSmall))
                        Text(text = "Retry")
                    }

                    Spacer(modifier = Modifier.width(NexusTheme.spacing.medium))
                }

                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("player_error_back_action")
                ) {
                    Text(text = "Go Back")
                }
            }
        }
    }
}
