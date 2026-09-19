package com.nexus.player.core.common.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Version-Aware Storage Permission Helper.
 *
 * Technical Decision Notes:
 * - API 33+ (Android 13 to Android 16/API 36): Uses granular `READ_MEDIA_VIDEO`. Deprecated
 *   `READ_EXTERNAL_STORAGE` is strictly avoided.
 * - API 28 to 32 (Android 9 to Android 12L): Uses `READ_EXTERNAL_STORAGE` as required for legacy access.
 * - No broad `MANAGE_EXTERNAL_STORAGE` is requested to adhere to principle of least privilege.
 */
object StoragePermissionHelper {

    /**
     * Returns the appropriate runtime permission required for indexing video files on the current OS version.
     */
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Checks if the required video media permission is currently granted.
     */
    fun isPermissionGranted(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
