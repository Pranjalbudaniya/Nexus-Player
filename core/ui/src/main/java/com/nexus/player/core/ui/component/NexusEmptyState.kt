package com.nexus.player.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Standardized, accessible empty state view for Nexus Player.
 *
 * Adheres strictly to Material 3 design tokens, 4dp grid spacing, and WCAG touch targets.
 *
 * @param icon Leading descriptive icon.
 * @param title Short heading describing the state.
 * @param description Explanatory text giving context and next steps.
 * @param modifier Layout modifier.
 * @param actionText Optional primary action button label.
 * @param actionIcon Optional icon accompanying primary action.
 * @param onActionClick Optional callback triggered by primary action button.
 * @param secondaryActionText Optional secondary button label.
 * @param onSecondaryActionClick Optional callback for secondary button.
 * @param iconTint Themed tint for the main icon.
 */
@Composable
fun NexusEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rounded Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(shapes.badge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(36.dp)
                )
            }

            VerticalSpacer(spacing.medium)

            // Accessible Title
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

            // Supportive Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = spacing.medium)
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                VerticalSpacer(spacing.large)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.smallMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!secondaryActionText.isNullOrBlank() && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            shape = shapes.pill,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            if (secondaryActionIcon != null) {
                                Icon(
                                    imageVector = secondaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                HorizontalSpacer(spacing.small)
                            }
                            Text(
                                text = secondaryActionText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

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
}
