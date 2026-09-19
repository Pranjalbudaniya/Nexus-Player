package com.nexus.player.core.ui.component.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.media.model.MediaMetadata

/**
 * Reusable host composable that orchestrates the video context menu sheet and all its dialogs
 * (File Info, Rename, Delete confirmation, Move/Copy folder selection).
 */
@Composable
fun VideoActionHost(
    video: MediaMetadata?,
    isSheetVisible: Boolean,
    folders: List<VideoFolder>,
    onDismissSheet: () -> Unit,
    onPlay: (String) -> Unit,
    onAddToPlaylist: (MediaMetadata) -> Unit,
    onToggleFavorite: (MediaMetadata) -> Unit,
    onShare: (MediaMetadata) -> Unit,
    onOpenContainingFolder: (folderPath: String, folderName: String) -> Unit,
    onRenameConfirm: (video: MediaMetadata, newName: String) -> Unit,
    onMoveConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onCopyConfirm: (video: MediaMetadata, targetFolderPath: String) -> Unit,
    onDeleteConfirm: (video: MediaMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeVideo by remember { mutableStateOf<MediaMetadata?>(null) }
    if (video != null) {
        activeVideo = video
    }

    val currentVideo = activeVideo ?: return

    var isFileInfoOpen by remember { mutableStateOf(false) }
    var isRenameOpen by remember { mutableStateOf(false) }
    var isDeleteOpen by remember { mutableStateOf(false) }
    var folderOperationMode by remember { mutableStateOf<FolderOperationMode?>(null) }

    // If all dialogs and sheet are closed, reset active video
    if (!isSheetVisible && !isFileInfoOpen && !isRenameOpen && !isDeleteOpen && folderOperationMode == null && video == null) {
        activeVideo = null
    }

    // Context Menu Bottom Sheet
    if (isSheetVisible) {
        VideoContextMenuSheet(
            video = currentVideo,
            onPlay = onPlay,
            onAddToPlaylist = { onAddToPlaylist(currentVideo) },
            onToggleFavorite = { onToggleFavorite(currentVideo) },
            onShare = { onShare(currentVideo) },
            onShowFileInfo = { isFileInfoOpen = true },
            onOpenContainingFolder = {
                onOpenContainingFolder(currentVideo.folderPath, currentVideo.folderName)
            },
            onRename = { isRenameOpen = true },
            onMove = { folderOperationMode = FolderOperationMode.MOVE },
            onCopy = { folderOperationMode = FolderOperationMode.COPY },
            onDelete = { isDeleteOpen = true },
            onDismissRequest = onDismissSheet,
            modifier = modifier
        )
    }

    // File Info Dialog
    if (isFileInfoOpen) {
        VideoFileInfoDialog(
            video = currentVideo,
            onDismissRequest = {
                isFileInfoOpen = false
                if (!isSheetVisible && video == null) activeVideo = null
            }
        )
    }

    // Rename Dialog
    if (isRenameOpen) {
        RenameVideoDialog(
            video = currentVideo,
            onDismissRequest = {
                isRenameOpen = false
                if (!isSheetVisible && video == null) activeVideo = null
            },
            onConfirmRename = { newName ->
                onRenameConfirm(currentVideo, newName)
                isRenameOpen = false
                if (!isSheetVisible && video == null) activeVideo = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (isDeleteOpen) {
        DeleteConfirmationDialog(
            video = currentVideo,
            onDismissRequest = {
                isDeleteOpen = false
                if (!isSheetVisible && video == null) activeVideo = null
            },
            onConfirmDelete = {
                onDeleteConfirm(currentVideo)
                isDeleteOpen = false
                if (!isSheetVisible && video == null) activeVideo = null
            }
        )
    }

    // Move / Copy Folder Picker Dialog
    val activeMode = folderOperationMode
    if (activeMode != null) {
        MoveCopyFolderDialog(
            mode = activeMode,
            video = currentVideo,
            folders = folders,
            onFolderSelected = { targetPath ->
                if (activeMode == FolderOperationMode.MOVE) {
                    onMoveConfirm(currentVideo, targetPath)
                } else {
                    onCopyConfirm(currentVideo, targetPath)
                }
                folderOperationMode = null
                if (!isSheetVisible && video == null) activeVideo = null
            },
            onDismissRequest = {
                folderOperationMode = null
                if (!isSheetVisible && video == null) activeVideo = null
            }
        )
    }
}
