package com.nexus.player.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.NexusBadge
import com.nexus.player.core.ui.component.NexusCard
import com.nexus.player.core.ui.component.NexusScaffold
import com.nexus.player.core.ui.component.NexusSectionHeader
import com.nexus.player.core.ui.component.NexusTopAppBar
import com.nexus.player.core.ui.component.VerticalSpacer

/**
 * Minimal placeholder screen used strictly to verify navigation routing,
 * selected bottom-tab state, and back navigation without premature feature logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusPlaceholderScreen(
    title: String,
    subtitle: String,
    badgeText: String = "Placeholder",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = NexusTheme.spacing

    NexusScaffold(
        modifier = modifier,
        topBar = {
            NexusTopAppBar(
                title = title,
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NexusSectionHeader(
                    title = title,
                    subtitle = subtitle
                )
                VerticalSpacer(spacing.small)
                NexusCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.large),
                        verticalArrangement = Arrangement.spacedBy(spacing.smallMedium)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VerticalSpacer(spacing.extraSmall)
                        NexusBadge(text = badgeText)
                    }
                }
            }
        }
    }
}
