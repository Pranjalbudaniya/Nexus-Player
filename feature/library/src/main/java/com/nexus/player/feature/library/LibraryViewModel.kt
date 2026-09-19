package com.nexus.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.media.model.toMediaMetadata
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.feature.library.preferences.FolderSortOption
import com.nexus.player.feature.library.preferences.LibraryLayoutMode
import com.nexus.player.feature.library.preferences.LibraryPreferencesRepository
import com.nexus.player.feature.library.preferences.LibrarySortDirection
import com.nexus.player.feature.library.preferences.LibrarySortField
import com.nexus.player.feature.library.preferences.LibrarySortOption
import com.nexus.player.feature.library.preferences.LibraryTab
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val folderRepository: FolderRepository,
    private val mediaScanOrchestrator: MediaScanOrchestrator,
    private val storageAccessRepository: StorageAccessRepository,
    private val libraryPreferencesRepository: LibraryPreferencesRepository,
    val thumbnailLoader: ThumbnailLoader,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _selectedVideoForMenu = MutableStateFlow<MediaMetadata?>(null)
    private val _isSortSheetVisible = MutableStateFlow(false)
    private val _isFolderSortSheetVisible = MutableStateFlow(false)

    // Reactively switches Room query whenever user changes sort preferences
    private val videosFlow = libraryPreferencesRepository.sortOption
        .flatMapLatest { sortOption ->
            videoRepository.getAllVideos(sortOption.toVideoSortOrder())
        }
        .map { videos ->
            videos.map { it.toMediaMetadata() }
        }
        .flowOn(ioDispatcher)

    // Reactively queries root folders sorted by current folder sort preference
    private val foldersFlow = libraryPreferencesRepository.folderSortOption
        .flatMapLatest { sortOption ->
            folderRepository.getRootFolders(sortOption.toFolderSortOrder())
        }
        .flowOn(ioDispatcher)

    private val preferencesFlow = combine(
        libraryPreferencesRepository.layoutMode,
        libraryPreferencesRepository.sortOption,
        libraryPreferencesRepository.selectedTab,
        libraryPreferencesRepository.folderSortOption
    ) { mode, sort, tab, folderSort ->
        LibraryPreferencesSnapshot(mode, sort, tab, folderSort)
    }

    private val scannerFlow = combine(
        mediaScanOrchestrator.scanState,
        mediaScanOrchestrator.isScanning,
        storageAccessRepository.storageAccessState
    ) { scanState, isScanning, storageAccess ->
        Triple(scanState, isScanning, storageAccess.isPermissionGranted)
    }

    private val dialogFlow = combine(
        _selectedVideoForMenu,
        _isSortSheetVisible,
        _isFolderSortSheetVisible
    ) { menuVideo, isSortVisible, isFolderSortVisible ->
        Triple(menuVideo, isSortVisible, isFolderSortVisible)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        videosFlow,
        foldersFlow,
        preferencesFlow,
        scannerFlow,
        dialogFlow
    ) { videos, folders, prefs, (scanState, isScanning, isPermissionGranted), (menuVideo, isSortVisible, isFolderSortVisible) ->
        LibraryUiState(
            videos = videos,
            folders = folders,
            selectedTab = prefs.selectedTab,
            layoutMode = prefs.layoutMode,
            sortOption = prefs.sortOption,
            folderSortOption = prefs.folderSortOption,
            scanState = scanState,
            isScanning = isScanning,
            isStorageAccessGranted = isPermissionGranted,
            isLoading = false,
            selectedVideoForMenu = menuVideo,
            isSortSheetVisible = isSortVisible,
            isFolderSortSheetVisible = isFolderSortVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun setSelectedTab(tab: LibraryTab) {
        viewModelScope.launch {
            libraryPreferencesRepository.setSelectedTab(tab)
        }
    }

    fun setLayoutMode(mode: LibraryLayoutMode) {
        viewModelScope.launch {
            libraryPreferencesRepository.setLayoutMode(mode)
        }
    }

    fun setSortField(field: LibrarySortField) {
        viewModelScope.launch {
            val currentOption = uiState.value.sortOption
            val newDirection = if (currentOption.field == field) {
                if (currentOption.direction == LibrarySortDirection.ASCENDING) {
                    LibrarySortDirection.DESCENDING
                } else {
                    LibrarySortDirection.ASCENDING
                }
            } else {
                when (field) {
                    LibrarySortField.NAME -> LibrarySortDirection.ASCENDING
                    else -> LibrarySortDirection.DESCENDING
                }
            }
            libraryPreferencesRepository.setSortOption(
                LibrarySortOption(field = field, direction = newDirection)
            )
        }
    }

    fun setSortOption(sortOption: LibrarySortOption) {
        viewModelScope.launch {
            libraryPreferencesRepository.setSortOption(sortOption)
        }
    }

    fun setFolderSortOption(sortOption: FolderSortOption) {
        viewModelScope.launch {
            libraryPreferencesRepository.setFolderSortOption(sortOption)
        }
    }

    fun toggleSortDirection() {
        viewModelScope.launch {
            if (uiState.value.selectedTab == LibraryTab.VIDEOS) {
                val current = uiState.value.sortOption
                val inverted = if (current.direction == LibrarySortDirection.ASCENDING) {
                    LibrarySortDirection.DESCENDING
                } else {
                    LibrarySortDirection.ASCENDING
                }
                libraryPreferencesRepository.setSortOption(current.copy(direction = inverted))
            } else {
                val current = uiState.value.folderSortOption
                val inverted = if (current.direction == com.nexus.player.feature.library.preferences.FolderSortDirection.ASCENDING) {
                    com.nexus.player.feature.library.preferences.FolderSortDirection.DESCENDING
                } else {
                    com.nexus.player.feature.library.preferences.FolderSortDirection.ASCENDING
                }
                libraryPreferencesRepository.setFolderSortOption(current.copy(direction = inverted))
            }
        }
    }

    fun showSortSheet(visible: Boolean) {
        _isSortSheetVisible.value = visible
    }

    fun showFolderSortSheet(visible: Boolean) {
        _isFolderSortSheetVisible.value = visible
    }

    fun triggerRescan(): Boolean {
        return mediaScanOrchestrator.triggerManualScan()
    }

    fun cancelScan() {
        mediaScanOrchestrator.cancelScan()
    }

    fun onVideoLongClick(video: MediaMetadata) {
        _selectedVideoForMenu.value = video
    }

    fun dismissContextMenu() {
        _selectedVideoForMenu.value = null
    }

    private data class LibraryPreferencesSnapshot(
        val layoutMode: LibraryLayoutMode,
        val sortOption: LibrarySortOption,
        val selectedTab: LibraryTab,
        val folderSortOption: FolderSortOption
    )
}
