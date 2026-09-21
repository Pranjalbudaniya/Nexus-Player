package com.nexus.player.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Standardized inline screen-level error state composable.
 *
 * Provides a clean, accessible error display with polite screen reader announcements,
 * dual visual indicators (icon + text), and an optional retry action.
 *
 * @param message The user-facing error message (formatted without raw stack traces).
 * @param modifier Layout modifier.
 * @param title Short heading (defaults to "Something went wrong").
 * @param icon Leading error icon.
 * @param actionText Optional retry action label.
 * @param actionIcon Optional icon accompanying retry action.
 * @param onActionClick Optional retry callback.
 */
@Composable
fun NexusErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    icon: ImageVector = Icons.Default.ErrorOutline,
    actionText: String? = "Try Again",
    actionIcon: ImageVector? = Icons.Default.Refresh,
    onActionClick: (() -> Unit)? = null
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Error Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(shapes.badge)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }

            VerticalSpacer(spacing.medium)

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            VerticalSpacer(spacing.small)

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = spacing.medium)
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                VerticalSpacer(spacing.large)

                Button(
                    onClick = onActionClick,
                    shape = shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    if (actionIcon != null) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        HorizontalSpacer(spacing.small)
                    }
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
