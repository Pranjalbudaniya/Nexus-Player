package com.nexus.player.core.scanner.datasource

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.nexus.player.core.scanner.util.VideoFormatDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SafFolderScanner"

/**
 * Traverses user-selected directories granted via the Storage Access Framework (SAF).
 * Safely handles revoked folder permissions, cyclic structures, and nested subfolders.
 */
@Singleton
open class SafFolderScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Crawls all granted tree folders in [folderUris] and invokes [onItemDiscovered] for each video.
     */
    open suspend fun scanFolders(
        folderUris: Set<String>,
        onItemDiscovered: suspend (DiscoveredMediaItem) -> Unit
    ): Int {
        var totalDiscovered = 0
        val visitedUris = mutableSetOf<String>()

        for (uriString in folderUris) {
            currentCoroutineContext().ensureActive()
            try {
                val treeUri = Uri.parse(uriString)
                val documentId = DocumentsContract.getTreeDocumentId(treeUri)
                val folderName = treeUri.lastPathSegment?.substringAfterLast(':') ?: "Folder"

                totalDiscovered += scanDirectory(
                    treeUri = treeUri,
                    parentDocumentId = documentId,
                    folderName = folderName,
                    folderPath = uriString,
                    depth = 0,
                    visited = visitedUris,
                    onItemDiscovered = onItemDiscovered
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Access revoked or restricted for folder URI $uriString: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Error traversing SAF folder $uriString: ${e.message}")
            }
        }

        return totalDiscovered
    }

    private suspend fun scanDirectory(
        treeUri: Uri,
        parentDocumentId: String,
        folderName: String,
        folderPath: String,
        depth: Int,
        visited: MutableSet<String>,
        onItemDiscovered: suspend (DiscoveredMediaItem) -> Unit
    ): Int {
        if (depth > 20) return 0 // Guard against deep or cyclic folder nesting

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val childrenKey = childrenUri.toString()
        if (!visited.add(childrenKey)) {
            return 0 // Cycle prevention
        }

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        var discoveredCount = 0
        val contentResolver = context.contentResolver

        try {
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()

                    val docId = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Document_$docId"
                    val mimeType = cursor.getString(mimeColumn)

                    if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                        // Recurse into child directory
                        discoveredCount += scanDirectory(
                            treeUri = treeUri,
                            parentDocumentId = docId,
                            folderName = name,
                            folderPath = "$folderPath/$name",
                            depth = depth + 1,
                            visited = visited,
                            onItemDiscovered = onItemDiscovered
                        )
                    } else if (VideoFormatDetector.isVideoFile(name, mimeType)) {
                        val size = cursor.getLong(sizeColumn)
                        if (size <= 0) continue

                        val lastModified = cursor.getLong(modifiedColumn)
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        val title = VideoFormatDetector.extractDisplayTitle(name)

                        val item = DiscoveredMediaItem(
                            id = docUri,
                            mediaUri = docUri,
                            filePath = null,
                            fileName = name,
                            title = title,
                            folderName = folderName,
                            folderPath = folderPath,
                            sizeBytes = size,
                            durationMs = 0L,
                            width = 0,
                            height = 0,
                            dateAdded = if (lastModified > 0) lastModified else System.currentTimeMillis(),
                            lastModified = if (lastModified > 0) lastModified else System.currentTimeMillis(),
                            mimeType = mimeType
                        )

                        onItemDiscovered(item)
                        discoveredCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning directory $childrenUri: ${e.message}")
        }

        return discoveredCount
    }
}
