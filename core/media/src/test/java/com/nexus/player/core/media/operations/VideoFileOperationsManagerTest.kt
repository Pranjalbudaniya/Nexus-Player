package com.nexus.player.core.media.operations

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nexus.player.core.database.model.SearchFilter
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoFolder
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VideoFileOperationsManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var fakeRepository: TestVideoRepository
    private lateinit var operationsManager: VideoFileOperationsManager

    private lateinit var moviesDir: File
    private lateinit var downloadsDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeRepository = TestVideoRepository()
        operationsManager = VideoFileOperationsManagerImpl(
            context = context,
            videoRepository = fakeRepository,
            ioDispatcher = testDispatcher
        )

        moviesDir = tempFolder.newFolder("Movies")
        downloadsDir = tempFolder.newFolder("Downloads")
    }

    private fun createPhysicalVideo(
        parentDir: File,
        fileName: String,
        content: String = "dummy video bytes"
    ): File {
        val file = File(parentDir, fileName)
        file.writeText(content)
        return file
    }

    private fun sampleVideo(
        id: String,
        file: File,
        isFavorite: Boolean = false
    ): Video {
        return Video(
            id = id,
            mediaUri = "file://${file.absolutePath}",
            filePath = file.absolutePath,
            fileName = file.name,
            title = file.nameWithoutExtension,
            folderName = file.parentFile.name,
            folderPath = file.parentFile.absolutePath,
            sizeBytes = file.length(),
            durationMs = 120_000L,
            width = 1920,
            height = 1080,
            resolutionLabel = "1080p",
            dateAdded = System.currentTimeMillis(),
            lastModified = file.lastModified(),
            isFavorite = isFavorite
        )
    }

    @Test
    fun renameVideo_blankName_returnsFailure() = runTest {
        val file = createPhysicalVideo(moviesDir, "avatar.mp4")
        val video = sampleVideo("vid-1", file)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.renameVideo("vid-1", "   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun renameVideo_invalidCharacters_returnsFailure() = runTest {
        val file = createPhysicalVideo(moviesDir, "avatar.mp4")
        val video = sampleVideo("vid-1", file)
        fakeRepository.upsertVideo(video)

        val invalidNames = listOf("avatar:the_way", "avatar?movie", "avatar*cut", "avatar<new>", "avatar|hd", "avatar/2")
        for (name in invalidNames) {
            val result = operationsManager.renameVideo("vid-1", name)
            assertTrue("Expected failure for name: $name", result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun renameVideo_missingFile_returnsFailure() = runTest {
        val nonExistentFile = File(moviesDir, "ghost.mp4")
        val video = sampleVideo("vid-ghost", nonExistentFile)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.renameVideo("vid-ghost", "real_movie")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }

    @Test
    fun renameVideo_duplicateCollision_returnsFailure() = runTest {
        val file1 = createPhysicalVideo(moviesDir, "matrix.mp4")
        createPhysicalVideo(moviesDir, "reloaded.mp4")

        val video1 = sampleVideo("vid-1", file1)
        fakeRepository.upsertVideo(video1)

        val result = operationsManager.renameVideo("vid-1", "reloaded")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun renameVideo_success_renamesDiskFile_preservesExtension_updatesRoom() = runTest {
        val file = createPhysicalVideo(moviesDir, "inception.mkv")
        val video = sampleVideo("vid-inc", file)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.renameVideo("vid-inc", "Inception Remastered")
        assertTrue(result.isSuccess)

        val updatedMeta = result.getOrThrow()
        assertEquals("Inception Remastered", updatedMeta.title)
        assertEquals("Inception Remastered.mkv", updatedMeta.fileName)

        // Physical file verification
        assertFalse(file.exists())
        val newPhysicalFile = File(moviesDir, "Inception Remastered.mkv")
        assertTrue(newPhysicalFile.exists())
        assertEquals(newPhysicalFile.absolutePath, updatedMeta.filePath)

        // Room entity verification
        val inDb = fakeRepository.getVideoById("vid-inc")
        assertNotNull(inDb)
        assertEquals("Inception Remastered", inDb?.title)
        assertEquals("Inception Remastered.mkv", inDb?.fileName)
        assertEquals(newPhysicalFile.absolutePath, inDb?.filePath)
    }

    @Test
    fun moveVideo_success_movesToTargetFolder_preservesVideoId() = runTest {
        val file = createPhysicalVideo(moviesDir, "gladiator.mp4")
        val video = sampleVideo("vid-glad", file)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.moveVideo("vid-glad", downloadsDir.absolutePath)
        assertTrue(result.isSuccess)

        val updatedMeta = result.getOrThrow()
        assertEquals("vid-glad", updatedMeta.id) // Preserves stable ID!
        assertEquals("Downloads", updatedMeta.folderName)
        assertEquals(downloadsDir.absolutePath, updatedMeta.folderPath)

        // Physical verification
        assertFalse(file.exists())
        val movedFile = File(downloadsDir, "gladiator.mp4")
        assertTrue(movedFile.exists())
        assertEquals(movedFile.absolutePath, updatedMeta.filePath)

        // Room verification
        val inDb = fakeRepository.getVideoById("vid-glad")
        assertNotNull(inDb)
        assertEquals("vid-glad", inDb?.id)
        assertEquals(downloadsDir.absolutePath, inDb?.folderPath)
        assertEquals("Downloads", inDb?.folderName)
    }

    @Test
    fun moveVideo_collision_failsWhenTargetExists() = runTest {
        val file1 = createPhysicalVideo(moviesDir, "dune.mp4")
        createPhysicalVideo(downloadsDir, "dune.mp4") // duplicate in target folder

        val video = sampleVideo("vid-dune", file1)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.moveVideo("vid-dune", downloadsDir.absolutePath)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(file1.exists()) // Source unchanged
    }

    @Test
    fun copyVideo_success_createsNewFileWithUniqueId() = runTest {
        val file = createPhysicalVideo(moviesDir, "interstellar.mp4")
        val video = sampleVideo("vid-inter", file)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.copyVideo("vid-inter", downloadsDir.absolutePath)
        assertTrue(result.isSuccess)

        val copyMeta = result.getOrThrow()
        assertTrue(copyMeta.id != "vid-inter") // New UUID
        assertEquals("Downloads", copyMeta.folderName)

        // Source file still exists
        assertTrue(file.exists())

        // Target file exists
        val copyFile = File(downloadsDir, "interstellar.mp4")
        assertTrue(copyFile.exists())
        assertEquals(file.length(), copyFile.length())

        // Room has both records
        assertNotNull(fakeRepository.getVideoById("vid-inter"))
        assertNotNull(fakeRepository.getVideoById(copyMeta.id))
    }

    @Test
    fun copyVideo_existingTargetFile_autoResolvesNameConflict() = runTest {
        val file = createPhysicalVideo(moviesDir, "oppenheimer.mp4")
        createPhysicalVideo(downloadsDir, "oppenheimer.mp4") // Existing in destination

        val video = sampleVideo("vid-oppen", file)
        fakeRepository.upsertVideo(video)

        val result = operationsManager.copyVideo("vid-oppen", downloadsDir.absolutePath)
        assertTrue(result.isSuccess)

        val copyMeta = result.getOrThrow()
        assertEquals("oppenheimer (1).mp4", copyMeta.fileName)
        assertTrue(File(downloadsDir, "oppenheimer (1).mp4").exists())
    }

    @Test
    fun deleteVideo_stagedForUndo_andRestore_cycle() = runTest {
        val file = createPhysicalVideo(moviesDir, "batman.mp4")
        val video = sampleVideo("vid-batman", file)
        fakeRepository.upsertVideo(video)

        // 1. Delete staged
        val deleteResult = operationsManager.deleteVideo("vid-batman", stageForUndo = true)
        assertTrue(deleteResult.isSuccess)

        // Original physical file removed from Movies folder
        assertFalse(file.exists())
        // Record deleted from Room
        assertNull(fakeRepository.getVideoById("vid-batman"))

        // 2. Undo restoration
        val restoreResult = operationsManager.restoreDeletedVideo("vid-batman")
        assertTrue(restoreResult.isSuccess)

        // Physical file restored back in Movies folder
        assertTrue(file.exists())

        // Record re-inserted into Room
        val restoredInDb = fakeRepository.getVideoById("vid-batman")
        assertNotNull(restoredInDb)
        assertEquals("batman", restoredInDb?.title)
    }

    @Test
    fun setFavorite_updatesRepository() = runTest {
        val file = createPhysicalVideo(moviesDir, "spirited_away.mp4")
        val video = sampleVideo("vid-spirited", file, isFavorite = false)
        fakeRepository.upsertVideo(video)

        operationsManager.setFavorite("vid-spirited", true)
        val inDb = fakeRepository.getVideoById("vid-spirited")
        assertTrue(inDb?.isFavorite == true)

        operationsManager.setFavorite("vid-spirited", false)
        val inDb2 = fakeRepository.getVideoById("vid-spirited")
        assertFalse(inDb2?.isFavorite == true)
    }

    // --- In-memory Fake Video Repository ---

    private class TestVideoRepository : VideoRepository {
        private val videos = mutableMapOf<String, Video>()
        private val _flow = MutableStateFlow<List<Video>>(emptyList())

        override fun getAllVideos(sortOrder: VideoSortOrder): Flow<List<Video>> = _flow.asStateFlow()
        override fun getRecentlyAddedVideos(limit: Int): Flow<List<Video>> = _flow.asStateFlow()
        override fun getFavoriteVideos(): Flow<List<Video>> = _flow.map { it.filter { v -> v.isFavorite } }
        override fun getFavoriteVideos(limit: Int): Flow<List<Video>> = getFavoriteVideos()
        override fun getContinueWatchingVideos(limit: Int): Flow<List<Video>> = _flow.asStateFlow()
        override fun getHistoryVideos(limit: Int): Flow<List<Video>> = _flow.asStateFlow()
        override fun getVideosByFolder(folderPath: String): Flow<List<Video>> =
            _flow.map { it.filter { v -> v.folderPath == folderPath } }

        override fun getFolders(): Flow<List<VideoFolder>> = _flow.map { list ->
            list.groupBy { it.folderPath }.map { (path, vids) ->
                VideoFolder(
                    folderPath = path,
                    folderName = vids.first().folderName,
                    videoCount = vids.size,
                    previewMediaUri = vids.first().mediaUri
                )
            }
        }

        override suspend fun getVideoById(id: String): Video? = videos[id]
        override suspend fun getVideoByUri(mediaUri: String): Video? = videos.values.find { it.mediaUri == mediaUri }
        override suspend fun getVideosCount(): Int = videos.size
        override suspend fun insertVideo(video: Video): Long {
            videos[video.id] = video
            _flow.value = videos.values.toList()
            return 1L
        }
        override suspend fun upsertVideo(video: Video) {
            videos[video.id] = video
            _flow.value = videos.values.toList()
        }
        override suspend fun upsertVideos(videos: List<Video>) {
            videos.forEach { this.videos[it.id] = it }
            _flow.value = this.videos.values.toList()
        }
        override suspend fun updatePlaybackProgress(id: String, positionMs: Long, percentage: Float, lastPlayedAt: Long) {}
        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            videos[id]?.let {
                videos[id] = it.copy(isFavorite = isFavorite)
                _flow.value = videos.values.toList()
            }
        }
        override suspend fun deleteVideo(id: String) {
            videos.remove(id)
            _flow.value = videos.values.toList()
        }
        override suspend fun deleteVideoByUri(mediaUri: String) {
            videos.values.find { it.mediaUri == mediaUri }?.let { videos.remove(it.id) }
            _flow.value = videos.values.toList()
        }
        override suspend fun deleteStaleVideos(validIds: List<String>) {}
        override suspend fun clearAll() {
            videos.clear()
            _flow.value = emptyList()
        }
    }
}
