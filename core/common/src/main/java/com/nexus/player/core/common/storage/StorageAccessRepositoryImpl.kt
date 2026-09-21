package com.nexus.player.core.common.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageAccessRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : StorageAccessRepository {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("key_onboarding_completed")
        val STORAGE_ACCESS_MODE = stringPreferencesKey("key_storage_access_mode")
        val SELECTED_FOLDER_URIS = stringSetPreferencesKey("key_selected_folder_uris")
        val EXCLUDED_FOLDER_PATHS = stringSetPreferencesKey("key_library_excluded_folders")
    }

    override val storageAccessState: Flow<StorageAccessState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
            val modeString = preferences[PreferencesKeys.STORAGE_ACCESS_MODE]
            val accessMode = try {
                if (modeString != null) StorageAccessMode.valueOf(modeString) else StorageAccessMode.ALL_MEDIA
            } catch (_: IllegalArgumentException) {
                StorageAccessMode.ALL_MEDIA
            }
            val folders = preferences[PreferencesKeys.SELECTED_FOLDER_URIS] ?: emptySet()
            val excluded = preferences[PreferencesKeys.EXCLUDED_FOLDER_PATHS] ?: emptySet()
            val permissionGranted = isPermissionGranted()

            StorageAccessState(
                isOnboardingCompleted = isCompleted,
                isPermissionGranted = permissionGranted,
                accessMode = accessMode,
                selectedFolderUris = folders,
                excludedFolderPaths = excluded
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setStorageAccessMode(mode: StorageAccessMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.STORAGE_ACCESS_MODE] = mode.name
        }
    }

    override suspend fun addSelectedFolderUri(uriString: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SELECTED_FOLDER_URIS] ?: emptySet()
            preferences[PreferencesKeys.SELECTED_FOLDER_URIS] = current + uriString
            preferences[PreferencesKeys.STORAGE_ACCESS_MODE] = StorageAccessMode.SELECTED_FOLDERS.name
        }
    }

    override suspend fun removeSelectedFolderUri(uriString: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SELECTED_FOLDER_URIS] ?: emptySet()
            val updated = current - uriString
            preferences[PreferencesKeys.SELECTED_FOLDER_URIS] = updated
            if (updated.isEmpty()) {
                preferences[PreferencesKeys.STORAGE_ACCESS_MODE] = StorageAccessMode.ALL_MEDIA.name
            }
        }
    }

    override suspend fun clearSelectedFolders() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SELECTED_FOLDER_URIS)
            preferences[PreferencesKeys.STORAGE_ACCESS_MODE] = StorageAccessMode.ALL_MEDIA.name
        }
    }

    override suspend fun addExcludedFolder(folderPath: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXCLUDED_FOLDER_PATHS] ?: emptySet()
            preferences[PreferencesKeys.EXCLUDED_FOLDER_PATHS] = current + folderPath
        }
    }

    override suspend fun removeExcludedFolder(folderPath: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXCLUDED_FOLDER_PATHS] ?: emptySet()
            preferences[PreferencesKeys.EXCLUDED_FOLDER_PATHS] = current - folderPath
        }
    }

    override suspend fun clearExcludedFolders() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.EXCLUDED_FOLDER_PATHS)
        }
    }

    override fun isPermissionGranted(): Boolean {
        return StoragePermissionHelper.isPermissionGranted(context)
    }

    override fun getRequiredPermissions(): List<String> {
        return StoragePermissionHelper.getRequiredPermissions()
    }
}
