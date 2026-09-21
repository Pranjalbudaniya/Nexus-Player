package com.nexus.player.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Centered, accessible progress indicator matching Material 3 primary color role.
 *
 * Supports an optional status label for operations such as network connecting,
 * media indexing, or metadata loading.
 */
@Composable
fun NexusLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null
) {
    val spacing = NexusTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = listOfNotNull(label, description).joinToString(". ").ifEmpty { "Loading" }
            },
        contentAlignment = Alignment.Center
    ) {
        if (label.isNullOrBlank() && description.isNullOrBlank()) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                if (!label.isNullOrBlank()) {
                    VerticalSpacer(spacing.medium)
                    Text(
                        text = label,
                        style = if (description != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        color = if (description != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!description.isNullOrBlank()) {
                    VerticalSpacer(spacing.extraSmall)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
