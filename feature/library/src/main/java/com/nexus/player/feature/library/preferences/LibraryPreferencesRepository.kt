package com.nexus.player.feature.library.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for managing persistent Library preferences (layout mode and sorting).
 */
interface LibraryPreferencesRepository {
    val layoutMode: Flow<LibraryLayoutMode>
    val sortOption: Flow<LibrarySortOption>
    val selectedTab: Flow<LibraryTab>
    val folderSortOption: Flow<FolderSortOption>

    suspend fun setLayoutMode(mode: LibraryLayoutMode)
    suspend fun setSortOption(sortOption: LibrarySortOption)
    suspend fun setSelectedTab(tab: LibraryTab)
    suspend fun setFolderSortOption(sortOption: FolderSortOption)
}

@Singleton
class LibraryPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LibraryPreferencesRepository {

    private object PreferencesKeys {
        val LAYOUT_MODE = stringPreferencesKey("key_library_layout_mode")
        val SORT_FIELD = stringPreferencesKey("key_library_sort_field")
        val SORT_DIRECTION = stringPreferencesKey("key_library_sort_direction")
        val SELECTED_TAB = stringPreferencesKey("key_library_selected_tab")
        val FOLDER_SORT_FIELD = stringPreferencesKey("key_folder_sort_field")
        val FOLDER_SORT_DIRECTION = stringPreferencesKey("key_folder_sort_direction")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val layoutMode: Flow<LibraryLayoutMode> = safePreferences
        .map { prefs ->
            val modeString = prefs[PreferencesKeys.LAYOUT_MODE]
            try {
                if (modeString != null) LibraryLayoutMode.valueOf(modeString) else LibraryLayoutMode.GRID
            } catch (_: IllegalArgumentException) {
                LibraryLayoutMode.GRID
            }
        }

    override val sortOption: Flow<LibrarySortOption> = safePreferences
        .map { prefs ->
            val fieldString = prefs[PreferencesKeys.SORT_FIELD]
            val dirString = prefs[PreferencesKeys.SORT_DIRECTION]

            val field = try {
                if (fieldString != null) LibrarySortField.valueOf(fieldString) else LibrarySortField.RECENTLY_ADDED
            } catch (_: IllegalArgumentException) {
                LibrarySortField.RECENTLY_ADDED
            }

            val direction = try {
                if (dirString != null) LibrarySortDirection.valueOf(dirString) else LibrarySortDirection.DESCENDING
            } catch (_: IllegalArgumentException) {
                LibrarySortDirection.DESCENDING
            }

            LibrarySortOption(field = field, direction = direction)
        }

    override val selectedTab: Flow<LibraryTab> = safePreferences
        .map { prefs ->
            val tabString = prefs[PreferencesKeys.SELECTED_TAB]
            try {
                if (tabString != null) LibraryTab.valueOf(tabString) else LibraryTab.VIDEOS
            } catch (_: IllegalArgumentException) {
                LibraryTab.VIDEOS
            }
        }

    override val folderSortOption: Flow<FolderSortOption> = safePreferences
        .map { prefs ->
            val fieldString = prefs[PreferencesKeys.FOLDER_SORT_FIELD]
            val dirString = prefs[PreferencesKeys.FOLDER_SORT_DIRECTION]

            val field = try {
                if (fieldString != null) FolderSortField.valueOf(fieldString) else FolderSortField.NAME
            } catch (_: IllegalArgumentException) {
                FolderSortField.NAME
            }

            val direction = try {
                if (dirString != null) FolderSortDirection.valueOf(dirString) else FolderSortDirection.ASCENDING
            } catch (_: IllegalArgumentException) {
                FolderSortDirection.ASCENDING
            }

            FolderSortOption(field = field, direction = direction)
        }

    override suspend fun setLayoutMode(mode: LibraryLayoutMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAYOUT_MODE] = mode.name
        }
    }

    override suspend fun setSortOption(sortOption: LibrarySortOption) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SORT_FIELD] = sortOption.field.name
            prefs[PreferencesKeys.SORT_DIRECTION] = sortOption.direction.name
        }
    }

    override suspend fun setSelectedTab(tab: LibraryTab) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_TAB] = tab.name
        }
    }

    override suspend fun setFolderSortOption(sortOption: FolderSortOption) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOLDER_SORT_FIELD] = sortOption.field.name
            prefs[PreferencesKeys.FOLDER_SORT_DIRECTION] = sortOption.direction.name
        }
    }
}
