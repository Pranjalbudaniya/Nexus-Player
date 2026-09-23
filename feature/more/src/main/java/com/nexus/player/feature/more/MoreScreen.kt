package com.nexus.player.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.media.model.MediaMetadata
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.contextmenu.VideoFileInfoDialog
import com.nexus.player.feature.more.component.OpenNetworkUrlDialog
import com.nexus.player.feature.more.settings.component.LicensesDialog
import com.nexus.player.feature.more.settings.component.ProjectInfoDialog

/**
 * Top-level route for the More hub screen.
 *
 * Coordinates navigation to less-frequently-used areas (History, Analytics,
 * Favorites, Playlists, Storage/Scanning) and technical/licensing dialogs.
 */
@Composable
fun MoreRoute(
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToStorageLocations: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val recentVideo by viewModel.recentVideoForInfo.collectAsStateWithLifecycle()

    MoreScreen(
        recentVideo = recentVideo,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToAnalytics = onNavigateToAnalytics,
        onNavigateToFavorites = onNavigateToFavorites,
        onNavigateToPlaylists = onNavigateToPlaylists,
        onNavigateToStorageLocations = onNavigateToStorageLocations,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPlayer = onNavigateToPlayer,
        modifier = modifier
    )
}

/**
 * More Screen presenting a simple, flat Material 3 list of secondary destinations,
 * technical tools, and about/licensing actions.
 *
 * Strictly avoids nested sections, category headers, and giant cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    recentVideo: MediaMetadata?,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToStorageLocations: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    var showFileInfoDialog by remember { mutableStateOf(false) }
    var showNoMediaDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNetworkUrlDialog by remember { mutableStateOf(false) }

    NexusScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NexusTopAppBar(
                title = "More",
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Settings" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val bottomNavPadding = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.medium,
                    end = spacing.medium,
                    top = spacing.smallMedium,
                    bottom = bottomNavPadding
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            // 1. History
            MoreListItem(
                icon = Icons.Default.History,
                title = "History",
                subtitle = "Recently played videos, playback progress, and completed videos",
                onClick = onNavigateToHistory
            )

            // 2. Playback Analytics
            MoreListItem(
                icon = Icons.Default.Analytics,
                title = "Playback Analytics",
                subtitle = "Local watch time, completion rates, and format trends",
                onClick = onNavigateToAnalytics
            )

            // 3. Favorites
            MoreListItem(
                icon = Icons.Default.Favorite,
                title = "Favorites",
                subtitle = "Quick access to starred and favorite videos",
                onClick = onNavigateToFavorites
            )

            // 4. Playlists
            MoreListItem(
                icon = Icons.Default.Subscriptions,
                title = "Playlists",
                subtitle = "Manage custom playlists and saved queues",
                onClick = onNavigateToPlaylists
            )

            // 5. Network Stream
            MoreListItem(
                icon = Icons.Default.Link,
                title = "Network Stream",
                subtitle = "Play video directly from an HTTP or HTTPS stream",
                onClick = { showNetworkUrlDialog = true }
            )

            // 5. Technical Information / File & Codec Info
            MoreListItem(
                icon = Icons.Default.Code,
                title = "File & Codec Information",
                subtitle = "Inspect video codecs, audio streams, and container details",
                onClick = {
                    if (recentVideo != null) {
                        showFileInfoDialog = true
                    } else {
                        showNoMediaDialog = true
                    }
                }
            )

            // 6. Storage & Scanning
            MoreListItem(
                icon = Icons.Default.Folder,
                title = "Storage & Scanning",
                subtitle = "Manage storage access, directory folders, and media scanning",
                onClick = onNavigateToStorageLocations
            )

            // 7. Open Source Licenses
            MoreListItem(
                icon = Icons.Default.Gavel,
                title = "Open Source Licenses",
                subtitle = "Third-party libraries and license notices",
                onClick = { showLicensesDialog = true }
            )

            // 8. About Nexus Player
            MoreListItem(
                icon = Icons.Default.Info,
                title = "About Nexus Player",
                subtitle = "Version 1.0.0 • Architecture & acknowledgements",
                onClick = { showAboutDialog = true }
            )
        }

        // Technical Media Information Dialog
        if (showFileInfoDialog && recentVideo != null) {
            VideoFileInfoDialog(
                video = recentVideo,
                onDismissRequest = { showFileInfoDialog = false }
            )
        }

        // Informative Dialog when no media is available for technical inspection
        if (showNoMediaDialog) {
            AlertDialog(
                onDismissRequest = { showNoMediaDialog = false },
                title = {
                    Text(
                        text = "File & Codec Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "No media files are currently loaded or indexed. Play a video or scan your storage folders to inspect detailed codec, container, and audio stream specifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showNoMediaDialog = false }) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = NexusTheme.customShapes.dialog
            )
        }

        // Third-party Open Source Licenses Dialog
        if (showLicensesDialog) {
            LicensesDialog(
                onDismiss = { showLicensesDialog = false }
            )
        }

        // About & Project Architecture Dialog
        if (showAboutDialog) {
            ProjectInfoDialog(
                onDismiss = { showAboutDialog = false }
            )
        }

        // Open Network URL Dialog
        if (showNetworkUrlDialog) {
            OpenNetworkUrlDialog(
                onPlayUrl = { url ->
                    showNetworkUrlDialog = false
                    onNavigateToPlayer(url)
                },
                onDismissRequest = { showNetworkUrlDialog = false }
            )
        }
    }
}

/**
 * Flat, compact Material 3 list row designed with 4dp spacing,
 * semantic accessibility, and theme compliance.
 */
@Composable
fun MoreListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing
    val shapes = NexusTheme.customShapes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            ),
        shape = shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = spacing.smallMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shapes.thumbnail)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalSpacer(spacing.medium)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalSpacer(spacing.small)

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
