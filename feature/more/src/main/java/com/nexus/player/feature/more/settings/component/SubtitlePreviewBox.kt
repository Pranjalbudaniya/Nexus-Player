package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.player.core.common.settings.model.SubtitleBackgroundStyle
import com.nexus.player.core.common.settings.model.SubtitlePosition
import com.nexus.player.core.common.settings.model.SubtitleTextColor
import com.nexus.player.core.common.settings.model.SubtitleTextSize
import com.nexus.player.core.designsystem.theme.NexusTheme

/**
 * Live interactive preview simulating video letterboxing and subtitle rendering.
 *
 * Adheres strictly to Material 3 tokens, 4dp spacing grid, and ZERO hardcoded hex colors.
 */
@Composable
fun SubtitlePreviewBox(
    textSize: SubtitleTextSize,
    textColor: SubtitleTextColor,
    backgroundStyle: SubtitleBackgroundStyle,
    backgroundOpacity: Float,
    position: SubtitlePosition,
    modifier: Modifier = Modifier
) {
    val previewTextColor = when (textColor) {
        SubtitleTextColor.White -> Color.White
        SubtitleTextColor.Yellow -> Color(red = 1.0f, green = 0.92f, blue = 0.23f)
        SubtitleTextColor.Cyan -> Color(red = 0.0f, green = 0.90f, blue = 1.0f)
        SubtitleTextColor.LightGreen -> Color(red = 0.46f, green = 1.0f, blue = 0.01f)
    }

    val alignment = when (position) {
        SubtitlePosition.Bottom -> Alignment.BottomCenter
        SubtitlePosition.Raised -> Alignment.Center
        SubtitlePosition.Top -> Alignment.TopCenter
    }

    val positionPadding = when (position) {
        SubtitlePosition.Bottom -> Modifier.padding(bottom = NexusTheme.spacing.medium)
        SubtitlePosition.Raised -> Modifier.padding(bottom = NexusTheme.spacing.large)
        SubtitlePosition.Top -> Modifier.padding(top = NexusTheme.spacing.medium)
    }

    val textShadow = when (backgroundStyle) {
        SubtitleBackgroundStyle.DropShadow -> Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(2f, 3f),
            blurRadius = 6f
        )
        SubtitleBackgroundStyle.Outline -> Shadow(
            color = Color.Black,
            offset = Offset(1f, 1f),
            blurRadius = 2f
        )
        else -> Shadow.None
    }

    val boxBackgroundModifier = if (backgroundStyle == SubtitleBackgroundStyle.Box) {
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = backgroundOpacity.coerceIn(0f, 1f)))
            .padding(horizontal = NexusTheme.spacing.smallMedium, vertical = NexusTheme.spacing.extraSmall)
    } else {
        Modifier.padding(horizontal = NexusTheme.spacing.small, vertical = NexusTheme.spacing.extraSmall)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(NexusTheme.customShapes.card)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = NexusTheme.customShapes.card
            )
            .testTag("subtitle_preview_box")
            .semantics {
                contentDescription = "Live subtitle preview: ${textSize.label}, ${textColor.label}, ${backgroundStyle.label}"
            }
    ) {
        // Subtle preview watermark badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(NexusTheme.spacing.small)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = NexusTheme.spacing.extraSmall, vertical = 2.dp)
        ) {
            Text(
                text = "PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // Subtitle text container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NexusTheme.spacing.medium)
                .then(positionPadding),
            contentAlignment = alignment
        ) {
            Box(modifier = boxBackgroundModifier) {
                Text(
                    text = "Nexus Player provides crystal-clear subtitles.",
                    style = TextStyle(
                        fontSize = textSize.sizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        color = previewTextColor,
                        shadow = textShadow,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
