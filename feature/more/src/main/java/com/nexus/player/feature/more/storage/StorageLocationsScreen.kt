package com.nexus.player.feature.more.storage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.ui.component.HorizontalSpacer
import androidx.compose.material3.AlertDialog
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer

@Composable
fun StorageLocationsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageLocationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StorageLocationsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSetStorageAccessMode = viewModel::setStorageAccessMode,
        onAddFolderUri = viewModel::addFolderUri,
        onRequestRemoveFolder = viewModel::requestRemoveFolder,
        onDismissRemoveFolder = viewModel::dismissRemoveFolder,
        onConfirmRemoveFolder = viewModel::confirmRemoveFolder,
        onAddExcludedFolder = viewModel::addExcludedFolder,
        onRemoveExcludedFolder = viewModel::removeExcludedFolder,
        onTriggerFullScan = viewModel::triggerFullScan,
        onTriggerIncrementalScan = viewModel::triggerIncrementalScan,
        onTriggerLocationScan = viewModel::triggerLocationScan,
        onCancelScan = viewModel::cancelScan,
        onClearUserMessage = viewModel::clearUserMessage,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageLocationsScreen(
    uiState: StorageLocationsUiState,
    onNavigateBack: () -> Unit,
    onSetStorageAccessMode: (StorageAccessMode) -> Unit,
    onAddFolderUri: (Uri) -> Unit,
    onRequestRemoveFolder: (StorageFolderItem) -> Unit,
    onDismissRemoveFolder: () -> Unit,
    onConfirmRemoveFolder: (deleteVideos: Boolean) -> Unit,
    onAddExcludedFolder: (String) -> Unit,
    onRemoveExcludedFolder: (String) -> Unit,
    onTriggerFullScan: () -> Unit,
    onTriggerIncrementalScan: () -> Unit,
    onTriggerLocationScan: (String) -> Unit,
    onCancelScan: () -> Unit,
    onClearUserMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteVideosOnRemoval by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                onAddFolderUri(uri)
            }
        }
    )

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearUserMessage()
        }
    }

    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            NexusTopAppBar(
                title = "Storage Locations",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onTriggerIncrementalScan,
                        enabled = !uiState.isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Refresh scan"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(NexusTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.medium)
        ) {
            // 1. Active Scan Progress Banner
            if (uiState.isScanning || uiState.scanState is ScanState.Scanning) {
                item(key = "active_scan_card") {
                    val progress = (uiState.scanState as? ScanState.Scanning)?.progress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(NexusTheme.spacing.medium)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    HorizontalSpacer(NexusTheme.spacing.extraSmall)
                                    Text(
                                        text = "Scanning Media Library...",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = onCancelScan,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }

                            VerticalSpacer(NexusTheme.spacing.extraSmall)
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )

                            VerticalSpacer(NexusTheme.spacing.extraSmall)
                            Text(
                                text = if (progress?.currentFile != null) {
                                    "Processed ${progress.current} items (${progress.currentFile})"
                                } else {
                                    "Discovered ${progress?.current ?: 0} items..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Storage Mode Selection Card
            item(key = "storage_mode_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(NexusTheme.spacing.medium)) {
                        Text(
                            text = "Media Discovery Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose how Nexus Player discovers and indexes video files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        VerticalSpacer(NexusTheme.spacing.small)

                        // Mode: ALL_MEDIA
                        StorageModeOption(
                            title = "All Device Media (Recommended)",
                            description = "Indexes all accessible videos across device storage using system MediaStore.",
                            selected = uiState.accessMode == StorageAccessMode.ALL_MEDIA,
                            onClick = { onSetStorageAccessMode(StorageAccessMode.ALL_MEDIA) }
                        )

                        VerticalSpacer(NexusTheme.spacing.extraSmall)

                        // Mode: SELECTED_FOLDERS
                        StorageModeOption(
                            title = "Selected Folders Only",
                            description = "Strictly indexes media located in user-selected directories via Storage Access Framework.",
                            selected = uiState.accessMode == StorageAccessMode.SELECTED_FOLDERS,
                            onClick = { onSetStorageAccessMode(StorageAccessMode.SELECTED_FOLDERS) }
                        )
                    }
                }
            }

            // 3. Action Toolbar: Add Folder / Rescan All
            item(key = "scan_actions_card") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
                ) {
                    FilledTonalButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        HorizontalSpacer(NexusTheme.spacing.extraSmall)
                        Text("Add Folder", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onTriggerFullScan,
                        enabled = !uiState.isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        HorizontalSpacer(NexusTheme.spacing.extraSmall)
                        Text("Full Rescan", maxLines = 1)
                    }
                }
            }

            // 4. Indexed Folders Section Header
            item(key = "indexed_folders_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Indexed Folders (${uiState.indexedFolders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 5. Empty State or Folder Items
            if (uiState.indexedFolders.isEmpty()) {
                item(key = "empty_folders_card") {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NexusTheme.spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            VerticalSpacer(NexusTheme.spacing.small)
                            Text(
                                text = "No storage folders configured",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            VerticalSpacer(NexusTheme.spacing.extraSmall)
                            Text(
                                text = "Add a directory using the button above to begin indexing videos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.indexedFolders,
                    key = { it.uriOrPath }
                ) { folder ->
                    StorageFolderRow(
                        folder = folder,
                        isExcluded = uiState.excludedFolders.any {
                            folder.uriOrPath.equals(it, ignoreCase = true) ||
                                folder.uriOrPath.startsWith(it.trimEnd('/') + "/")
                        },
                        isScanning = uiState.isScanning,
                        onScanLocation = { onTriggerLocationScan(folder.uriOrPath) },
                        onExcludeFolder = { onAddExcludedFolder(folder.uriOrPath) },
                        onRemoveFolder = { onRequestRemoveFolder(folder) }
                    )
                }
            }

            // 6. Excluded Folders Section
            if (uiState.excludedFolders.isNotEmpty()) {
                item(key = "excluded_folders_header") {
                    VerticalSpacer(NexusTheme.spacing.extraSmall)
                    Text(
                        text = "Excluded Folders (${uiState.excludedFolders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Excluded folders are skipped during media scans. Existing media records are preserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    items = uiState.excludedFolders.toList(),
                    key = { "excluded_$it" }
                ) { excludedPath ->
                    ExcludedFolderRow(
                        folderPath = excludedPath,
                        onRemoveExclusion = { onRemoveExcludedFolder(excludedPath) }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Folder Removal
    uiState.folderPendingRemoval?.let { folder ->
        AlertDialog(
            onDismissRequest = onDismissRemoveFolder,
            title = {
                Text(
                    text = "Remove Folder",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Remove \"${folder.displayName}\" from indexed locations?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    VerticalSpacer(NexusTheme.spacing.small)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { deleteVideosOnRemoval = !deleteVideosOnRemoval }
                            .padding(NexusTheme.spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteVideosOnRemoval,
                            onCheckedChange = { deleteVideosOnRemoval = it }
                        )
                        HorizontalSpacer(NexusTheme.spacing.extraSmall)
                        Text(
                            text = "Also remove indexed videos from library database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmRemoveFolder(deleteVideosOnRemoval)
                        deleteVideosOnRemoval = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRemoveFolder) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(NexusTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        HorizontalSpacer(NexusTheme.spacing.extraSmall)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageFolderRow(
    folder: StorageFolderItem,
    isExcluded: Boolean,
    isScanning: Boolean,
    onScanLocation: () -> Unit,
    onExcludeFolder: () -> Unit,
    onRemoveFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NexusTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (folder.isAccessible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalSpacer(NexusTheme.spacing.small)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!folder.isAccessible) {
                        HorizontalSpacer(NexusTheme.spacing.extraSmall)
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Location inaccessible",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${folder.videoCount} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isExcluded) {
                        HorizontalSpacer(NexusTheme.spacing.extraSmall)
                        Text(
                            text = "• Excluded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Quick Actions
            IconButton(
                onClick = onScanLocation,
                enabled = !isScanning && folder.isAccessible
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Scan this folder",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isExcluded) {
                IconButton(
                    onClick = onExcludeFolder
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Exclude folder from scans",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (folder.isSafFolder) {
                IconButton(
                    onClick = onRemoveFolder
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove folder",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcludedFolderRow(
    folderPath: String,
    onRemoveExclusion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NexusTheme.spacing.small, vertical = NexusTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )

            HorizontalSpacer(NexusTheme.spacing.small)

            Text(
                text = folderPath.substringAfterLast('/'),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            TextButton(
                onClick = onRemoveExclusion,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Include")
            }
        }
    }
}
