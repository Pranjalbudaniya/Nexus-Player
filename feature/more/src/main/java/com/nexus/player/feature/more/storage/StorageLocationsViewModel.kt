package com.nexus.player.feature.more.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.player.core.common.network.Dispatcher
import com.nexus.player.core.common.network.NexusDispatchers
import com.nexus.player.core.common.settings.SettingsRepository
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.common.storage.StorageAccessRepository
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.scanner.orchestrator.MediaScanOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageLocationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageAccessRepository: StorageAccessRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaScanOrchestrator: MediaScanOrchestrator,
    private val videoRepository: VideoRepository,
    @Dispatcher(NexusDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _folderPendingRemoval = MutableStateFlow<StorageFolderItem?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val scanInfoFlow = combine(
        mediaScanOrchestrator.scanState,
        mediaScanOrchestrator.isScanning
    ) { state, scanning -> state to scanning }

    private val removalAndMessageFlow = combine(
        _folderPendingRemoval,
        _userMessage
    ) { removal, msg -> removal to msg }

    val uiState: StateFlow<StorageLocationsUiState> = combine(
        storageAccessRepository.storageAccessState,
        settingsRepository.settings,
        videoRepository.getFolders(),
        scanInfoFlow,
        removalAndMessageFlow
    ) { accessState, settings, dbFolders, (scanState, isScanning), (pendingRemoval, message) ->

        val excluded = settings.library.excludedFolders + accessState.excludedFolderPaths

        val indexedFolders = when (accessState.accessMode) {
            StorageAccessMode.ALL_MEDIA -> {
                dbFolders.map { folder ->
                    StorageFolderItem(
                        uriOrPath = folder.folderPath,
                        displayName = folder.folderName,
                        isSafFolder = false,
                        videoCount = folder.videoCount,
                        isAccessible = true,
                        lastModified = folder.lastModified
                    )
                }
            }
            StorageAccessMode.SELECTED_FOLDERS -> {
                val persistedUris = try {
                    context.contentResolver.persistedUriPermissions
                } catch (_: Exception) {
                    emptyList()
                }

                accessState.selectedFolderUris.map { uriString ->
                    val uri = Uri.parse(uriString)
                    val displayName = extractSafDisplayName(uri)
                    val isGranted = persistedUris.any { it.uri == uri && it.isReadPermission }
                    val matchingDbFolder = dbFolders.find {
                        it.folderPath == uriString || it.folderPath.startsWith(uriString)
                    }

                    StorageFolderItem(
                        uriOrPath = uriString,
                        displayName = displayName,
                        isSafFolder = true,
                        videoCount = matchingDbFolder?.videoCount ?: 0,
                        isAccessible = isGranted,
                        lastModified = matchingDbFolder?.lastModified ?: 0L
                    )
                }
            }
        }

        StorageLocationsUiState(
            accessMode = accessState.accessMode,
            isPermissionGranted = accessState.isPermissionGranted,
            indexedFolders = indexedFolders,
            excludedFolders = excluded,
            scanState = scanState,
            isScanning = isScanning,
            folderPendingRemoval = pendingRemoval,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StorageLocationsUiState()
    )

    fun setStorageAccessMode(mode: StorageAccessMode) {
        viewModelScope.launch(ioDispatcher) {
            storageAccessRepository.setStorageAccessMode(mode)
        }
    }

    fun addFolderUri(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Ensure persistent access
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                _userMessage.value = "Cannot access restricted or system-protected directory."
                return@launch
            } catch (e: Exception) {
                _userMessage.value = "Failed to grant folder permissions: ${e.message}"
                return@launch
            }

            val uriString = uri.toString()
            val currentFolders = uiState.value.indexedFolders
            if (currentFolders.any { it.uriOrPath == uriString }) {
                _userMessage.value = "Folder is already indexed."
                return@launch
            }

            storageAccessRepository.addSelectedFolderUri(uriString)
            mediaScanOrchestrator.triggerLocationScan(uriString)
            _userMessage.value = "Folder added and scan initiated."
        }
    }

    fun requestRemoveFolder(folder: StorageFolderItem) {
        _folderPendingRemoval.value = folder
    }

    fun dismissRemoveFolder() {
        _folderPendingRemoval.value = null
    }

    fun confirmRemoveFolder(deleteVideos: Boolean) {
        val folder = _folderPendingRemoval.value ?: return
        viewModelScope.launch(ioDispatcher) {
            if (folder.isSafFolder) {
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(folder.uriOrPath),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                storageAccessRepository.removeSelectedFolderUri(folder.uriOrPath)
            }

            if (deleteVideos) {
                videoRepository.deleteVideosInFolder(folder.uriOrPath)
            }

            _folderPendingRemoval.value = null
            _userMessage.value = "Folder removed from indexed locations."
        }
    }

    fun addExcludedFolder(folderPath: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.addExcludedFolder(folderPath)
            storageAccessRepository.addExcludedFolder(folderPath)
            _userMessage.value = "Folder excluded from future media scans."
        }
    }

    fun removeExcludedFolder(folderPath: String) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.removeExcludedFolder(folderPath)
            storageAccessRepository.removeExcludedFolder(folderPath)
            _userMessage.value = "Folder exclusion removed."
        }
    }

    fun triggerFullScan() {
        mediaScanOrchestrator.triggerManualScan()
    }

    fun triggerIncrementalScan() {
        mediaScanOrchestrator.triggerIncrementalScan()
    }

    fun triggerLocationScan(folderUriOrPath: String) {
        mediaScanOrchestrator.triggerLocationScan(folderUriOrPath)
    }

    fun cancelScan() {
        mediaScanOrchestrator.cancelScan()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun extractSafDisplayName(uri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val segment = docId.substringAfterLast(':').substringAfterLast('/')
            if (segment.isNotBlank()) segment else uri.lastPathSegment ?: "Folder"
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast(':') ?: "Folder"
        }
    }
}
