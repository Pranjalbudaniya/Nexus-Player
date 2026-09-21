package com.nexus.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.nexus.player.core.database.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Projection tuple for folder-level video aggregations.
 */
data class FolderSummary(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val previewMediaUri: String? = null,
    val lastModified: Long = 0L
)

/**
 * Lightweight projection for high-performance media scanner diffing.
 */
data class VideoScanLookup(
    val id: String,
    val mediaUri: String,
    val lastModified: Long,
    val sizeBytes: Long,
    val fileName: String,
    val folderPath: String
)

/**
 * Data Access Object for local video media items.
 */
@Dao
interface VideoDao {

    // --- All Videos Queries with explicit sort orders ---

    @Query("SELECT * FROM videos ORDER BY title COLLATE NOCASE ASC")
    fun getAllVideosByTitleAsc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY title COLLATE NOCASE DESC")
    fun getAllVideosByTitleDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideosByDateAddedDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded ASC")
    fun getAllVideosByDateAddedAsc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY durationMs DESC")
    fun getAllVideosByDurationDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY durationMs ASC")
    fun getAllVideosByDurationAsc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY sizeBytes DESC")
    fun getAllVideosBySizeDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY sizeBytes ASC")
    fun getAllVideosBySizeAsc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY CASE WHEN lastPlayedAt IS NULL THEN 1 ELSE 0 END, lastPlayedAt DESC, dateAdded DESC")
    fun getAllVideosByLastPlayedDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY CASE WHEN lastPlayedAt IS NULL THEN 1 ELSE 0 END, lastPlayedAt ASC, dateAdded ASC")
    fun getAllVideosByLastPlayedAsc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY lastModified DESC")
    fun getAllVideosByDateModifiedDesc(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY lastModified ASC")
    fun getAllVideosByDateModifiedAsc(): Flow<List<VideoEntity>>

    // --- Filtered & Categorized Queries ---

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedVideos(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC, title COLLATE NOCASE ASC LIMIT :limit")
    fun getFavoriteVideosWithLimit(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE playbackPositionMs > 0 
          AND playbackPercentage < 0.95 
          AND isCompleted = 0 
          AND lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun getContinueWatchingVideos(limit: Int = 10): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getHistoryVideos(limit: Int = 20): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun getAllHistoryVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun getVideosByFolder(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun getVideosByFolderTitleAsc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE DESC")
    fun getVideosByFolderTitleDesc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY dateAdded DESC")
    fun getVideosByFolderDateAddedDesc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY dateAdded ASC")
    fun getVideosByFolderDateAddedAsc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY durationMs DESC")
    fun getVideosByFolderDurationDesc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY durationMs ASC")
    fun getVideosByFolderDurationAsc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY sizeBytes DESC")
    fun getVideosByFolderSizeDesc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY sizeBytes ASC")
    fun getVideosByFolderSizeAsc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY lastModified DESC")
    fun getVideosByFolderDateModifiedDesc(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY lastModified ASC")
    fun getVideosByFolderDateModifiedAsc(folderPath: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT 
            folderPath, 
            folderName, 
            COUNT(*) as videoCount,
            (SELECT mediaUri FROM videos v2 WHERE v2.folderPath = videos.folderPath ORDER BY dateAdded DESC LIMIT 1) as previewMediaUri,
            MAX(lastModified) as lastModified
        FROM videos 
        GROUP BY folderPath, folderName 
        ORDER BY folderName COLLATE NOCASE ASC
    """)
    fun getFolders(): Flow<List<FolderSummary>>

    @Query("""
        SELECT 
            folderPath, 
            folderName, 
            COUNT(*) as videoCount,
            (SELECT mediaUri FROM videos v2 WHERE v2.folderPath = videos.folderPath ORDER BY dateAdded DESC LIMIT 1) as previewMediaUri,
            MAX(lastModified) as lastModified
        FROM videos 
        WHERE folderPath = :folderPath
        GROUP BY folderPath, folderName 
        LIMIT 1
    """)
    fun getFolderSummaryByPath(folderPath: String): Flow<FolderSummary?>

    // --- Global Search Queries ---

    @Query("""
        SELECT * FROM videos 
        WHERE (:escapedQuery = '' OR 
               title LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR fileName LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR folderName LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR folderPath LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR resolutionLabel LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            OR (videoCodec IS NOT NULL AND videoCodec LIKE '%' || :escapedQuery || '%' ESCAPE '\')
            OR (audioCodec IS NOT NULL AND audioCodec LIKE '%' || :escapedQuery || '%' ESCAPE '\')
        )
        ORDER BY 
            CASE 
                WHEN title = :rawQuery THEN 1
                WHEN title LIKE :prefixQuery ESCAPE '\' THEN 2
                WHEN fileName LIKE :prefixQuery ESCAPE '\' THEN 3
                WHEN title LIKE '%' || :escapedQuery || '%' ESCAPE '\' THEN 4
                ELSE 5
            END,
            dateAdded DESC
    """)
    fun searchVideos(
        rawQuery: String,
        escapedQuery: String,
        prefixQuery: String
    ): Flow<List<VideoEntity>>

    // --- Single Item Lookups ---

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE mediaUri = :mediaUri")
    suspend fun getVideoByUri(mediaUri: String): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideosCount(): Int

    @Query("SELECT id, mediaUri, lastModified, sizeBytes, fileName, folderPath FROM videos")
    suspend fun getAllScanLookup(): List<VideoScanLookup>

    @Query("SELECT id, mediaUri, lastModified, sizeBytes, fileName, folderPath FROM videos WHERE folderPath = :folderPath")
    suspend fun getScanLookupForFolder(folderPath: String): List<VideoScanLookup>

    @Query("SELECT id FROM videos WHERE folderPath IN (:folderPaths)")
    suspend fun getVideoIdsInFolders(folderPaths: List<String>): List<String>

    // --- Insertions & Upserts ---

    /**
     * Strict insert that aborts on conflict.
     * Useful for duplicate detection and prevention tests.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVideo(video: VideoEntity): Long

    /**
     * Upserts a single video item, updating existing data if primary key matches.
     */
    @Upsert
    suspend fun upsertVideo(video: VideoEntity)

    /**
     * Bulk upsert for media scanner batches.
     */
    @Upsert
    suspend fun upsertVideos(videos: List<VideoEntity>)

    // --- In-place Metadata Updates ---

    @Query("""
        UPDATE videos 
        SET playbackPositionMs = :positionMs, 
            playbackPercentage = :percentage, 
            lastPlayedAt = :lastPlayedAt,
            isCompleted = CASE WHEN :isCompleted = 1 OR :percentage >= 0.949 THEN 1 ELSE :isCompleted END,
            watchCount = CASE WHEN :isCompleted = 1 OR :percentage >= 0.949 THEN watchCount + 1 ELSE watchCount END
        WHERE id = :id
    """)
    suspend fun updatePlaybackProgress(
        id: String,
        positionMs: Long,
        percentage: Float,
        lastPlayedAt: Long,
        isCompleted: Boolean = false
    )

    @Query("""
        UPDATE videos 
        SET isCompleted = 0, 
            playbackPositionMs = 0, 
            playbackPercentage = 0.0, 
            lastPlayedAt = :startTimeMs, 
            watchCount = watchCount + 1 
        WHERE id = :id
    """)
    suspend fun restartPlayback(id: String, startTimeMs: Long)

    @Query("""
        UPDATE videos 
        SET lastPlayedAt = NULL, 
            playbackPositionMs = 0, 
            playbackPercentage = 0.0, 
            isCompleted = 0 
        WHERE id = :id
    """)
    suspend fun clearHistoryForVideo(id: String)

    @Query("""
        UPDATE videos 
        SET lastPlayedAt = NULL, 
            playbackPositionMs = 0, 
            playbackPercentage = 0.0, 
            isCompleted = 0 
        WHERE lastPlayedAt IS NOT NULL
    """)
    suspend fun clearAllHistory()

    @Query("""
        UPDATE videos 
        SET lastPlayedAt = NULL, 
            playbackPositionMs = 0, 
            playbackPercentage = 0.0, 
            isCompleted = 0,
            watchCount = 0 
        WHERE lastPlayedAt IS NOT NULL OR watchCount > 0 OR playbackPositionMs > 0
    """)
    suspend fun clearAllAnalyticsAndHistory()

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    // --- Deletions & Cleanup ---

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<String>)

    @Query("DELETE FROM videos WHERE mediaUri = :mediaUri")
    suspend fun deleteByUri(mediaUri: String)

    @Query("DELETE FROM videos WHERE id NOT IN (:validIds)")
    suspend fun deleteStaleVideos(validIds: List<String>)

    @Query("DELETE FROM videos WHERE folderPath IN (:scannedFolderPaths) AND id NOT IN (:validIds)")
    suspend fun deleteStaleVideosInFolders(validIds: List<String>, scannedFolderPaths: List<String>)

    @Query("DELETE FROM videos WHERE folderPath IN (:scannedFolderPaths)")
    suspend fun deleteVideosInFolders(scannedFolderPaths: List<String>)

    @Query("DELETE FROM videos WHERE folderPath = :folderPath")
    suspend fun deleteVideosInFolder(folderPath: String)

    @Query("DELETE FROM videos")
    suspend fun clearAll()
}
