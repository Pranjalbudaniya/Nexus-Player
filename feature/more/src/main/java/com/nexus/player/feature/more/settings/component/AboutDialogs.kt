package com.nexus.player.feature.more.settings.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.more.R

private data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val description: String
)

private val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "AndroidX Media3 / ExoPlayer",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        description = "High-performance, extensible media playback engine and codecs."
    ),
    OpenSourceLibrary(
        name = "AndroidX Jetpack Compose",
        author = "Google LLC",
        license = "Apache License 2.0",
        description = "Modern declarative toolkit for building native Android UI."
    ),
    OpenSourceLibrary(
        name = "KotlinX Coroutines & Flow",
        author = "JetBrains s.r.o.",
        license = "Apache License 2.0",
        description = "Asynchronous programming and reactive data stream primitives."
    ),
    OpenSourceLibrary(
        name = "AndroidX Room & SQLite",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        description = "Robust local SQLite object-mapping database layer."
    ),
    OpenSourceLibrary(
        name = "Google Dagger & Hilt",
        author = "Google LLC",
        license = "Apache License 2.0",
        description = "Dependency injection framework for Android applications."
    ),
    OpenSourceLibrary(
        name = "AndroidX DataStore",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        description = "Thread-safe, asynchronous key-value storage solution."
    ),
    OpenSourceLibrary(
        name = "Coil",
        author = "Coil Contributors",
        license = "Apache License 2.0",
        description = "Fast, lightweight image and thumbnail loading for Android."
    ),
    OpenSourceLibrary(
        name = "Material Components 3",
        author = "Google LLC",
        license = "Apache License 2.0",
        description = "Material You design system and adaptive component primitives."
    )
)

@Composable
fun ProjectInfoDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.smallMedium)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nexus_logo),
                    contentDescription = "Nexus Player Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(NexusTheme.customShapes.card)
                )
                Text(
                    text = "Nexus Player",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
            ) {
                Text(
                    text = "Nexus Player is a modern, privacy-first offline video player crafted with Jetpack Compose and AndroidX Media3.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Key Capabilities:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Hardware-accelerated playback with ExoPlayer\n" +
                            "• Live subtitle customization & sync delay controls\n" +
                            "• 5-band hardware equalizer & 200% volume boost\n" +
                            "• Gesture controls (double-tap seek, brightness, volume)\n" +
                            "• Storage Access Framework & scoped folder indexing\n" +
                            "• 100% offline, account-free, and telemetry-free architecture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = NexusTheme.customShapes.dialog,
        modifier = modifier
    )
}

@Composable
fun LicensesDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Open Source Licenses",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.smallMedium)
            ) {
                Text(
                    text = "Nexus Player is built with the following open-source software libraries:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OPEN_SOURCE_LIBRARIES.forEach { lib ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = lib.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        VerticalSpacer(NexusTheme.spacing.extraSmall)
                        Text(
                            text = "${lib.author} • ${lib.license}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        VerticalSpacer(NexusTheme.spacing.extraSmall)
                        Text(
                            text = lib.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = NexusTheme.spacing.extraSmall)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = NexusTheme.customShapes.dialog,
        modifier = modifier
    )
}

@Composable
fun AcknowledgementsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Acknowledgements",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NexusTheme.spacing.small)
            ) {
                Text(
                    text = "We express sincere gratitude to the open-source community whose work makes Nexus Player possible:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• The Android Open Source Project (AOSP) for Android OS and developer tools.\n" +
                            "• The AndroidX Media3 / ExoPlayer team for pioneering robust mobile video engines.\n" +
                            "• The Jetpack Compose and Kotlin teams for the modern declarative UI toolkit.\n" +
                            "• The open-source contributors and developers advocating for offline-first, privacy-respecting software.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = NexusTheme.customShapes.dialog,
        modifier = modifier
    )
}
