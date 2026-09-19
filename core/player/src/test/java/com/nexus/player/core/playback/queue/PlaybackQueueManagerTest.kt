package com.nexus.player.core.playback.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackQueueManagerTest {

    private lateinit var queueManager: PlaybackQueueManagerImpl

    @Before
    fun setUp() {
        queueManager = PlaybackQueueManagerImpl()
    }

    @Test
    fun setQueue_initializesQueueWithCorrectOrderAndItem() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2", source = QueueSource.Folder("/storage/Movies", "Movies"))

        val state = queueManager.queueState.value
        assertEquals(3, state.size)
        assertEquals("vid_2", state.currentVideoId)
        assertEquals(1, state.currentIndex)
        assertEquals("vid_1", state.previousVideoId)
        assertEquals("vid_3", state.nextVideoId)
        assertTrue(state.hasPrevious)
        assertTrue(state.hasNext)
        assertTrue(state.source is QueueSource.Folder)
        assertEquals("/storage/Movies", (state.source as QueueSource.Folder).folderPath)
    }

    @Test
    fun setQueue_emptyList_clearsQueue() {
        queueManager.setQueue(emptyList())

        val state = queueManager.queueState.value
        assertTrue(state.isEmpty)
        assertEquals(-1, state.currentIndex)
        assertNull(state.currentVideoId)
        assertFalse(state.hasPrevious)
        assertFalse(state.hasNext)
    }

    @Test
    fun setQueue_singleItem_hasNoPreviousOrNext() {
        queueManager.setQueue(listOf("vid_single"), "vid_single")

        val state = queueManager.queueState.value
        assertEquals(1, state.size)
        assertEquals("vid_single", state.currentVideoId)
        assertEquals(0, state.currentIndex)
        assertFalse(state.hasPrevious)
        assertFalse(state.hasNext)
    }

    @Test
    fun playNext_advancesQueueAndReturnsNextId() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_1")

        val nextId = queueManager.playNext()
        assertEquals("vid_2", nextId)
        assertEquals("vid_2", queueManager.queueState.value.currentVideoId)
        assertEquals(1, queueManager.queueState.value.currentIndex)

        val nextId2 = queueManager.playNext()
        assertEquals("vid_3", nextId2)
        assertEquals(2, queueManager.queueState.value.currentIndex)

        // At end with repeat OFF
        val nextId3 = queueManager.playNext()
        assertNull(nextId3)
        assertEquals(2, queueManager.queueState.value.currentIndex)
    }

    @Test
    fun playPrevious_pastThreshold_returnsRestartCurrentWithoutChangingIndex() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        // 5000ms is > threshold (3000ms)
        val result = queueManager.playPrevious(currentPositionMs = 5000L)
        assertTrue(result is PreviousResult.RestartCurrent)
        assertEquals(1, queueManager.queueState.value.currentIndex)
        assertEquals("vid_2", queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun playPrevious_withinThreshold_movesToPreviousItem() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        // 1500ms is <= threshold (3000ms)
        val result = queueManager.playPrevious(currentPositionMs = 1500L)
        assertTrue(result is PreviousResult.PlayVideo)
        assertEquals("vid_1", (result as PreviousResult.PlayVideo).videoId)
        assertEquals(0, queueManager.queueState.value.currentIndex)
        assertEquals("vid_1", queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun playPrevious_atStartWithinThreshold_returnsNone() {
        val videos = listOf("vid_1", "vid_2")
        queueManager.setQueue(videos, initialVideoId = "vid_1")

        val result = queueManager.playPrevious(currentPositionMs = 1000L)
        assertTrue(result is PreviousResult.None)
        assertEquals(0, queueManager.queueState.value.currentIndex)
    }

    @Test
    fun repeatAll_loopsNextAndPrevious() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_3")
        queueManager.setRepeatMode(RepeatMode.REPEAT_ALL)

        val state = queueManager.queueState.value
        assertTrue(state.hasNext)
        assertEquals("vid_1", state.nextVideoId)

        // Next at end loops to index 0
        val nextId = queueManager.playNext()
        assertEquals("vid_1", nextId)
        assertEquals(0, queueManager.queueState.value.currentIndex)

        // Previous at index 0 loops to end
        val prevResult = queueManager.playPrevious(currentPositionMs = 500L)
        assertTrue(prevResult is PreviousResult.PlayVideo)
        assertEquals("vid_3", (prevResult as PreviousResult.PlayVideo).videoId)
        assertEquals(2, queueManager.queueState.value.currentIndex)
    }

    @Test
    fun cycleRepeatMode_cyclesThroughExpectedSequence() {
        assertEquals(RepeatMode.OFF, queueManager.queueState.value.repeatMode)

        val m1 = queueManager.cycleRepeatMode()
        assertEquals(RepeatMode.REPEAT_ALL, m1)
        assertEquals(RepeatMode.REPEAT_ALL, queueManager.queueState.value.repeatMode)

        val m2 = queueManager.cycleRepeatMode()
        assertEquals(RepeatMode.REPEAT_ONE, m2)
        assertEquals(RepeatMode.REPEAT_ONE, queueManager.queueState.value.repeatMode)

        val m3 = queueManager.cycleRepeatMode()
        assertEquals(RepeatMode.OFF, m3)
        assertEquals(RepeatMode.OFF, queueManager.queueState.value.repeatMode)
    }

    @Test
    fun shuffle_preservesOriginalQueueAndRetainsCurrentItem() {
        val videos = listOf("vid_1", "vid_2", "vid_3", "vid_4", "vid_5")
        queueManager.setQueue(videos, initialVideoId = "vid_3")

        // Enable shuffle
        queueManager.setShuffleEnabled(true)
        val shuffledState = queueManager.queueState.value

        assertTrue(shuffledState.isShuffleEnabled)
        assertEquals(videos, shuffledState.items) // original order unchanged
        assertEquals("vid_3", shuffledState.currentVideoId) // current item not changed
        assertEquals(0, shuffledState.currentIndex) // current item positioned first in active order
        assertEquals("vid_3", shuffledState.playbackOrder.first())
        assertEquals(5, shuffledState.playbackOrder.size)
        assertTrue(shuffledState.playbackOrder.containsAll(videos))

        // Disable shuffle
        queueManager.setShuffleEnabled(false)
        val restoredState = queueManager.queueState.value

        assertFalse(restoredState.isShuffleEnabled)
        assertEquals(videos, restoredState.playbackOrder)
        assertEquals(2, restoredState.currentIndex)
        assertEquals("vid_3", restoredState.currentVideoId)
    }

    @Test
    fun playItem_jumpsToSpecificId() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_1")

        val success = queueManager.playItem("vid_3")
        assertTrue(success)
        assertEquals(2, queueManager.queueState.value.currentIndex)
        assertEquals("vid_3", queueManager.queueState.value.currentVideoId)

        val fail = queueManager.playItem("non_existent")
        assertFalse(fail)
    }

    @Test
    fun removeItem_nonCurrentItem_removesAndPreservesPlayback() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        val result = queueManager.removeItem("vid_1")
        assertEquals("vid_2", result)
        assertEquals(listOf("vid_2", "vid_3"), queueManager.queueState.value.items)
        assertEquals(0, queueManager.queueState.value.currentIndex)
        assertEquals("vid_2", queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun removeItem_currentItem_safelyTransitionsToNext() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        val result = queueManager.removeItem("vid_2")
        assertEquals("vid_3", result)
        assertEquals("vid_3", queueManager.queueState.value.currentVideoId)
        assertEquals(1, queueManager.queueState.value.currentIndex)
        assertEquals(listOf("vid_1", "vid_3"), queueManager.queueState.value.items)
    }

    @Test
    fun removeItem_lastItem_transitionsToPrevious() {
        val videos = listOf("vid_1", "vid_2")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        val result = queueManager.removeItem("vid_2")
        assertEquals("vid_1", result)
        assertEquals("vid_1", queueManager.queueState.value.currentVideoId)
        assertEquals(0, queueManager.queueState.value.currentIndex)
    }

    @Test
    fun removeItem_soleItem_clearsQueue() {
        queueManager.setQueue(listOf("vid_only"), initialVideoId = "vid_only")

        val result = queueManager.removeItem("vid_only")
        assertNull(result)
        assertTrue(queueManager.queueState.value.isEmpty)
        assertNull(queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun moveItem_shiftsItemInOrder() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_1")

        queueManager.moveItem(fromIndex = 0, toIndex = 2)
        assertEquals(listOf("vid_2", "vid_3", "vid_1"), queueManager.queueState.value.playbackOrder)
        assertEquals(2, queueManager.queueState.value.currentIndex)
        assertEquals("vid_1", queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun reorderQueue_updatesPlaybackOrder() {
        val videos = listOf("vid_1", "vid_2", "vid_3")
        queueManager.setQueue(videos, initialVideoId = "vid_2")

        queueManager.reorderQueue(listOf("vid_3", "vid_2", "vid_1"))
        assertEquals(listOf("vid_3", "vid_2", "vid_1"), queueManager.queueState.value.playbackOrder)
        assertEquals(1, queueManager.queueState.value.currentIndex)
        assertEquals("vid_2", queueManager.queueState.value.currentVideoId)
    }

    @Test
    fun clearQueue_resetsAllState() {
        val videos = listOf("vid_1", "vid_2")
        queueManager.setQueue(videos, initialVideoId = "vid_1")

        queueManager.clearQueue()
        assertTrue(queueManager.queueState.value.isEmpty)
        assertNull(queueManager.queueState.value.currentVideoId)
        assertEquals(-1, queueManager.queueState.value.currentIndex)
    }
}
