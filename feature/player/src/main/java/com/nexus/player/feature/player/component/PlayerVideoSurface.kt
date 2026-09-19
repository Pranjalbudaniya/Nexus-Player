package com.nexus.player.feature.player.component

import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.nexus.player.core.playback.NexusPlayer
import com.nexus.player.core.playback.model.VideoScaleMode

/**
 * AndroidView host wrapping Media3 [PlayerView] for video rendering.
 *
 * Configured with:
 * - Pure [Color.Black] background and black shutter to ensure letterboxing is always black
 * - Direct attach/detach tied specifically to the [PlayerView] instance via [AndroidView.onRelease]
 * - Screen kept awake during playback via [PlayerView.setKeepScreenOn]
 * - Custom Compose controls overlay (embedded controller disabled)
 * - Decoupled transform layer: zoom, pan, and crop scaling applied strictly to the
 *   video content frame / surface view, leaving [PlayerView.getSubtitleView] anchored
 *   and unclipped at the screen bottom
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerVideoSurface(
    player: NexusPlayer,
    scaleMode: VideoScaleMode = VideoScaleMode.Fit,
    zoom: Float = 1.0f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_video_surface")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    player.attachPlayerView(this)
                }
            },
            update = { playerView ->
                player.attachPlayerView(playerView)
                playerView.resizeMode = scaleMode.resizeMode

                // Calculate base scale according to display mode
                val baseScale = when (scaleMode) {
                    VideoScaleMode.Fit -> 1.0f
                    VideoScaleMode.Fill -> 1.0f
                    VideoScaleMode.Crop -> 1.25f
                    VideoScaleMode.Stretch -> 1.0f
                    VideoScaleMode.Original -> {
                        val videoWidth = player.state.value.videoWidth
                        val videoHeight = player.state.value.videoHeight
                        val viewW = playerView.width
                        val viewH = playerView.height
                        if (viewW > 0 && viewH > 0 && videoWidth > 0 && videoHeight > 0) {
                            if (videoWidth < viewW && videoHeight < viewH) {
                                minOf(
                                    videoWidth.toFloat() / viewW.toFloat(),
                                    videoHeight.toFloat() / viewH.toFloat()
                                ).coerceIn(0.1f, 1.0f)
                            } else {
                                1.0f
                            }
                        } else {
                            1.0f
                        }
                    }
                }

                val totalScale = baseScale * zoom

                // Target only the video content frame or surface view, avoiding subtitleView
                val targetView = (playerView.videoSurfaceView?.parent as? View)?.takeIf { it != playerView }
                    ?: playerView.videoSurfaceView

                targetView?.apply {
                    scaleX = totalScale
                    scaleY = totalScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
            },
            onRelease = { playerView ->
                player.detachPlayerView(playerView)
            }
        )
    }
}
