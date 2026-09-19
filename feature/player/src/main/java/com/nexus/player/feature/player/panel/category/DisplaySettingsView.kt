package com.nexus.player.feature.player.panel.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.feature.player.panel.component.PlayerSettingItem

// Media3 AspectRatioFrameLayout resize mode constants
const val RESIZE_MODE_FIT = 0
const val RESIZE_MODE_FILL = 3
const val RESIZE_MODE_ZOOM = 4

@Composable
fun DisplaySettingsView(
    currentResizeMode: Int,
    onResizeModeSelected: (Int) -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NexusTheme.spacing.medium)
            .testTag("display_settings_view")
    ) {
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Fit to Screen",
            subtitle = "Preserve aspect ratio, add letterbox",
            leadingIcon = Icons.Filled.AspectRatio,
            isSelected = currentResizeMode == RESIZE_MODE_FIT,
            onClick = { onResizeModeSelected(RESIZE_MODE_FIT) },
            testTag = "aspect_ratio_fit"
        )

        PlayerSettingItem(
            title = "Fill Screen",
            subtitle = "Zoom to fill entire viewport",
            leadingIcon = Icons.Filled.CropFree,
            isSelected = currentResizeMode == RESIZE_MODE_ZOOM,
            onClick = { onResizeModeSelected(RESIZE_MODE_ZOOM) },
            testTag = "aspect_ratio_zoom"
        )

        PlayerSettingItem(
            title = "Crop",
            subtitle = "Crop edges to fill 16:9 frame",
            leadingIcon = Icons.Filled.CropFree,
            isSelected = false,
            onClick = { onResizeModeSelected(RESIZE_MODE_ZOOM) },
            testTag = "aspect_ratio_crop"
        )

        PlayerSettingItem(
            title = "Stretch",
            subtitle = "Stretch video to screen boundaries",
            leadingIcon = Icons.Filled.ZoomOutMap,
            isSelected = currentResizeMode == RESIZE_MODE_FILL,
            onClick = { onResizeModeSelected(RESIZE_MODE_FILL) },
            testTag = "aspect_ratio_stretch"
        )

        PlayerSettingItem(
            title = "Original",
            subtitle = "Native pixel aspect ratio",
            leadingIcon = Icons.Filled.AspectRatio,
            isSelected = false,
            onClick = { onResizeModeSelected(RESIZE_MODE_FIT) },
            testTag = "aspect_ratio_original"
        )

        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(NexusTheme.spacing.medium))

        Text(
            text = "Screen & Orientation",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = NexusTheme.spacing.small)
        )

        PlayerSettingItem(
            title = "Toggle Orientation / Fullscreen",
            subtitle = "Switch between portrait and landscape fullscreen",
            leadingIcon = Icons.Filled.ScreenRotation,
            onClick = onToggleOrientation,
            testTag = "display_toggle_orientation"
        )

        PlayerSettingItem(
            title = "Take Screenshot",
            subtitle = "Capture current video frame",
            leadingIcon = Icons.Filled.CropFree,
            onClick = {},
            enabled = false,
            badgeText = "Coming Soon"
        )
    }
}
