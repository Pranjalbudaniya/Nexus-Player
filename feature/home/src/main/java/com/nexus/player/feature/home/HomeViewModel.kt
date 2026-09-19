package com.nexus.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import com.nexus.player.feature.home.domain.provider.ContinueWatchingSectionProvider
import com.nexus.player.feature.home.domain.provider.FavoritesSectionProvider
import com.nexus.player.feature.home.domain.provider.FoldersSectionProvider
import com.nexus.player.feature.home.domain.provider.RecentlyAddedSectionProvider
import com.nexus.player.core.media.thumbnail.ThumbnailLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
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
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

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
}
