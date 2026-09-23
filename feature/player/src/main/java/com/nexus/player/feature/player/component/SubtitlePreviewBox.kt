package com.nexus.player.feature.player.component

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
import com.nexus.player.core.playback.model.SubtitleAppearance

/**
 * Live interactive preview simulating video playback and subtitle styling.
 */
@Composable
fun SubtitlePreviewBox(
    appearance: SubtitleAppearance,
    modifier: Modifier = Modifier
) {
    val previewTextColor = when (appearance.textColor) {
        SubtitleTextColor.White -> Color.White
        SubtitleTextColor.Yellow -> Color(red = 1.0f, green = 0.92f, blue = 0.23f)
        SubtitleTextColor.Cyan -> Color(red = 0.0f, green = 0.90f, blue = 1.0f)
        SubtitleTextColor.LightGreen -> Color(red = 0.46f, green = 1.0f, blue = 0.01f)
    }

    val alignment = when (appearance.position) {
        SubtitlePosition.Bottom -> Alignment.BottomCenter
        SubtitlePosition.Raised -> Alignment.Center
        SubtitlePosition.Top -> Alignment.TopCenter
    }

    val positionPadding = when (appearance.position) {
        SubtitlePosition.Bottom -> Modifier.padding(bottom = 12.dp)
        SubtitlePosition.Raised -> Modifier.padding(bottom = 0.dp)
        SubtitlePosition.Top -> Modifier.padding(top = 12.dp)
    }

    val textShadow = when (appearance.backgroundStyle) {
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

    val boxBackgroundModifier = if (appearance.backgroundStyle == SubtitleBackgroundStyle.Box) {
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = appearance.backgroundOpacity.coerceIn(0f, 1f)))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    } else {
        Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
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
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = "Subtitle preview demonstration box"
            }
            .testTag("subtitle_preview_box"),
        contentAlignment = alignment
    ) {
        // Inner simulated cinematic bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = alignment
        ) {
            Box(
                modifier = positionPadding
                    .then(boxBackgroundModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sample Subtitle Text Preview",
                    style = TextStyle(
                        color = previewTextColor,
                        fontSize = when (appearance.textSize) {
                            SubtitleTextSize.Small -> 13.sp
                            SubtitleTextSize.Normal -> 16.sp
                            SubtitleTextSize.Large -> 20.sp
                            SubtitleTextSize.ExtraLarge -> 24.sp
                        },
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        shadow = textShadow
                    ),
                    modifier = Modifier.testTag("subtitle_preview_text")
                )
            }
        }
    }
}
