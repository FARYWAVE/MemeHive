package com.farywave.memehive.data.local.db.mapper

import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.relation.CollectionDetails
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image

fun Collection.toEntity() = CollectionEntity(
    id = id,
    name = name,
    coverImageId = cover?.id
)

fun CollectionEntity.toModel(cover: Image?, mediaItemCount: Int) = Collection(
    id = id,
    name = name,
    cover = cover,
    mediaItemCount = mediaItemCount
)

fun CollectionDetails.toModel(mediaItemCount: Int) = collection.toModel(
    cover = coverImage?.toModel(),
    mediaItemCount = mediaItemCount
)
