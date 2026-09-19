package com.nexus.player.core.scanner

import com.nexus.player.core.scanner.util.VideoFormatDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFormatDetectorTest {

    @Test
    fun supportedVideoExtensions() {
        val supported = listOf(
            "mp4", "mkv", "webm", "avi", "mov", "3gp",
            "ts", "m2ts", "mts", "m4v", "wmv", "flv",
            "vob", "ogv", "f4v", "divx", "rmvb", "asf",
            "mpg", "mpeg"
        )
        for (ext in supported) {
            assertTrue("Expected extension $ext to be recognized as video", VideoFormatDetector.isVideoExtension(ext))
            assertTrue("Expected video.$ext to be recognized", VideoFormatDetector.isVideoFile("movie.$ext"))
            assertTrue("Expected uppercase .$ext to be recognized", VideoFormatDetector.isVideoFile("movie.${ext.uppercase()}"))
        }
    }

    @Test
    fun unsupportedExtensions() {
        val unsupported = listOf("mp3", "flac", "jpg", "png", "txt", "pdf", "apk", "zip", "exe")
        for (ext in unsupported) {
            assertFalse("Expected extension $ext to be rejected", VideoFormatDetector.isVideoExtension(ext))
            assertFalse("Expected file.$ext to be rejected", VideoFormatDetector.isVideoFile("file.$ext"))
        }
    }

    @Test
    fun mimeTypeDetection() {
        assertTrue(VideoFormatDetector.isVideoMimeType("video/mp4"))
        assertTrue(VideoFormatDetector.isVideoMimeType("video/x-matroska"))
        assertTrue(VideoFormatDetector.isVideoMimeType("video/webm"))
        assertTrue(VideoFormatDetector.isVideoMimeType("application/x-matroska"))
        assertTrue(VideoFormatDetector.isVideoMimeType("application/vnd.apple.mpegurl"))

        assertFalse(VideoFormatDetector.isVideoMimeType("audio/mp4"))
        assertFalse(VideoFormatDetector.isVideoMimeType("image/jpeg"))
        assertFalse(VideoFormatDetector.isVideoMimeType("application/json"))
        assertFalse(VideoFormatDetector.isVideoMimeType(""))
    }

    @Test
    fun extractDisplayTitle() {
        assertEquals("Avengers.Endgame", VideoFormatDetector.extractDisplayTitle("Avengers.Endgame.mp4"))
        assertEquals("My Video File", VideoFormatDetector.extractDisplayTitle("My Video File.mkv"))
        assertEquals("archive.tar", VideoFormatDetector.extractDisplayTitle("archive.tar.gz"))
        assertEquals("no_extension", VideoFormatDetector.extractDisplayTitle("no_extension"))
    }
}
