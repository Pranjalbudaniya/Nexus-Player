package com.nexus.player.feature.more.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Result of URL validation for network playback streams.
 */
sealed interface UrlValidationResult {
    data object Empty : UrlValidationResult
    data object Valid : UrlValidationResult
    data class Invalid(val reason: String) : UrlValidationResult
}

/**
 * Validates network video stream URLs.
 * Strictly permits only `http://` and `https://` schemes.
 */
fun validateNetworkUrl(rawUrl: String): UrlValidationResult {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return UrlValidationResult.Empty

    val lower = trimmed.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
        return if (lower.contains("://")) {
            val scheme = lower.substringBefore("://")
            UrlValidationResult.Invalid("Unsupported scheme '$scheme'. Only http:// and https:// links are supported.")
        } else {
            UrlValidationResult.Invalid("URL must start with http:// or https://")
        }
    }

    return try {
        val uri = URI(trimmed)
        if (uri.host.isNullOrBlank()) {
            UrlValidationResult.Invalid("URL is missing a valid host or domain name.")
        } else {
            UrlValidationResult.Valid
        }
    } catch (_: Exception) {
        UrlValidationResult.Invalid("Malformed URL address format.")
    }
}

/**
 * Material 3 dialog for opening and validating direct network video URLs.
 *
 * Provides:
 * - URL input text field with live validation
 * - Clear button when text is present
 * - One-tap clipboard paste button
 * - Play / Open action with loading feedback
 * - Strict rejection of unsupported protocols
 * - Full Material 3 theme token compliance with zero hardcoded colors
 */
@Composable
fun OpenNetworkUrlDialog(
    onPlayUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var urlText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val validationResult = remember(urlText) {
        validateNetworkUrl(urlText)
    }

    val isPlayEnabled = validationResult is UrlValidationResult.Valid && !isLoading

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismissRequest()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Open Network Stream",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = "Enter an HTTP or HTTPS direct video stream address to play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.extraSmall))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("network_url_input"),
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = false,
                    maxLines = 3,
                    isError = validationResult is UrlValidationResult.Invalid,
                    supportingText = {
                        when (validationResult) {
                            is UrlValidationResult.Invalid -> {
                                Text(
                                    text = validationResult.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("network_url_error_text")
                                )
                            }
                            UrlValidationResult.Empty -> {
                                Text(
                                    text = "Supports HTTP & HTTPS direct video streams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            UrlValidationResult.Valid -> {
                                Text(
                                    text = "Ready to play stream.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = if (validationResult is UrlValidationResult.Invalid) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (urlText.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlText = "" },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("network_url_clear_button")
                                        .semantics { contentDescription = "Clear URL" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text?.trim()
                                    if (!clip.isNullOrBlank()) {
                                        urlText = clip
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("network_url_paste_button")
                                    .semantics { contentDescription = "Paste from clipboard" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    shape = shapes.card,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPlayEnabled) {
                        isLoading = true
                        onPlayUrl(urlText.trim())
                    }
                },
                enabled = isPlayEnabled,
                shape = shapes.pill,
                modifier = Modifier.testTag("network_url_play_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("network_url_loading_indicator"),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text("Connecting…")
                } else {
                    Text("Play")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isLoading,
                shape = shapes.pill,
                modifier = Modifier.testTag("network_url_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.testTag("network_url_dialog")
    )
}
