package com.nexus.player.core.media

import com.nexus.player.core.database.model.Video
import com.nexus.player.core.media.model.formatBitrate
import com.nexus.player.core.media.model.formatDuration
import com.nexus.player.core.media.model.formatFileSize
import com.nexus.player.core.media.model.formatFps
import com.nexus.player.core.media.model.formatModifiedDate
import com.nexus.player.core.media.model.toMediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaMetadataTest {

    @Test
    fun validVideoMetadataMapping() {
        val video = Video(
            id = "vid_1",
            mediaUri = "content://media/external/video/media/1",
            filePath = "/storage/Movies/Inception.mp4",
            fileName = "Inception.mp4",
            title = "Inception",
            folderName = "Sci-Fi",
            folderPath = "/storage/Movies",
            sizeBytes = 2_147_483_648L, // 2 GB
            durationMs = 8_880_000L,     // 2h 28m
            width = 3840,
            height = 2160,
            resolutionLabel = "4K",
            videoCodec = "HEVC",
            videoBitrate = 15_000_000L,
            frameRate = 23.976f,
            audioCodec = "DTS-HD",
            audioTrackCount = 3,
            subtitleTrackCount = 5,
            dateAdded = 1600000000000L,
            lastModified = 1600000000000L,
            lastPlayedAt = 1605000000000L,
            playbackPositionMs = 3600000L,
            playbackPercentage = 0.40f,
            isFavorite = true,
            watchCount = 1
        )

        val metadata = video.toMediaMetadata()

        assertEquals("Inception", metadata.title)
        assertEquals("Sci-Fi", metadata.folderName)
        assertEquals("2:28:00", metadata.formattedDuration)
        assertEquals("4K", metadata.resolutionLabel)
        assertEquals("3840x2160", metadata.dimensionsLabel)
        assertEquals("HEVC", metadata.videoCodec)
        assertEquals("DTS-HD", metadata.audioCodec)
        assertEquals("24.0 fps", metadata.formattedFps)
        assertEquals("15.0 Mbps", metadata.formattedBitrate)
        assertEquals("2.0 GB", metadata.formattedSize)
        assertTrue(metadata.isFavorite)
        assertEquals(3600000L, metadata.playbackPositionMs)
        assertEquals(0.40f, metadata.playbackPercentage, 0.001f)
    }

    @Test
    fun missingMetadataGracefulFallbacks() {
        val sparseVideo = Video(
            id = "vid_sparse",
            mediaUri = "content://media/external/video/media/2",
            filePath = null,
            fileName = "trailer.mp4",
            title = "",
            folderName = "",
            folderPath = "",
            sizeBytes = 0L,
            durationMs = 0L,
            width = 0,
            height = 0,
            resolutionLabel = "",
            videoCodec = null,
            videoBitrate = null,
            frameRate = null,
            audioCodec = null,
            dateAdded = 0L,
            lastModified = 0L
        )

        val metadata = sparseVideo.toMediaMetadata()

        assertEquals("trailer.mp4", metadata.title)
        assertEquals("Internal Storage", metadata.folderName)
        assertEquals("00:00", metadata.formattedDuration)
        assertEquals("SD", metadata.resolutionLabel)
        assertEquals("Unknown", metadata.dimensionsLabel)
        assertEquals("Unknown", metadata.videoCodec)
        assertEquals("Unknown", metadata.audioCodec)
        assertEquals("-- fps", metadata.formattedFps)
        assertEquals("-- Mbps", metadata.formattedBitrate)
        assertEquals("0 B", metadata.formattedSize)
        assertEquals("Unknown Date", metadata.formattedModifiedDate)
    }

    @Test
    fun invalidNegativeValuesFallbacks() {
        val corruptedVideo = Video(
            id = "vid_corrupted",
            mediaUri = "content://corrupt",
            filePath = null,
            fileName = "",
            title = "",
            folderName = "",
            folderPath = "",
            sizeBytes = -500L,
            durationMs = -1000L,
            width = -1920,
            height = -1080,
            resolutionLabel = "",
            videoCodec = null,
            videoBitrate = -9000L,
            frameRate = -24.0f,
            audioCodec = null,
            playbackPercentage = 5.0f,
            dateAdded = -1L,
            lastModified = -1L
        )

        val metadata = corruptedVideo.toMediaMetadata()

        assertEquals("Untitled", metadata.title)
        assertEquals("00:00", metadata.formattedDuration)
        assertEquals(0L, metadata.durationMs)
        assertEquals("Unknown", metadata.dimensionsLabel)
        assertEquals(0, metadata.width)
        assertEquals(0, metadata.height)
        assertEquals("0 B", metadata.formattedSize)
        assertEquals(0L, metadata.sizeBytes)
        assertEquals(1.0f, metadata.playbackPercentage, 0.001f) // Coerced to max 1.0f
        assertEquals("Unknown Date", metadata.formattedModifiedDate)
    }

    @Test
    fun formatDurationTests() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:00", formatDuration(-100L))
        assertEquals("00:45", formatDuration(45_000L))
        assertEquals("09:59", formatDuration(599_000L))
        assertEquals("10:00", formatDuration(600_000L))
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("1:01:05", formatDuration(3_665_000L))
        assertEquals("10:15:30", formatDuration(36_930_000L))
    }

    @Test
    fun formatFileSizeTests() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("0 B", formatFileSize(-50L))
        assertEquals("500 B", formatFileSize(500L))
        assertEquals("1.0 KB", formatFileSize(1024L))
        assertEquals("450.0 MB", formatFileSize(450L * 1024L * 1024L))
        assertEquals("1.5 GB", formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatBitrateTests() {
        assertEquals("-- Mbps", formatBitrate(null))
        assertEquals("-- Mbps", formatBitrate(0L))
        assertEquals("-- Mbps", formatBitrate(-100L))
        assertEquals("850 Kbps", formatBitrate(850_000L))
        assertEquals("4.5 Mbps", formatBitrate(4_500_000L))
    }

    @Test
    fun formatFpsTests() {
        assertEquals("-- fps", formatFps(null))
        assertEquals("-- fps", formatFps(0f))
        assertEquals("-- fps", formatFps(-24f))
        assertEquals("24 fps", formatFps(24.0f))
        assertEquals("60 fps", formatFps(60.0f))
        assertEquals("23.9 fps", formatFps(23.94f))
    }
}
