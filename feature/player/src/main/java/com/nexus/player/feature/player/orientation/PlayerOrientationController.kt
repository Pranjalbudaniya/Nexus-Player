package com.nexus.player.feature.player.orientation

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Scoped controller managing player fullscreen and orientation state.
 *
 * Guarantees:
 * - Detects video orientation (landscape vs portrait)
 * - Enables sensor-based auto-rotation ([ActivityInfo.SCREEN_ORIENTATION_SENSOR]) when unlocked,
 *   allowing the player to rotate even when system auto-rotate is toggled off
 * - Provides dedicated orientation lock ([ActivityInfo.SCREEN_ORIENTATION_LOCKED]) to freeze
 *   current orientation on demand
 * - Landscape videos enter landscape orientation in fullscreen
 * - Portrait videos remain in portrait orientation in fullscreen
 * - Hides status bar and navigation bar in fullscreen
 * - ALWAYS restores default app orientation ([ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED])
 *   and system bars on exit or back navigation
 * - Prevents the rest of the application from getting stuck in landscape
 */
@Stable
class PlayerOrientationController(
    private val activity: Activity?,
    private val window: Window?
) {
    var isFullscreen by mutableStateOf(false)
        private set

    var isOrientationLocked by mutableStateOf(false)
        private set

    init {
        // Enable sensor rotation by default in player when unlocked, unless rotating
        if (activity?.isChangingConfigurations != true) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    /**
     * Toggles orientation lock mode.
     * When locked, locks the screen to the current orientation.
     * When unlocked, restores automatic sensor orientation.
     */
    fun toggleOrientationLock() {
        isOrientationLocked = !isOrientationLocked
        if (isOrientationLocked) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            activity?.requestedOrientation = if (isFullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }

    /**
     * Toggles fullscreen state based on whether the video is landscape or portrait.
     */
    fun toggleFullscreen(isLandscapeVideo: Boolean) {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen(isLandscapeVideo)
        }
    }

    /**
     * Enters fullscreen, hides system bars, and adapts orientation to video aspect.
     */
    fun enterFullscreen(isLandscapeVideo: Boolean) {
        isFullscreen = true
        if (!isOrientationLocked) {
            activity?.let { act ->
                act.requestedOrientation = if (isLandscapeVideo) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
        hideSystemBars()
    }

    /**
     * Exits fullscreen, restores system bars, and returns to unlocked sensor or locked state.
     */
    fun exitFullscreen() {
        isFullscreen = false
        activity?.requestedOrientation = if (isOrientationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        showSystemBars()
    }

    /**
     * Resets the activity orientation and window system bars to standard application defaults.
     * Guaranteed to be called on player disposal/exit.
     */
    fun resetToAppDefault() {
        isFullscreen = false
        isOrientationLocked = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        showSystemBars()
    }

    private fun hideSystemBars() {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars() {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun rememberPlayerOrientationController(
    context: Context = LocalContext.current
): PlayerOrientationController {
    val activity = context as? Activity
    val window = activity?.window

    val controller = remember(activity, window) {
        PlayerOrientationController(activity, window)
    }

    // Always reset to application default on dispose, but skip if only changing configurations (rotation)
    DisposableEffect(controller) {
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                controller.resetToAppDefault()
            }
        }
    }

    return controller
}
