package com.farywave.memehive.data.local.db.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.entity.ImageEntity

data class CollectionDetails (
    @Embedded
    val collection: CollectionEntity,

    @Embedded(prefix = "cover_")
    val coverImage: ImageEntity? = null,

    @ColumnInfo(name = "media_item_count")
    val mediaItemCount: Int,
)
