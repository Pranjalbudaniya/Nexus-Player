package com.nexus.player.feature.player.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
 * - Double tap center: reset zoom or toggle zoom
 * - Vertical drag left half: adjust brightness
 * - Vertical drag right half: adjust volume
 * - Multi-touch pinch: zoom video in/out (1.0x to 4.0x)
 * - Multi-touch or single-finger pan: inspect zoomed frame
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
    onZoomChange: (Float) -> Unit,
    onPanChange: (Float, Float, Float, Float) -> Unit,
    onResetZoom: () -> Unit,
    isZoomed: Boolean,
    modifier: Modifier = Modifier
) {
    val viewConfig = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isZoomed) {
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
                    val isCenterRegion = downX >= size.width * 0.25f && downX <= size.width * 0.75f

                    var isDragging = false
                    var isPanning = false
                    var isMultiTouchActive = false
                    var isLongPressActive = false

                    // Launch long press timer for speed boost
                    val longPressJob = scope.launch {
                        delay(longPressTimeoutMs)
                        if (!isDragging && !isPanning && !isMultiTouchActive) {
                            isLongPressActive = true
                            singleTapJob?.cancel()
                            onSpeedBoost(true)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }

                        // Multi-touch pinch-to-zoom & pan disambiguation
                        if (pressedCount >= 2) {
                            isMultiTouchActive = true
                            longPressJob.cancel()
                            singleTapJob?.cancel()
                            lastTapTime = 0L // Prevent accidental double-tap seeking after pinch
                            isDragging = false

                            val zoomDelta = event.calculateZoom()
                            val panDelta = event.calculatePan()

                            if (zoomDelta != 1f) {
                                onZoomChange(zoomDelta)
                            }
                            if (panDelta.x != 0f || panDelta.y != 0f) {
                                onPanChange(panDelta.x, panDelta.y, size.width.toFloat(), size.height.toFloat())
                            }

                            event.changes.forEach { it.consume() }
                            continue
                        }

                        // If user was in multi-touch gesture and lifts one finger, avoid sudden single-touch gestures
                        if (isMultiTouchActive) {
                            if (pressedCount == 0) {
                                break
                            }
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            // Pointer released
                            longPressJob.cancel()
                            if (isLongPressActive) {
                                onSpeedBoost(false)
                                isLongPressActive = false
                            } else if (!isDragging && !isPanning) {
                                // Tap detection
                                val currentTime = System.currentTimeMillis()
                                val timeSinceLastTap = currentTime - lastTapTime
                                val isDoubleTap = timeSinceLastTap < doubleTapTimeoutMs &&
                                    abs(downX - lastTapX) < touchSlop * 3

                                if (isDoubleTap) {
                                    singleTapJob?.cancel()
                                    lastTapTime = 0L
                                    val leftThreshold = size.width * 0.35f
                                    val rightThreshold = size.width * 0.65f
                                    when {
                                        downX <= leftThreshold -> onSeekBackward()
                                        downX >= rightThreshold -> onSeekForward()
                                        else -> {
                                            // Center double tap: reset zoom if zoomed, otherwise toggle 2x zoom
                                            if (isZoomed) {
                                                onResetZoom()
                                            } else {
                                                onZoomChange(2.0f)
                                            }
                                        }
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

                        // Single finger drag handling: Pan when zoomed vs Volume/Brightness when not zoomed
                        val totalDy = downY - change.position.y
                        val totalDx = abs(downX - change.position.x)

                        if (isZoomed && (isCenterRegion || totalDx > abs(totalDy))) {
                            // User is panning zoomed frame
                            if (!isPanning && (abs(totalDy) > touchSlop || totalDx > touchSlop)) {
                                isPanning = true
                                longPressJob.cancel()
                                singleTapJob?.cancel()
                            }
                            if (isPanning) {
                                val posChange = change.positionChange()
                                onPanChange(posChange.x, posChange.y, size.width.toFloat(), size.height.toFloat())
                                change.consume()
                            }
                        } else {
                            // Vertical brightness / volume drag with axis dominance and edge protection
                            if (!isDragging && !isInEdge && abs(totalDy) > touchSlop * 1.25f && abs(totalDy) > totalDx * 1.5f) {
                                isDragging = true
                                longPressJob.cancel()
                                singleTapJob?.cancel()
                            }

                            if (isDragging) {
                                val dy = change.positionChange().y
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
            }
            .testTag("player_surface_tap_target")
    )
}
