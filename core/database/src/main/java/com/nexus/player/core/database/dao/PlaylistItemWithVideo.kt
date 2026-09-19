package com.nexus.player.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.nexus.player.core.database.entity.PlaylistItemEntity
import com.nexus.player.core.database.entity.VideoEntity

/**
 * Room relationship mapping a [PlaylistItemEntity] to its optional [VideoEntity].
 * If the video record no longer exists or the file is missing, [video] is null.
 */
data class PlaylistItemWithVideo(
    @Embedded
    val item: PlaylistItemEntity,

    @Relation(
        parentColumn = "videoId",
        entityColumn = "id"
    )
    val video: VideoEntity?
)
