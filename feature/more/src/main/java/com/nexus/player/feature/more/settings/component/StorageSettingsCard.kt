package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.player.core.common.settings.model.LibrarySettings
import com.nexus.player.core.common.settings.model.StorageSettings
import com.nexus.player.core.common.storage.StorageAccessMode
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.scanner.model.ScanState
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer

@Composable
fun StorageSettingsCard(
    storageSettings: StorageSettings,
    librarySettings: LibrarySettings,
    accessMode: StorageAccessMode,
    indexedFolderCount: Int,
    indexedVideoCount: Int,
    scanState: ScanState,
    isScanning: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToStorageLocations: () -> Unit,
    onSetScanOnAppLaunch: (Boolean) -> Unit,
    onSetIncludeHiddenFiles: (Boolean) -> Unit,
    onTriggerIncrementalScan: () -> Unit,
    onTriggerFullScan: () -> Unit,
    onCancelScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSectionCard(
        title = "Storage & Scanning",
        subtitle = "Scan locations, indexing behavior, cache, and exclusions",
        icon = Icons.Default.Storage,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier
    ) {
        // Active Scan Progress Banner
        if (isScanning || scanState is ScanState.Scanning) {
            val progress = (scanState as? ScanState.Scanning)?.progress
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NexusTheme.spacing.extraSmall),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(NexusTheme.spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            HorizontalSpacer(NexusTheme.spacing.extraSmall)
                            Text(
                                text = "Scanning in progress...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    progress?.currentFile?.let { file ->
                        VerticalSpacer(NexusTheme.spacing.extraSmall)
                        Text(
                            text = "Processing: $file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Manage Storage Locations Navigation Row
        SettingActionRow(
            title = "Manage Storage Locations",
            description = when (accessMode) {
                StorageAccessMode.ALL_MEDIA -> "All Device Media • $indexedFolderCount folders ($indexedVideoCount videos)"
                StorageAccessMode.SELECTED_FOLDERS -> "Selected Folders • $indexedFolderCount locations ($indexedVideoCount videos)"
            },
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
            onClick = onNavigateToStorageLocations
        )

        // Excluded Folders Navigation Row
        SettingActionRow(
            title = "Excluded Folders",
            description = if (librarySettings.excludedFolders.isEmpty()) {
                "No folders excluded • Tap to manage"
            } else {
                "${librarySettings.excludedFolders.size} excluded folders • Tap to manage"
            },
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForwardIos,
            onClick = onNavigateToStorageLocations
        )

        // Quick Scan Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NexusTheme.spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
        ) {
            FilledTonalButton(
                onClick = onTriggerIncrementalScan,
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                HorizontalSpacer(NexusTheme.spacing.extraSmall)
                Text("Refresh Scan", maxLines = 1)
            }

            OutlinedButton(
                onClick = onTriggerFullScan,
                enabled = !isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                HorizontalSpacer(NexusTheme.spacing.extraSmall)
                Text("Full Rescan", maxLines = 1)
            }
        }

        // Scan on App Launch Switch
        SettingSwitchRow(
            title = "Scan on App Launch",
            checked = librarySettings.scanOnAppLaunch,
            onCheckedChange = onSetScanOnAppLaunch,
            description = "Automatically detect new and modified media each time Nexus Player opens"
        )

        // Include Hidden Files Switch
        SettingSwitchRow(
            title = "Include Hidden Files",
            checked = librarySettings.includeHiddenFiles,
            onCheckedChange = onSetIncludeHiddenFiles,
            description = "Index video files and directories that begin with a dot ('.')"
        )

        // Cache Capacity
        SettingInfoRow(
            title = "Thumbnail Cache Capacity",
            value = "${storageSettings.cacheThumbnailMaxEntries} entries",
            description = "In-memory LRU cache capacity for video thumbnails"
        )

        // Preserve Staged Deletions
        SettingInfoRow(
            title = "Preserve Staged Deletions",
            value = if (storageSettings.preserveStagedDeletions) "Enabled" else "Disabled",
            description = "Safeguard media files against accidental deletion"
        )
    }
}
