package com.farywave.memehive.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity

data class MediaWithTags(
    @Embedded val media: MediaItemEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MediaItemTagEntity::class,
            parentColumn = "mediaItemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)