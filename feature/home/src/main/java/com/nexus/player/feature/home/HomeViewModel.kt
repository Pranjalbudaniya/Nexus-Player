package com.nexus.player.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.operations.VideoFileOperationsManager
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProvider
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProvider
import com.nexus.player.feature.home.domain.provider.FoldersSectionProvider
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Snapshot of domain data mapped to UI models for the 4 Home sections.
 */
private data class HomeSectionsSnapshot(
    val continueWatching: List<ContinueWatchingItem>,
    val recentlyAdded: List<RecentVideoItem>,
    val favorites: List<FavoriteVideoItem>,
    val folders: List<FolderItem>
)

/**
 * Snapshot of underlying scanner and storage access status.
 */
private data class HomeSystemStatus(
    val isScanning: Boolean,
    val isPermissionGranted: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val continueWatchingProvider: ContinueWatchingSectionProvider,
    private val recentlyAddedProvider: RecentlyAddedSectionProvider,
    private val favoritesProvider: FavoritesSectionProvider,
    private val foldersProvider: FoldersSectionProvider,
    private val mediaScanOrchestrator: MediaScanOrchestrator,
    private val storageAccessRepository: StorageAccessRepository,
    val thumbnailLoader: ThumbnailLoader,
    val videoRepository: VideoRepository,
    val fileOperationsManager: VideoFileOperationsManager,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    val playbackQueueManager: PlaybackQueueManager = PlaybackQueueManagerImpl()
) : ViewModel() {

    val folders: StateFlow<List<VideoFolder>> = fileOperationsManager
        .getAvailableFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedVideoForMenu = MutableStateFlow<MediaMetadata?>(null)
    val selectedVideoForMenu: StateFlow<MediaMetadata?> = _selectedVideoForMenu.asStateFlow()

    private val sectionsFlow = combine(
        continueWatchingProvider.getContinueWatching(),
        recentlyAddedProvider.getRecentlyAdded(),
        favoritesProvider.getFavorites(),
        foldersProvider.getHomeFolders()
    ) { continueWatching, recentlyAdded, favorites, folders ->
        HomeSectionsSnapshot(
            continueWatching = continueWatching.map { it.toUiItem() },
            recentlyAdded = recentlyAdded.map { it.toUiItem() },
            favorites = favorites.map { it.toUiItem() },
            folders = folders.map { it.toUiItem() }
        )
    }.flowOn(ioDispatcher)

    private val statusFlow = combine(
        mediaScanOrchestrator.isScanning,
        storageAccessRepository.storageAccessState
    ) { isScanning, storageAccess ->
        HomeSystemStatus(
            isScanning = isScanning,
            isPermissionGranted = storageAccess.isPermissionGranted
        )
    }.flowOn(ioDispatcher)

    val uiState: StateFlow<HomeUiState> = combine(
        sectionsFlow,
        statusFlow
    ) { sections, status ->
        val hasAnyMedia = sections.continueWatching.isNotEmpty() ||
                sections.recentlyAdded.isNotEmpty() ||
                sections.favorites.isNotEmpty() ||
                sections.folders.isNotEmpty()

        when {
            !status.isPermissionGranted && !hasAnyMedia -> {
                HomeUiState.Empty(
                    isScanning = status.isScanning,
                    noAccessibleMedia = true
                )
            }
            !hasAnyMedia -> {
                HomeUiState.Empty(
                    isScanning = status.isScanning,
                    noAccessibleMedia = false
                )
            }
            else -> {
                HomeUiState.Success(
                    continueWatching = sections.continueWatching,
                    recentlyAdded = sections.recentlyAdded,
                    favorites = sections.favorites,
                    folders = sections.folders,
                    isScanning = status.isScanning,
                    hasAccessibleMedia = status.isPermissionGranted
                )
            }
        }
    }
    .catch { throwable ->
        emit(HomeUiState.Error(message = throwable.message ?: "An unexpected error occurred"))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState.Loading
    )

    fun playVideo(videoId: String) {
        val currentSuccess = uiState.value as? HomeUiState.Success
        val (items, source) = when {
            currentSuccess?.continueWatching?.any { it.id == videoId } == true -> {
                currentSuccess.continueWatching.map { it.id } to QueueSource.HomeSection("Continue Watching")
            }
            currentSuccess?.recentlyAdded?.any { it.id == videoId } == true -> {
                currentSuccess.recentlyAdded.map { it.id } to QueueSource.HomeSection("Recently Added")
            }
            currentSuccess?.favorites?.any { it.id == videoId } == true -> {
                currentSuccess.favorites.map { it.id } to QueueSource.Favorites
            }
            else -> {
                listOf(videoId) to QueueSource.HomeSection("Home")
            }
        }
        playbackQueueManager.setQueue(
            items = items,
            initialVideoId = videoId,
            source = source
        )
    }

    fun onVideoLongClick(videoId: String) {
        viewModelScope.launch {
            val video = videoRepository.getVideoById(videoId)
            _selectedVideoForMenu.value = video?.toMediaMetadata()
        }
    }

    fun dismissContextMenu() {
        _selectedVideoForMenu.value = null
    }

    fun toggleFavorite(video: MediaMetadata) {
        viewModelScope.launch {
            fileOperationsManager.setFavorite(video.id, !video.isFavorite)
        }
    }

    fun renameVideo(video: MediaMetadata, newName: String, onResult: (Result<MediaMetadata>) -> Unit) {
        viewModelScope.launch {
            val result = fileOperationsManager.renameVideo(video.id, newName)
            onResult(result)
        }
    }

    fun moveVideo(video: MediaMetadata, targetFolderPath: String, onResult: (Result<MediaMetadata>) -> Unit) {
        viewModelScope.launch {
            val result = fileOperationsManager.moveVideo(video.id, targetFolderPath)
            onResult(result)
        }
    }

    fun copyVideo(video: MediaMetadata, targetFolderPath: String, onResult: (Result<MediaMetadata>) -> Unit) {
        viewModelScope.launch {
            val result = fileOperationsManager.copyVideo(video.id, targetFolderPath)
            onResult(result)
        }
    }

    fun deleteVideo(video: MediaMetadata, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            playbackQueueManager.removeItem(video.id)
            val result = fileOperationsManager.deleteVideo(video.id, stageForUndo = true)
            onResult(result)
        }
    }

    fun restoreDeletedVideo(videoId: String, onResult: (Result<MediaMetadata>) -> Unit) {
        viewModelScope.launch {
            val result = fileOperationsManager.restoreDeletedVideo(videoId)
            onResult(result)
        }
    }

    fun purgeStagedDeletions() {
        viewModelScope.launch {
            fileOperationsManager.purgeStagedDeletions()
        }
    }

    fun shareVideo(context: Context, video: MediaMetadata) {
        fileOperationsManager.shareVideo(context, video)
    }

    fun triggerRescan(): Boolean {
        return mediaScanOrchestrator.triggerManualScan()
    }
}
