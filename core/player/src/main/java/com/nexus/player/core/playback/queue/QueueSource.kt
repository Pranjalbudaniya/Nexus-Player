package com.nexus.player.core.playback.queue

/**
 * Identifies the logical origin of a playback queue.
 *
 * This allows the player and future features (such as "Save as Playlist")
 * to understand the context from which the queue was created.
 */
sealed interface QueueSource {
    /**
     * Queue originated from a file-system folder browser.
     */
    data class Folder(val folderPath: String, val folderName: String = "") : QueueSource

    /**
     * Queue originated from a user playlist.
     */
    data class Playlist(val playlistId: String, val playlistName: String = "") : QueueSource

    /**
     * Queue originated from search results.
     */
    data class Search(val query: String) : QueueSource

    /**
     * Queue originated from the main Library screen.
     */
    data object Library : QueueSource

    /**
     * Queue originated from user favorites.
     */
    data object Favorites : QueueSource

    /**
     * Queue originated from a specific Home screen section (e.g. Continue Watching, Recently Added).
     */
    data class HomeSection(val sectionTitle: String) : QueueSource

    /**
     * Queue created from ad-hoc or manually selected videos.
     */
    data object Manual : QueueSource

    /**
     * No active queue source context.
     */
    data object None : QueueSource
}
