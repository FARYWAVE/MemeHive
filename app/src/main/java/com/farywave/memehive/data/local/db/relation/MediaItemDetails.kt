package com.farywave.memehive.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.ui.model.MediaItem
import java.io.File

data class MediaItemDetails(
    @Embedded val mediaItem: MediaItemEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MediaItemTagEntity::class,
            parentColumn = "media_item_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "image_id",
        entityColumn = "id"
    )
    val image: ImageEntity? = null
) {
    fun toMediaItem(): MediaItem {
        return MediaItem(
            id = mediaItem.id,
            image = image?.toModel(),
            caption = mediaItem.caption,
            description = mediaItem.description,
            tags = tags.map { it.name }
        )
    }
}