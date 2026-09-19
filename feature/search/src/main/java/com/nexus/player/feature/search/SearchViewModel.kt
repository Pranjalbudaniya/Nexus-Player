package com.nexus.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.QueueSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel managing reactive global search, debouncing, and playback queue orchestration.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    val playbackQueueManager: PlaybackQueueManager,
    val thumbnailLoader: ThumbnailLoader
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _layoutMode = MutableStateFlow(SearchLayoutMode.LIST)
    val layoutMode: StateFlow<SearchLayoutMode> = _layoutMode.asStateFlow()

    private val _selectedVideoForMenu = MutableStateFlow<MediaMetadata?>(null)
    val selectedVideoForMenu: StateFlow<MediaMetadata?> = _selectedVideoForMenu.asStateFlow()

    private val recentVideosFlow: Flow<List<MediaMetadata>> = videoRepository
        .getRecentlyAddedVideos(limit = 10)
        .map { list -> list.map { it.toMediaMetadata() } }

    private val searchResultsFlow: Flow<Pair<String, List<MediaMetadata>?>> = _searchQuery
        .debounce(200L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                flowOf(trimmed to null)
            } else {
                videoRepository.searchVideos(trimmed)
                    .map { list -> trimmed to list.map { it.toMediaMetadata() } }
            }
        }

    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        searchResultsFlow,
        recentVideosFlow,
        _layoutMode,
        _selectedVideoForMenu
    ) { rawQuery, (resultQuery, results), recent, layout, menuVideo ->
        val trimmed = rawQuery.trim()
        when {
            trimmed.isEmpty() -> {
                SearchUiState.Initial(
                    query = rawQuery,
                    recentVideos = recent,
                    layoutMode = layout,
                    selectedVideoForMenu = menuVideo
                )
            }
            resultQuery != trimmed -> {
                SearchUiState.Searching(
                    query = rawQuery,
                    layoutMode = layout,
                    selectedVideoForMenu = menuVideo
                )
            }
            results == null || results.isEmpty() -> {
                SearchUiState.Empty(
                    query = rawQuery,
                    trimmedQuery = trimmed,
                    layoutMode = layout,
                    selectedVideoForMenu = menuVideo
                )
            }
            else -> {
                SearchUiState.Success(
                    query = rawQuery,
                    trimmedQuery = trimmed,
                    results = results,
                    resultCount = results.size,
                    layoutMode = layout,
                    selectedVideoForMenu = menuVideo
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SearchUiState.Initial()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun toggleLayoutMode() {
        _layoutMode.value = if (_layoutMode.value == SearchLayoutMode.LIST) {
            SearchLayoutMode.GRID
        } else {
            SearchLayoutMode.LIST
        }
    }

    fun onVideoLongClick(video: MediaMetadata) {
        _selectedVideoForMenu.value = video
    }

    fun dismissContextMenu() {
        _selectedVideoForMenu.value = null
    }

    /**
     * Initializes playback queue with search results and starts playback at [videoId].
     */
    fun playVideo(videoId: String) {
        val currentResults = (uiState.value as? SearchUiState.Success)?.results?.map { it.id }
            ?: listOf(videoId)

        playbackQueueManager.setQueue(
            items = currentResults,
            initialVideoId = videoId,
            source = QueueSource.Search(query = _searchQuery.value.trim())
        )
    }
}
