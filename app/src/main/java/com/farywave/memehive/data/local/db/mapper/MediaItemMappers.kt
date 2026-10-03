package com.farywave.memehive.data.local.db.mapper

import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.ui.model.MediaItem

fun MediaItem.toEntity() = MediaItemEntity(
    id = id,
    imageID = image?.id,
    caption = caption,
    description = description
)

fun MediaItemDetails.toMediaItem() = MediaItem(
    id = mediaItem.id,
    image = image?.toModel(),
    caption = mediaItem.caption,
    description = mediaItem.description,
    tags = tags.map { it.name }
)
