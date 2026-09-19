package com.nexus.player.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexus.player.core.database.dao.VideoDao
import com.nexus.player.core.database.model.Video
import com.nexus.player.core.database.model.VideoSortOrder
import com.nexus.player.core.database.repository.FolderRepository
import com.nexus.player.core.database.repository.FolderRepositoryImpl
import com.nexus.player.core.database.repository.FolderSortOrder
import com.nexus.player.core.database.repository.VideoRepository
import com.nexus.player.core.database.repository.VideoRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FolderRepositoryTest {

    private lateinit var database: NexusDatabase
    private lateinit var videoDao: VideoDao
    private lateinit var videoRepository: VideoRepository
    private lateinit var folderRepository: FolderRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoDao = database.videoDao()
        videoRepository = VideoRepositoryImpl(videoDao = videoDao, ioDispatcher = testDispatcher)
        folderRepository = FolderRepositoryImpl(videoDao = videoDao, ioDispatcher = testDispatcher)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    private fun sampleVideo(
        id: String,
        title: String,
        folderName: String,
        folderPath: String,
        dateAdded: Long = 1000L,
        lastModified: Long = 1000L
    ): Video = Video(
        id = id,
        mediaUri = "content://media/$id",
        filePath = "$folderPath/$title.mp4",
        fileName = "$title.mp4",
        title = title,
        folderName = folderName,
        folderPath = folderPath,
        durationMs = 120000L,
        sizeBytes = 1000000L,
        width = 1920,
        height = 1080,
        resolutionLabel = "1080p",
        dateAdded = dateAdded,
        lastModified = lastModified
    )

    @Test
    fun folderDiscovery_computesCountsAndPreviewUri() = runTest {
        videoRepository.upsertVideos(
            listOf(
                sampleVideo(id = "1", title = "Action Movie", folderName = "Action", folderPath = "/Movies/Action", dateAdded = 100L),
                sampleVideo(id = "2", title = "Comedy Movie", folderName = "Comedy", folderPath = "/Movies/Comedy", dateAdded = 200L),
                sampleVideo(id = "3", title = "Another Action", folderName = "Action", folderPath = "/Movies/Action", dateAdded = 300L)
            )
        )

        val folders = folderRepository.getFolders().first()
        assertEquals(2, folders.size)

        val actionFolder = folders.first { it.folderName == "Action" }
        assertEquals(2, actionFolder.videoCount)
        assertEquals("/Movies/Action", actionFolder.folderPath)
        assertEquals("/Movies", actionFolder.parentPath)
        // Preview URI should be the latest added video (id = 3)
        assertEquals("content://media/3", actionFolder.previewMediaUri)

        val comedyFolder = folders.first { it.folderName == "Comedy" }
        assertEquals(1, comedyFolder.videoCount)
        assertEquals("content://media/2", comedyFolder.previewMediaUri)
    }

    @Test
    fun rootAndChildFolders_derivedCorrectly() = runTest {
        videoRepository.upsertVideos(
            listOf(
                sampleVideo(id = "1", title = "Root Video", folderName = "Movies", folderPath = "/Movies"),
                sampleVideo(id = "2", title = "Action Video", folderName = "Action", folderPath = "/Movies/Action"),
                sampleVideo(id = "3", title = "SciFi Video", folderName = "SciFi", folderPath = "/Movies/Action/SciFi"),
                sampleVideo(id = "4", title = "Download 1", folderName = "Downloads", folderPath = "/Downloads")
            )
        )

        // Root folders: /Movies and /Downloads (their parent is not an active folder in the library)
        val roots = folderRepository.getRootFolders().first()
        assertEquals(listOf("Downloads", "Movies"), roots.map { it.folderName }.sorted())

        // Child folders of /Movies: should only be /Movies/Action
        val movieChildren = folderRepository.getChildFolders("/Movies").first()
        assertEquals(1, movieChildren.size)
        assertEquals("Action", movieChildren.first().folderName)

        // Child folders of /Movies/Action: should be /Movies/Action/SciFi
        val actionChildren = folderRepository.getChildFolders("/Movies/Action").first()
        assertEquals(1, actionChildren.size)
        assertEquals("SciFi", actionChildren.first().folderName)
    }

    @Test
    fun folderSorting_allDimensions() = runTest {
        videoRepository.upsertVideos(
            listOf(
                sampleVideo(id = "1", title = "Zeta Video", folderName = "Zeta", folderPath = "/Zeta", lastModified = 3000L),
                sampleVideo(id = "2", title = "Alpha 1", folderName = "Alpha", folderPath = "/Alpha", lastModified = 1000L),
                sampleVideo(id = "3", title = "Alpha 2", folderName = "Alpha", folderPath = "/Alpha", lastModified = 2000L),
                sampleVideo(id = "4", title = "Beta", folderName = "Beta", folderPath = "/Beta", lastModified = 1500L)
            )
        )

        // Name ASC
        val byNameAsc = folderRepository.getFolders(FolderSortOrder.NAME_ASC).first()
        assertEquals(listOf("Alpha", "Beta", "Zeta"), byNameAsc.map { it.folderName })

        // Name DESC
        val byNameDesc = folderRepository.getFolders(FolderSortOrder.NAME_DESC).first()
        assertEquals(listOf("Zeta", "Beta", "Alpha"), byNameDesc.map { it.folderName })

        // Video Count DESC: Alpha (2), Beta (1), Zeta (1)
        val byCountDesc = folderRepository.getFolders(FolderSortOrder.VIDEO_COUNT_DESC).first()
        assertEquals("Alpha", byCountDesc.first().folderName)
        assertEquals(2, byCountDesc.first().videoCount)

        // Recently Modified DESC: Zeta (3000), Alpha (max 2000), Beta (1500)
        val byModDesc = folderRepository.getFolders(FolderSortOrder.MODIFIED_DESC).first()
        assertEquals(listOf("Zeta", "Alpha", "Beta"), byModDesc.map { it.folderName })
    }

    @Test
    fun getVideosInFolder_returnsVideosFilteredAndSorted() = runTest {
        videoRepository.upsertVideos(
            listOf(
                sampleVideo(id = "1", title = "Zebra", folderName = "Animals", folderPath = "/Animals"),
                sampleVideo(id = "2", title = "Aardvark", folderName = "Animals", folderPath = "/Animals"),
                sampleVideo(id = "3", title = "Bear", folderName = "Animals", folderPath = "/Animals"),
                sampleVideo(id = "4", title = "Car", folderName = "Vehicles", folderPath = "/Vehicles")
            )
        )

        val animalVideos = folderRepository.getVideosInFolder("/Animals", VideoSortOrder.TITLE_ASC).first()
        assertEquals(3, animalVideos.size)
        assertEquals(listOf("Aardvark", "Bear", "Zebra"), animalVideos.map { it.title })

        val vehicleVideos = folderRepository.getVideosInFolder("/Vehicles", VideoSortOrder.TITLE_ASC).first()
        assertEquals(1, vehicleVideos.size)
        assertEquals("Car", vehicleVideos.first().title)

        // Non-existent folder returns empty list without error
        val emptyList = folderRepository.getVideosInFolder("/NonExistent").first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun getFolderByPath_lookup() = runTest {
        videoRepository.upsertVideos(
            listOf(
                sampleVideo(id = "1", title = "Doc", folderName = "Documents", folderPath = "/Documents")
            )
        )

        val folder = folderRepository.getFolderByPath("/Documents").first()
        assertNotNull(folder)
        assertEquals("Documents", folder?.folderName)
        assertEquals(1, folder?.videoCount)

        val notFound = folderRepository.getFolderByPath("/Unknown").first()
        assertNull(notFound)
    }
}
