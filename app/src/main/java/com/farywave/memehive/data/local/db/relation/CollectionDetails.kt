package com.farywave.memehive.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.ui.model.Collection

data class CollectionDetails (
    @Embedded
    val collection: CollectionEntity,

    @Relation(
        parentColumn = "cover_image_id",
        entityColumn = "id"
    )
    val coverImage: ImageEntity? = null
) {
    fun toModel(mediaItemCount: Int) = Collection(
            id = collection.id,
            name = collection.name,
            cover = coverImage?.toModel(),
            mediaItemCount = mediaItemCount
        )
}