package com.nexus.player.feature.more.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.feature.more.analytics.domain.GetWatchAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val getWatchAnalyticsUseCase: GetWatchAnalyticsUseCase,
    val playbackQueueManager: PlaybackQueueManager,
    val thumbnailLoader: ThumbnailLoader,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _isClearDialogOpen = MutableStateFlow(false)
    val isClearDialogOpen: StateFlow<Boolean> = _isClearDialogOpen.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        videoRepository.getAllHistoryVideos(),
        _isClearDialogOpen
    ) { historyVideos, isDialogOpen ->
        val analyticsData = getWatchAnalyticsUseCase(historyVideos)
        if (!analyticsData.hasHistory) {
            AnalyticsUiState.Empty
        } else {
            AnalyticsUiState.Success(
                data = analyticsData,
                isClearDialogOpen = isDialogOpen
            )
        }
    }.catch { error ->
        emit(AnalyticsUiState.Error(error.message ?: "Failed to load watch statistics"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AnalyticsUiState.Loading
    )

    fun playVideo(videoId: String) {
        val currentState = uiState.value
        val queueItems = if (currentState is AnalyticsUiState.Success) {
            val mostPlayedIds = currentState.data.mostPlayed.map { it.id }
            if (mostPlayedIds.contains(videoId)) mostPlayedIds else listOf(videoId)
        } else {
            listOf(videoId)
        }

        playbackQueueManager.setQueue(
            items = queueItems,
            initialVideoId = videoId,
            source = QueueSource.History
        )
    }

    fun setClearDialogOpen(open: Boolean) {
        _isClearDialogOpen.value = open
    }

    fun clearAllAnalytics() {
        viewModelScope.launch(ioDispatcher) {
            videoRepository.clearAllAnalyticsAndHistory()
            _isClearDialogOpen.value = false
        }
    }
}
