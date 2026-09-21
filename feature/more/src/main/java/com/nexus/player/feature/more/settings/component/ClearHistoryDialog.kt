package com.nexus.player.feature.more.settings.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Material 3 confirmation dialog for resetting playback resume points and history records.
 * Reassures the user that media files, playlists, favorites, and analytics statistics remain untouched.
 */
@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear Watch History?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "This will reset all playback progress, resume positions, and completion indicators for played videos.\n\nYour video files, playlists, favorites, and watch statistics will NOT be deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear History")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = NexusTheme.customShapes.dialog,
        modifier = modifier
    )
}
