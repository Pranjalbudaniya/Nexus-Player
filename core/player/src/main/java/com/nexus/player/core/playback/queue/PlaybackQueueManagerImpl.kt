package com.nexus.player.core.playback.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackQueueManagerImpl @Inject constructor() : PlaybackQueueManager {

    private val lock = Any()

    private var items: List<String> = emptyList()
    private var playbackOrder: List<String> = emptyList()
    private var currentIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var isShuffleEnabled: Boolean = false
    private var isAutoNextEnabled: Boolean = false
    private var source: QueueSource = QueueSource.None

    private val _queueState = MutableStateFlow(PlaybackQueueState())
    override val queueState: StateFlow<PlaybackQueueState> = _queueState.asStateFlow()

    override fun setQueue(
        items: List<String>,
        initialVideoId: String?,
        source: QueueSource
    ) {
        synchronized(lock) {
            val distinctItems = items.distinct()
            this.items = distinctItems
            this.source = source

            if (distinctItems.isEmpty()) {
                clearInternal()
                return
            }

            val targetId = initialVideoId ?: distinctItems.first()
            val targetOriginalIndex = distinctItems.indexOf(targetId).let { if (it >= 0) it else 0 }
            val resolvedId = distinctItems[targetOriginalIndex]

            if (isShuffleEnabled) {
                val others = (distinctItems - resolvedId).shuffled()
                playbackOrder = listOf(resolvedId) + others
                currentIndex = 0
            } else {
                playbackOrder = distinctItems
                currentIndex = targetOriginalIndex
            }

            emitStateLocked()
        }
    }

    override fun playNext(): String? {
        synchronized(lock) {
            if (playbackOrder.isEmpty()) return null

            if (currentIndex + 1 < playbackOrder.size) {
                currentIndex++
                emitStateLocked()
                return playbackOrder[currentIndex]
            } else if (repeatMode == RepeatMode.REPEAT_ALL && playbackOrder.isNotEmpty()) {
                currentIndex = 0
                emitStateLocked()
                return playbackOrder[0]
            }
            return null
        }
    }

    override fun playPrevious(currentPositionMs: Long): PreviousResult {
        synchronized(lock) {
            if (playbackOrder.isEmpty()) return PreviousResult.None

            // If past the threshold, restart the currently playing video
            if (currentPositionMs > PlaybackQueueManager.PREVIOUS_RESTART_THRESHOLD_MS) {
                return PreviousResult.RestartCurrent
            }

            // If within threshold and earlier items exist, navigate back
            if (currentIndex > 0) {
                currentIndex--
                emitStateLocked()
                return PreviousResult.PlayVideo(playbackOrder[currentIndex])
            } else if (repeatMode == RepeatMode.REPEAT_ALL && playbackOrder.size > 1) {
                currentIndex = playbackOrder.lastIndex
                emitStateLocked()
                return PreviousResult.PlayVideo(playbackOrder[currentIndex])
            }

            return PreviousResult.None
        }
    }

    override fun playItem(videoId: String): Boolean {
        synchronized(lock) {
            val index = playbackOrder.indexOf(videoId)
            if (index >= 0) {
                currentIndex = index
                emitStateLocked()
                return true
            }
            return false
        }
    }

    override fun removeItem(videoId: String): String? {
        synchronized(lock) {
            if (!items.contains(videoId)) {
                return _queueState.value.currentVideoId
            }

            val isCurrent = (_queueState.value.currentVideoId == videoId)

            items = items - videoId
            playbackOrder = playbackOrder - videoId

            if (playbackOrder.isEmpty()) {
                clearInternal()
                return null
            }

            if (isCurrent) {
                // Transition to next item if possible, or previous if we were at the end
                val newIndex = currentIndex.coerceIn(0, playbackOrder.size - 1)
                currentIndex = newIndex
                emitStateLocked()
                return playbackOrder[newIndex]
            } else {
                currentIndex = playbackOrder.indexOf(_queueState.value.currentVideoId).coerceIn(0, playbackOrder.size - 1)
                emitStateLocked()
                return _queueState.value.currentVideoId
            }
        }
    }

    override fun clearQueue() {
        synchronized(lock) {
            clearInternal()
        }
    }

    override fun moveItem(fromIndex: Int, toIndex: Int) {
        synchronized(lock) {
            if (fromIndex !in playbackOrder.indices || toIndex !in playbackOrder.indices || fromIndex == toIndex) {
                return
            }

            val activeId = _queueState.value.currentVideoId
            val mutableOrder = playbackOrder.toMutableList()
            val item = mutableOrder.removeAt(fromIndex)
            mutableOrder.add(toIndex, item)
            playbackOrder = mutableOrder

            if (!isShuffleEnabled) {
                val mutableItems = items.toMutableList()
                val itemInItems = mutableItems.removeAt(fromIndex)
                mutableItems.add(toIndex, itemInItems)
                items = mutableItems
            }

            currentIndex = playbackOrder.indexOf(activeId)
            emitStateLocked()
        }
    }

    override fun reorderQueue(newOrder: List<String>) {
        synchronized(lock) {
            if (newOrder.size != playbackOrder.size || !newOrder.containsAll(playbackOrder)) {
                return
            }

            val activeId = _queueState.value.currentVideoId
            playbackOrder = newOrder
            if (!isShuffleEnabled) {
                items = newOrder
            }
            currentIndex = playbackOrder.indexOf(activeId)
            emitStateLocked()
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        synchronized(lock) {
            this.repeatMode = mode
            emitStateLocked()
        }
    }

    override fun cycleRepeatMode(): RepeatMode {
        synchronized(lock) {
            val next = repeatMode.next()
            this.repeatMode = next
            emitStateLocked()
            return next
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        synchronized(lock) {
            if (this.isShuffleEnabled == enabled) return

            this.isShuffleEnabled = enabled
            val currentId = _queueState.value.currentVideoId

            if (enabled) {
                if (currentId != null && items.contains(currentId)) {
                    val others = (items - currentId).shuffled()
                    playbackOrder = listOf(currentId) + others
                    currentIndex = 0
                } else {
                    playbackOrder = items.shuffled()
                    currentIndex = if (playbackOrder.isNotEmpty()) 0 else -1
                }
            } else {
                playbackOrder = items
                currentIndex = if (currentId != null) items.indexOf(currentId).coerceAtLeast(0) else -1
            }

            emitStateLocked()
        }
    }

    override fun toggleShuffle(): Boolean {
        synchronized(lock) {
            setShuffleEnabled(!isShuffleEnabled)
            return isShuffleEnabled
        }
    }

    override fun setAutoNextEnabled(enabled: Boolean) {
        synchronized(lock) {
            this.isAutoNextEnabled = enabled
            emitStateLocked()
        }
    }

    private fun clearInternal() {
        items = emptyList()
        playbackOrder = emptyList()
        currentIndex = -1
        source = QueueSource.None
        _queueState.value = PlaybackQueueState(
            items = emptyList(),
            playbackOrder = emptyList(),
            currentIndex = -1,
            currentVideoId = null,
            previousVideoId = null,
            nextVideoId = null,
            hasPrevious = false,
            hasNext = false,
            repeatMode = repeatMode,
            isShuffleEnabled = isShuffleEnabled,
            isAutoNextEnabled = isAutoNextEnabled,
            source = QueueSource.None
        )
    }

    private fun emitStateLocked() {
        val nav = calculateNavigation(playbackOrder, currentIndex, repeatMode)
        _queueState.value = PlaybackQueueState(
            items = items,
            playbackOrder = playbackOrder,
            currentIndex = currentIndex,
            currentVideoId = nav.currentVideoId,
            previousVideoId = nav.previousVideoId,
            nextVideoId = nav.nextVideoId,
            hasPrevious = nav.hasPrevious,
            hasNext = nav.hasNext,
            repeatMode = repeatMode,
            isShuffleEnabled = isShuffleEnabled,
            isAutoNextEnabled = isAutoNextEnabled,
            source = source
        )
    }

    private data class NavigationInfo(
        val currentVideoId: String?,
        val previousVideoId: String?,
        val nextVideoId: String?,
        val hasPrevious: Boolean,
        val hasNext: Boolean
    )

    private fun calculateNavigation(
        order: List<String>,
        index: Int,
        repeat: RepeatMode
    ): NavigationInfo {
        if (order.isEmpty() || index !in order.indices) {
            return NavigationInfo(
                currentVideoId = null,
                previousVideoId = null,
                nextVideoId = null,
                hasPrevious = false,
                hasNext = false
            )
        }

        val current = order[index]
        val size = order.size

        val (hasPrev, prevId) = when {
            index > 0 -> true to order[index - 1]
            repeat == RepeatMode.REPEAT_ALL && size > 1 -> true to order.last()
            else -> false to null
        }

        val (hasNext, nextId) = when {
            index < size - 1 -> true to order[index + 1]
            repeat == RepeatMode.REPEAT_ALL && size > 1 -> true to order.first()
            else -> false to null
        }

        return NavigationInfo(
            currentVideoId = current,
            previousVideoId = prevId,
            nextVideoId = nextId,
            hasPrevious = hasPrev,
            hasNext = hasNext
        )
    }
}
