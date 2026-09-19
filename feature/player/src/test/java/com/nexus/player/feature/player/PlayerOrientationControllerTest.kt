package com.nexus.player.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import com.nexus.player.feature.player.orientation.PlayerOrientationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerOrientationControllerTest {

    private lateinit var activity: Activity
    private lateinit var controller: PlayerOrientationController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        controller = PlayerOrientationController(activity, activity.window)
    }

    @Test
    fun init_enablesSensorOrientationByDefault() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, activity.requestedOrientation)
        assertFalse(controller.isOrientationLocked)
    }

    @Test
    fun enterFullscreen_landscapeVideo_requestsLandscapeOrientation() {
        controller.enterFullscreen(isLandscapeVideo = true)

        assertTrue(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun enterFullscreen_portraitVideo_requestsPortraitOrientation() {
        controller.enterFullscreen(isLandscapeVideo = false)

        assertTrue(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun exitFullscreen_restoresSensorOrientation() {
        controller.enterFullscreen(isLandscapeVideo = true)
        controller.exitFullscreen()

        assertFalse(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, activity.requestedOrientation)
    }

    @Test
    fun orientationLock_togglesBetweenLockedAndSensor() {
        assertFalse(controller.isOrientationLocked)

        controller.toggleOrientationLock()
        assertTrue(controller.isOrientationLocked)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, activity.requestedOrientation)

        controller.toggleOrientationLock()
        assertFalse(controller.isOrientationLocked)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, activity.requestedOrientation)
    }

    @Test
    fun exitFullscreen_whenLocked_restoresLockedOrientation() {
        controller.toggleOrientationLock()
        controller.enterFullscreen(isLandscapeVideo = true)
        controller.exitFullscreen()

        assertFalse(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, activity.requestedOrientation)
    }

    @Test
    fun resetToAppDefault_restoresUnspecifiedOrientation() {
        controller.enterFullscreen(isLandscapeVideo = true)
        controller.resetToAppDefault()

        assertFalse(controller.isFullscreen)
        assertFalse(controller.isOrientationLocked)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
    }

    @Test
    fun toggleFullscreen_cyclesBetweenFullscreenAndRestored() {
        controller.toggleFullscreen(isLandscapeVideo = true)
        assertTrue(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)

        controller.toggleFullscreen(isLandscapeVideo = true)
        assertFalse(controller.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR, activity.requestedOrientation)
    }
}
