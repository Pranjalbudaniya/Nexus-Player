package com.nexus.player.core.ui.component.contextmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.VerticalSpacer

private val INVALID_CHARS_REGEX = Regex("[\\\\/:*?\"<>|]")

/**
 * Dialog for renaming a video file with live validation and conflict handling.
 */
@Composable
fun RenameVideoDialog(
    video: MediaMetadata,
    onDismissRequest: () -> Unit,
    onConfirmRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember(video) { mutableStateOf(video.title) }
    val trimmedName = nameInput.trim()

    val validationError = when {
        trimmedName.isBlank() -> "Name cannot be blank"
        trimmedName.contains(INVALID_CHARS_REGEX) -> "Cannot contain \\ / : * ? \" < > |"
        trimmedName.length > 255 -> "Filename exceeds 255 characters"
        else -> null
    }

    val isChanged = trimmedName != video.title.trim()
    val canRename = validationError == null && isChanged

    val spacing = NexusTheme.spacing

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Rename Video",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter a new name for this video file:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                VerticalSpacer(spacing.small)

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Video title") },
                    singleLine = true,
                    isError = validationError != null && isChanged,
                    supportingText = {
                        if (validationError != null && isChanged) {
                            Text(
                                text = validationError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            val ext = video.fileName.substringAfterLast('.', "")
                            if (ext.isNotEmpty()) {
                                Text(
                                    text = "Extension .$ext will be preserved",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canRename) {
                        onConfirmRename(trimmedName)
                        onDismissRequest()
                    }
                },
                enabled = canRename
            ) {
                Text(
                    text = "Rename",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (canRename) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier
    )
}
