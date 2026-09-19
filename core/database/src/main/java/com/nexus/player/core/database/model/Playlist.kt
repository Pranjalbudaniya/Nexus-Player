package com.nexus.player.core.database.model

/**
 * Clean domain representation of a user playlist.
 */
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int = 0,
    val thumbnailVideoIds: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = itemCount == 0

    val formattedItemCount: String
        get() = when (itemCount) {
            0 -> "Empty"
            1 -> "1 video"
            else -> "$itemCount videos"
        }
}
