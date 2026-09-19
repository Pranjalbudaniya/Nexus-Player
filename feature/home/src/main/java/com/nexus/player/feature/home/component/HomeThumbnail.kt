package com.nexus.player.feature.home.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.thumbnail.ThumbnailLoader

/**
 * Asynchronous, memory-bounded thumbnail renderer for Home cards.
 * Cancels decoding immediately when leaving composition.
 */
@Composable
fun HomeThumbnail(
    mediaUri: String,
    thumbnailLoader: ThumbnailLoader,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    targetWidth: Int = 320,
    targetHeight: Int = 180,
    shape: Shape = NexusTheme.customShapes.thumbnail
) {
    var thumbnailBitmap by remember(mediaUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(mediaUri, thumbnailLoader) {
        if (mediaUri.isNotBlank()) {
            thumbnailBitmap = thumbnailLoader.loadThumbnail(
                mediaUri = mediaUri,
                targetWidth = targetWidth,
                targetHeight = targetHeight
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = thumbnailBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }
}
