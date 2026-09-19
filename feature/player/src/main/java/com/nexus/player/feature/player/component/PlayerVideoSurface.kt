package com.nexus.player.feature.player.component

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

/**
 * AndroidView host wrapping Media3 [PlayerView] for video rendering.
 *
 * Configured with:
 * - Pure [Color.Black] background and black shutter to ensure letterboxing is always black
 * - Direct attach/detach tied specifically to the [PlayerView] instance via [AndroidView.onRelease]
 * - Screen kept awake during playback via [PlayerView.setKeepScreenOn]
 * - Custom Compose controls overlay (embedded controller disabled)
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerVideoSurface(
    player: NexusPlayer,
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
            },
            onRelease = { playerView ->
                player.detachPlayerView(playerView)
            }
        )
    }
}
