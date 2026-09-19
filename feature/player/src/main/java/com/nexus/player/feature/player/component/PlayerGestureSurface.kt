package com.nexus.player.feature.player.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * High-performance gesture surface implementing VLC-style media gestures:
 * - Single tap: toggle player controls
 * - Double tap left (<= 35% width): seek backward
 * - Double tap right (>= 65% width): seek forward
 * - Double tap center: no action
 * - Vertical drag left half: adjust brightness
 * - Vertical drag right half: adjust volume
 * - Press and hold: temporary speed boost (e.g. 2.0x) until released
 */
@Composable
fun PlayerGestureSurface(
    onToggleControls: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onSpeedBoost: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewConfig = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchSlop = viewConfig.touchSlop
                val doubleTapTimeoutMs = 300L
                val longPressTimeoutMs = 400L

                var lastTapTime = 0L
                var lastTapX = 0f
                var singleTapJob: Job? = null

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    val downY = down.position.y

                    // Ignore touches in the outer 4% edges to prevent system navigation back-gesture clashes
                    val edgeMargin = size.width * 0.04f
                    val isInEdge = downX < edgeMargin || downX > (size.width - edgeMargin)

                    val isLeftHalf = downX < size.width * 0.48f
                    val isRightHalf = downX > size.width * 0.52f

                    var isDragging = false
                    var isLongPressActive = false

                    // Launch long press timer
                    val longPressJob = scope.launch {
                        delay(longPressTimeoutMs)
                        if (!isDragging) {
                            isLongPressActive = true
                            singleTapJob?.cancel()
                            onSpeedBoost(true)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            // Pointer released
                            longPressJob.cancel()
                            if (isLongPressActive) {
                                onSpeedBoost(false)
                                isLongPressActive = false
                            } else if (!isDragging) {
                                // Tap detection
                                val currentTime = System.currentTimeMillis()
                                val timeSinceLastTap = currentTime - lastTapTime
                                val isDoubleTap = timeSinceLastTap < doubleTapTimeoutMs &&
                                    abs(downX - lastTapX) < touchSlop * 3

                                if (isDoubleTap) {
                                    singleTapJob?.cancel()
                                    lastTapTime = 0L
                                    // Check double-tap region
                                    val leftThreshold = size.width * 0.35f
                                    val rightThreshold = size.width * 0.65f
                                    when {
                                        downX <= leftThreshold -> onSeekBackward()
                                        downX >= rightThreshold -> onSeekForward()
                                        else -> { /* Center double tap: no action */ }
                                    }
                                } else {
                                    lastTapTime = currentTime
                                    lastTapX = downX
                                    singleTapJob?.cancel()
                                    singleTapJob = scope.launch {
                                        delay(doubleTapTimeoutMs)
                                        onToggleControls()
                                    }
                                }
                            }
                            break
                        }

                        // Check vertical drag with axis dominance and edge protection
                        val totalDy = downY - change.position.y
                        val totalDx = abs(downX - change.position.x)

                        if (!isDragging && !isInEdge && abs(totalDy) > touchSlop * 1.25f && abs(totalDy) > totalDx * 1.5f) {
                            isDragging = true
                            longPressJob.cancel()
                            singleTapJob?.cancel()
                        }

                        if (isDragging) {
                            val dy = change.positionChange().y
                            // Dragging upward: negative dy -> positive adjustment
                            val delta = -dy / size.height
                            if (isLeftHalf) {
                                onBrightnessDelta(delta)
                            } else if (isRightHalf) {
                                onVolumeDelta(delta)
                            }
                            change.consume()
                        }
                    }
                }
            }
            .testTag("player_surface_tap_target")
    )
}
