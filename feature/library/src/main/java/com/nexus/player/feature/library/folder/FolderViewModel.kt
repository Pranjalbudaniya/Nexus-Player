package com.nexus.player.feature.library.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.navigation.FolderRoute
import com.nexus.player.core.playback.queue.PlaybackQueueManager
import com.nexus.player.core.playback.queue.PlaybackQueueManagerImpl
import com.nexus.player.core.playback.queue.QueueSource
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibraryPreferencesRepository
import com.nexus.player.feature.library.preferences.LibrarySortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.nexus.player.core.media.operations.VideoFileOperationsManager

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderRepository: FolderRepository,
    private val libraryPreferencesRepository: LibraryPreferencesRepository,
    val thumbnailLoader: ThumbnailLoader,
    val fileOperationsManager: VideoFileOperationsManager,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    val playbackQueueManager: PlaybackQueueManager = PlaybackQueueManagerImpl()
) : ViewModel() {

    private val folderRoute: FolderRoute = runCatching {
        savedStateHandle.toRoute<FolderRoute>()
    }.getOrElse {
        FolderRoute(
            folderPath = savedStateHandle.get<String>("folderPath").orEmpty(),
            folderName = savedStateHandle.get<String>("folderName").orEmpty()
        )
    }

    val folderPath: String = folderRoute.folderPath
    val folderName: String = folderRoute.folderName.ifEmpty {
        folderPath.substringAfterLast('/').ifEmpty { "Folder" }
    }

    private val _selectedVideoForMenu = MutableStateFlow<MediaMetadata?>(null)
    private val _isSortSheetVisible = MutableStateFlow(false)

    // Child subfolders inside current directory
    private val subfoldersFlow = folderRepository.getChildFolders(folderPath)
        .flowOn(ioDispatcher)

    // Contained videos inside current directory, sorted reactively
    private val videosFlow = libraryPreferencesRepository.sortOption
        .flatMapLatest { sortOption ->
            folderRepository.getVideosInFolder(folderPath, sortOption.toVideoSortOrder())
        }
        .map { videos ->
            videos.map { it.toMediaMetadata() }
        }
        .flowOn(ioDispatcher)

    private val preferencesFlow = combine(
        libraryPreferencesRepository.layoutMode,
        libraryPreferencesRepository.sortOption
    ) { mode, sort -> mode to sort }

    // All available video folders on device for Move / Copy dialogs
    private val allFoldersFlow = fileOperationsManager.getAvailableFolders()
        .flowOn(ioDispatcher)

    private val dialogFlow = combine(
        _selectedVideoForMenu,
        _isSortSheetVisible
    ) { menuVideo, isSortVisible -> menuVideo to isSortVisible }

    val uiState: StateFlow<FolderUiState> = combine(
        subfoldersFlow,
        videosFlow,
        preferencesFlow,
        dialogFlow,
        allFoldersFlow
    ) { subfolders, videos, (layoutMode, sortOption), (menuVideo, isSortVisible), allFolders ->
        FolderUiState(
            folderPath = folderPath,
            folderName = folderName,
            subfolders = subfolders,
            videos = videos,
            layoutMode = layoutMode,
            sortOption = sortOption,
            isLoading = false,
            selectedVideoForMenu = menuVideo,
            isSortSheetVisible = isSortVisible,
            allFolders = allFolders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FolderUiState(
            folderPath = folderPath,
            folderName = folderName,
            isLoading = true
        )
    )

    fun setLayoutMode(mode: LibraryLayoutMode) {
        viewModelScope.launch {
            libraryPreferencesRepository.setLayoutMode(mode)
        }
    }

    fun setSortOption(sortOption: LibrarySortOption) {
        viewModelScope.launch {
            libraryPreferencesRepository.setSortOption(sortOption)
        }
    }

    fun showSortSheet(visible: Boolean) {
        _isSortSheetVisible.value = visible
    }

    fun onVideoLongClick(video: MediaMetadata) {
        _selectedVideoForMenu.value = video
    }

    fun dismissContextMenu() {
        _selectedVideoForMenu.value = null
    }

    fun playVideo(videoId: String) {
        val currentVideos = uiState.value.videos.map { it.id }.ifEmpty { listOf(videoId) }
        playbackQueueManager.setQueue(
            items = currentVideos,
            initialVideoId = videoId,
            source = QueueSource.Folder(folderPath = folderPath, folderName = folderName)
        )
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
}
