package com.nexus.player.feature.library.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.nexus.player.core.designsystem.theme.NexusTheme
import com.nexus.player.core.ui.component.HorizontalSpacer
import com.nexus.player.core.ui.component.VerticalSpacer
import com.nexus.player.feature.library.preferences.FolderSortDirection
import com.nexus.player.feature.library.preferences.FolderSortField
import com.nexus.player.feature.library.preferences.FolderSortOption

/**
 * Material 3 Bottom Sheet for sorting folders by field and direction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSortBottomSheet(
    currentSortOption: FolderSortOption,
    onSortOptionSelected: (FolderSortOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = NexusTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Sort Folders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            VerticalSpacer(spacing.small)

            // Direction Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                FilterChip(
                    selected = currentSortOption.direction == FolderSortDirection.ASCENDING,
                    onClick = {
                        onSortOptionSelected(currentSortOption.copy(direction = FolderSortDirection.ASCENDING))
                    },
                    label = { Text("Ascending") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = currentSortOption.direction == FolderSortDirection.DESCENDING,
                    onClick = {
                        onSortOptionSelected(currentSortOption.copy(direction = FolderSortDirection.DESCENDING))
                    },
                    label = { Text("Descending") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            VerticalSpacer(spacing.medium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            VerticalSpacer(spacing.small)

            // Sort Fields
            FolderSortField.entries.forEach { field ->
                val isSelected = currentSortOption.field == field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.RadioButton,
                            onClick = {
                                onSortOptionSelected(currentSortOption.copy(field = field))
                            }
                        )
                        .padding(vertical = spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = field.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (isSelected) {
                        HorizontalSpacer(spacing.small)
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            VerticalSpacer(spacing.medium)
        }
    }
}
