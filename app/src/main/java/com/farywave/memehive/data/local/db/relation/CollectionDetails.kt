package com.farywave.memehive.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.entity.ImageEntity

data class CollectionDetails (
    @Embedded
    val collection: CollectionEntity,

    @Relation(
        parentColumn = "cover_image_id",
        entityColumn = "id"
    )
    val coverImage: ImageEntity? = null
)
